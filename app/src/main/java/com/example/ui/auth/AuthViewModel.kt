package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.AdminAccountRecord
import com.example.data.model.UserProfile
import com.example.data.repository.UserProfileRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthSessionState {
  data object Loading : AuthSessionState
  data object Unauthenticated : AuthSessionState
  data class NeedsStudentOnboarding(
    val uid: String,
    val email: String,
    val displayName: String,
    val emailVerified: Boolean,
  ) : AuthSessionState
  data class AuthenticatedStudent(
    val profile: UserProfile,
    val emailVerified: Boolean,
  ) : AuthSessionState
  data class AuthenticatedAdmin(
    val profile: UserProfile,
    val emailVerified: Boolean,
    val adminRecord: AdminAccountRecord =
      AdminAccountRecord(
        uid = profile.uid,
        fullName = profile.fullName,
        email = profile.email,
        role =
          if (profile.role == UserProfile.ROLE_DEPARTMENT_ADMIN) {
            UserProfile.ROLE_DEPARTMENT_ADMIN
          } else {
            UserProfile.ROLE_SUPER_ADMIN
          },
        facultyId = profile.facultyId.takeIf { it.isNotBlank() },
        departmentId = profile.departmentId.takeIf { it.isNotBlank() },
        isActive = profile.isActive,
      ),
  ) : AuthSessionState
}

/**
 * Enforces strict 3-tier role-based navigation graph and destination isolation:
 * - LEVEL 1 (`super_admin`): Accesses ONLY Super Admin destinations inside [AdminNavigationGraph].
 * - LEVEL 2 (`department_admin`): Accesses ONLY Department Admin destinations inside [AdminNavigationGraph];
 *   NEVER accesses Super Admin destinations or Student destinations.
 * - LEVEL 3 (`student`): Accesses ONLY [StudentNavigationGraph]; NEVER accesses any administrator destination.
 * - Inactive accounts (`isActive == false`) immediately lose access to all authenticated destinations.
 */
object RoleAccessGuard {
  fun canAccessStudentGraph(state: AuthSessionState): Boolean {
    return state is AuthSessionState.AuthenticatedStudent &&
      state.profile.isActive &&
      state.profile.role == UserProfile.ROLE_STUDENT &&
      !state.profile.isAdminRole
  }

  fun canAccessSuperAdminDestination(state: AuthSessionState): Boolean {
    return state is AuthSessionState.AuthenticatedAdmin &&
      state.profile.isActive &&
      state.adminRecord.isActive &&
      state.adminRecord.isSuperAdmin &&
      state.profile.isSuperAdminRole &&
      !state.profile.isDepartmentAdminRole &&
      !state.profile.isStudentRole
  }

  fun canAccessDepartmentAdminDestination(state: AuthSessionState): Boolean {
    return state is AuthSessionState.AuthenticatedAdmin &&
      state.profile.isActive &&
      state.adminRecord.isActive &&
      state.adminRecord.isDepartmentAdmin &&
      state.profile.isDepartmentAdminRole &&
      !state.profile.isSuperAdminRole &&
      !state.profile.isStudentRole
  }

  fun canAccessAdminGraph(state: AuthSessionState): Boolean {
    return canAccessSuperAdminDestination(state) || canAccessDepartmentAdminDestination(state)
  }

  fun canExportEvaluationReport(state: AuthSessionState): Boolean {
    return canAccessSuperAdminDestination(state) || canAccessDepartmentAdminDestination(state)
  }
}

class AuthViewModel(
  private val repository: UserProfileRepository,
  private val auth: FirebaseAuth = Firebase.auth,
) : ViewModel() {

  private val _sessionState = MutableStateFlow<AuthSessionState>(AuthSessionState.Loading)
  val sessionState: StateFlow<AuthSessionState> = _sessionState.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private val _feedbackBanner = MutableStateFlow<Pair<Boolean, String>?>(null)
  val feedbackBanner: StateFlow<Pair<Boolean, String>?> = _feedbackBanner.asStateFlow()

  private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
    val currentUser = firebaseAuth.currentUser
    if (currentUser == null) {
      val current = _sessionState.value
      val isDemoSession =
        when (current) {
          is AuthSessionState.AuthenticatedStudent ->
            com.example.data.model.WcuDemoDataProvider.isDemoUid(current.profile.uid)
          is AuthSessionState.AuthenticatedAdmin ->
            com.example.data.model.WcuDemoDataProvider.isDemoUid(current.profile.uid)
          else -> false
        }
      if (!isDemoSession) {
        _sessionState.value = AuthSessionState.Unauthenticated
      }
    } else {
      resolveAuthenticatedUserSession(currentUser, requestedRole = null)
    }
  }

  init {
    auth.addAuthStateListener(authStateListener)
  }

  fun clearBanner() {
    _feedbackBanner.value = null
  }

  fun setErrorBanner(message: String) {
    _feedbackBanner.value = true to message
  }

  fun setSuccessBanner(message: String) {
    _feedbackBanner.value = false to message
  }

  fun loginAsDemoStudent() {
    _feedbackBanner.value =
      false to "Signed in as Demo Student (Software Engineering • WCU/1420/16)."
    _sessionState.value =
      AuthSessionState.AuthenticatedStudent(
        profile = com.example.data.model.WcuDemoDataProvider.demoStudentProfile,
        emailVerified = true,
      )
  }

  fun loginAsDemoDepartmentAdmin() {
    _feedbackBanner.value =
      false to "Signed in as Demo Department Admin (Software Engineering • dep_se)."
    _sessionState.value =
      AuthSessionState.AuthenticatedAdmin(
        profile = com.example.data.model.WcuDemoDataProvider.demoDepartmentAdminProfile,
        emailVerified = true,
        adminRecord = com.example.data.model.WcuDemoDataProvider.demoDepartmentAdminRecord,
      )
  }

  fun loginAsDemoSuperAdmin() {
    _feedbackBanner.value =
      false to "Signed in as Demo Super Admin (University-Wide Governance)."
    _sessionState.value =
      AuthSessionState.AuthenticatedAdmin(
        profile = com.example.data.model.WcuDemoDataProvider.demoSuperAdminProfile,
        emailVerified = true,
        adminRecord = com.example.data.model.WcuDemoDataProvider.demoSuperAdminRecord,
      )
  }

  fun signOutCurrentSession() {
    auth.signOut()
    _sessionState.value = AuthSessionState.Unauthenticated
  }

  fun resolveAuthenticatedUserSession(
    firebaseUser: FirebaseUser,
    requestedRole: String? = null,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val uid = firebaseUser.uid

      // Always check the trusted Firestore /admins/{uid} record first on session restore/login
      val trustedAdminRecord = repository.resolveAdminAccountRecord().getOrNull()
      val profileResult = repository.getUserProfile(uid)

      profileResult
        .onSuccess { existingProfile ->
          val requestedAdminLogin =
            requestedRole == UserProfile.ROLE_ADMIN ||
              requestedRole == UserProfile.ROLE_SUPER_ADMIN ||
              requestedRole == UserProfile.ROLE_DEPARTMENT_ADMIN

          // 1. If user attempted Administrator Login without an active trusted AdminAccountRecord,
          // deny access immediately and sign out so students or deactivated admins never enter admin screens.
          if (requestedAdminLogin && trustedAdminRecord == null) {
            _isBusy.value = false
            _feedbackBanner.value =
              true to "Access Denied: Your account (${firebaseUser.email ?: uid}) does not have active Administrator authorization."
            auth.signOut()
            _sessionState.value = AuthSessionState.Unauthenticated
            return@onSuccess
          }

          // 2. If user has a trusted active AdminAccountRecord (Super Admin or Department Admin)
          if (trustedAdminRecord != null && trustedAdminRecord.isActive) {
            if (existingProfile != null && !existingProfile.isActive) {
              _isBusy.value = false
              _feedbackBanner.value =
                true to "Your Wachemo University administrator account has been deactivated."
              auth.signOut()
              _sessionState.value = AuthSessionState.Unauthenticated
              return@onSuccess
            }

            val resolvedRole =
              if (trustedAdminRecord.isDepartmentAdmin) {
                UserProfile.ROLE_DEPARTMENT_ADMIN
              } else {
                UserProfile.ROLE_SUPER_ADMIN
              }

            val resolvedFacultyId =
              if (trustedAdminRecord.isDepartmentAdmin) {
                trustedAdminRecord.facultyId.orEmpty()
              } else {
                "WCU-SUPER-ADMIN"
              }

            val resolvedDepartmentId =
              if (trustedAdminRecord.isDepartmentAdmin) {
                trustedAdminRecord.departmentId.orEmpty()
              } else {
                "UNIVERSITY-WIDE"
              }

            val adminProfile =
              UserProfile(
                id = uid,
                uid = uid,
                fullName =
                  trustedAdminRecord.fullName.ifBlank {
                    existingProfile?.fullName
                      ?: firebaseUser.displayName
                      ?: "WCU Administrator"
                  },
                email = trustedAdminRecord.email.ifBlank { firebaseUser.email.orEmpty() },
                studentId = "",
                role = resolvedRole,
                facultyId = resolvedFacultyId,
                departmentId = resolvedDepartmentId,
                yearOfStudy = 1,
                isActive = true,
              )

            if (requestedRole == UserProfile.ROLE_STUDENT) {
              _feedbackBanner.value =
                false to "Administrator account verified (${resolvedRole.uppercase()}). Redirected to Administrator Portal."
            }

            _sessionState.value =
              AuthSessionState.AuthenticatedAdmin(
                profile = adminProfile,
                emailVerified = firebaseUser.isEmailVerified,
                adminRecord = trustedAdminRecord,
              )
            _isBusy.value = false
            return@onSuccess
          }

          // 3. If existingProfile claims an admin role in /users/{uid} but has NO active trusted /admins/{uid} record,
          // treat as deactivated / unauthorized admin and sign out immediately!
          if (existingProfile != null && existingProfile.isAdminRole) {
            _isBusy.value = false
            _feedbackBanner.value =
              true to "Administrator authorization is inactive or revoked. Session terminated."
            auth.signOut()
            _sessionState.value = AuthSessionState.Unauthenticated
            return@onSuccess
          }

          // 4. Regular Student flow
          if (existingProfile == null) {
            _sessionState.value =
              AuthSessionState.NeedsStudentOnboarding(
                uid = uid,
                email = firebaseUser.email.orEmpty(),
                displayName = firebaseUser.displayName.orEmpty(),
                emailVerified = firebaseUser.isEmailVerified,
              )
          } else if (!existingProfile.isActive) {
            _feedbackBanner.value =
              true to "Your Wachemo University account has been deactivated by an administrator."
            auth.signOut()
            _sessionState.value = AuthSessionState.Unauthenticated
          } else {
            _sessionState.value =
              AuthSessionState.AuthenticatedStudent(
                profile = existingProfile.copy(role = UserProfile.ROLE_STUDENT),
                emailVerified = firebaseUser.isEmailVerified,
              )
          }
          _isBusy.value = false
        }
        .onFailure { error ->
          _isBusy.value = false
          _feedbackBanner.value =
            true to (error.localizedMessage ?: "Failed to load user profile from Cloud Firestore.")
        }
    }
  }

  fun completeStudentRegistration(
    fullName: String,
    email: String,
    studentId: String,
    facultyId: String,
    departmentId: String,
    yearOfStudy: Int,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      _feedbackBanner.value = null

      val isAdmin = repository.verifyAdminAuthorization().getOrDefault(false)
      if (isAdmin) {
        _isBusy.value = false
        _feedbackBanner.value =
          true to "Administrator accounts cannot register as students or acquire student permissions."
        return@launch
      }

      val result =
        repository.createStudentProfile(
          fullName = fullName,
          email = email,
          studentId = studentId,
          facultyId = facultyId,
          departmentId = departmentId,
          yearOfStudy = yearOfStudy,
        )

      result
        .onSuccess { createdProfile ->
          _isBusy.value = false
          _feedbackBanner.value =
            false to "Student profile (${createdProfile.studentId}) saved to Cloud Firestore."
          _sessionState.value =
            AuthSessionState.AuthenticatedStudent(
              profile = createdProfile,
              emailVerified = auth.currentUser?.isEmailVerified ?: false,
            )
        }
        .onFailure { error ->
          _isBusy.value = false
          _feedbackBanner.value =
            true to (error.localizedMessage ?: "Failed to register student profile.")
        }
    }
  }

  fun sendPasswordResetEmail(email: String) {
    val trimmed = email.trim()
    if (trimmed.isEmpty() || !trimmed.contains("@")) {
      _feedbackBanner.value = true to "Please enter a valid registered email address."
      return
    }

    viewModelScope.launch {
      _isBusy.value = true
      _feedbackBanner.value = null
      try {
        auth.sendPasswordResetEmail(trimmed).await()
        _isBusy.value = false
        _feedbackBanner.value =
          false to "Password reset instructions sent to $trimmed via Firebase Authentication."
      } catch (e: Exception) {
        _isBusy.value = false
        _feedbackBanner.value =
          true to (e.localizedMessage ?: "Failed to send password reset email.")
      }
    }
  }

  fun sendEmailVerification() {
    val user = auth.currentUser ?: return
    viewModelScope.launch {
      _isBusy.value = true
      try {
        user.sendEmailVerification().await()
        _isBusy.value = false
        _feedbackBanner.value =
          false to "Verification email sent to ${user.email}. Please check your inbox."
      } catch (e: Exception) {
        _isBusy.value = false
        _feedbackBanner.value =
          true to (e.localizedMessage ?: "Could not send verification email.")
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    auth.removeAuthStateListener(authStateListener)
  }

  companion object {
    val Factory = viewModelFactory {
      initializer {
        val app =
          checkNotNull(this[APPLICATION_KEY]) {
            "APPLICATION_KEY missing from CreationExtras"
          }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        AuthViewModel(UserProfileRepository(db))
      }
    }
  }
}
