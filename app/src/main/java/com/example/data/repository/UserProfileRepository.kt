package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AdminAccountRecord
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserProfileRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
  }

  /**
   * Creates a verified student profile in `/users/{uid}`.
   * Enforces `role = "student"` on client registration — never allows self-assigned admin privileges.
   */
  suspend fun createStudentProfile(
    fullName: String,
    email: String,
    studentId: String,
    facultyId: String,
    departmentId: String,
    yearOfStudy: Int = 1,
  ): Result<UserProfile> = runCatching {
    val uid = requireUserId()
    val trimmedName = fullName.trim()
    val trimmedEmail = email.trim()
    val normalizedStudentId = studentId.trim().uppercase()

    require(trimmedName.length >= 2) { "Full name must be at least 2 characters." }
    require(trimmedEmail.contains("@")) { "A valid email address is required." }
    require(normalizedStudentId.startsWith("WCU/") || normalizedStudentId.startsWith("WCU-")) {
      "Student ID must follow official WCU format (e.g., WCU/1420/16)."
    }
    require(facultyId.isNotBlank()) { "Faculty selection is required." }
    require(departmentId.isNotBlank()) { "Department selection is required." }
    require(yearOfStudy in 1..7) { "Year of study must be between 1 and 7." }

    val docRef = db.collection(COLLECTION_USERS).document(uid)
    val payload =
      mapOf<String, Any?>(
        "uid" to uid,
        "fullName" to trimmedName,
        "email" to trimmedEmail,
        "studentId" to normalizedStudentId,
        "role" to UserProfile.ROLE_STUDENT,
        "facultyId" to facultyId.trim(),
        "departmentId" to departmentId.trim(),
        "yearOfStudy" to yearOfStudy,
        "isActive" to true,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      ).filterValues { it != null }.mapValues { it.value as Any }

    try {
      docRef.set(payload).await()
      val snapshot = docRef.get().await()
      snapshot.toObject(
        UserProfile::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
      ) ?: UserProfile(
        id = uid,
        uid = uid,
        fullName = trimmedName,
        email = trimmedEmail,
        studentId = normalizedStudentId,
        role = UserProfile.ROLE_STUDENT,
        facultyId = facultyId.trim(),
        departmentId = departmentId.trim(),
        yearOfStudy = yearOfStudy,
        isActive = true,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  /**
   * Fetches a user profile by UID. Security rules enforce that students can only fetch their own UID.
   */
  suspend fun getUserProfile(uid: String = requireUserId()): Result<UserProfile?> = runCatching {
    val docRef = db.collection(COLLECTION_USERS).document(uid)
    try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) return@runCatching null
      snapshot.toObject(
        UserProfile::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
  }

  /**
   * Queries the authenticated user's own profile using the Query Enforcer filter `whereEqualTo("uid", uid)`.
   */
  suspend fun queryOwnUserProfiles(): Result<List<UserProfile>> = runCatching {
    val uid = requireUserId()
    val path = COLLECTION_USERS
    try {
      val querySnapshot =
        db.collection(COLLECTION_USERS)
          .whereEqualTo("uid", uid)
          .get()
          .await()
      querySnapshot.toObjects(
        UserProfile::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      throw e
    }
  }

  /**
   * Real-time observation of the authenticated user's profile.
   */
  fun observeUserProfile(uid: String = requireUserId()): Flow<UserProfile?> = flow {
    val docRef = db.collection(COLLECTION_USERS).document(uid)
    emitAll(
      docRef
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              UserProfile::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
          }
          throw error
        }
    )
  }

  /**
   * Updates permitted student profile fields (`fullName`, `studentId`, `facultyId`, `departmentId`, `yearOfStudy`).
   * Never modifies `role`, `uid`, `email`, `isActive`, or `createdAt`.
   */
  suspend fun updateStudentProfile(
    fullName: String,
    studentId: String,
    facultyId: String,
    departmentId: String,
    yearOfStudy: Int,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection(COLLECTION_USERS).document(uid)
    val updates =
      mapOf<String, Any?>(
        "fullName" to fullName.trim(),
        "studentId" to studentId.trim().uppercase(),
        "facultyId" to facultyId.trim(),
        "departmentId" to departmentId.trim(),
        "yearOfStudy" to yearOfStudy,
        "updatedAt" to FieldValue.serverTimestamp(),
      ).filterValues { it != null }.mapValues { it.value as Any }

    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  /**
   * Resolves the trusted Administrator Authorization Record from `/admins/{uid}` or verified bootstrapped Super Admin.
   * Never trusts client-supplied role, facultyId, or departmentId.
   * If `/admins/{uid}` has `isActive == false`, returns `null` (immediately revoking admin access).
   */
  suspend fun resolveAdminAccountRecord(): Result<AdminAccountRecord?> = runCatching {
    val currentUser =
      auth.currentUser ?: throw IllegalStateException("No authenticated user session.")
    val uid = currentUser.uid

    val adminDocRef = db.collection(COLLECTION_ADMINS).document(uid)
    val adminDocSnapshot =
      try {
        adminDocRef.get().await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, adminDocRef.path)
        null
      }

    if (adminDocSnapshot != null && adminDocSnapshot.exists()) {
      val parsed =
        adminDocSnapshot.toObject(
          AdminAccountRecord::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      if (parsed == null || !parsed.isActive) {
        return@runCatching null
      }
      val normalizedRole =
        when (parsed.role) {
          UserProfile.ROLE_DEPARTMENT_ADMIN -> UserProfile.ROLE_DEPARTMENT_ADMIN
          else -> UserProfile.ROLE_SUPER_ADMIN
        }
      return@runCatching parsed.copy(
        uid = uid,
        role = normalizedRole,
      )
    }

    val isBootstrappedVerifiedSuperAdmin =
      currentUser.email == BOOTSTRAPPED_ADMIN_EMAIL && currentUser.isEmailVerified
    if (isBootstrappedVerifiedSuperAdmin) {
      return@runCatching AdminAccountRecord(
        uid = uid,
        fullName = currentUser.displayName ?: "University Super Administrator",
        email = currentUser.email.orEmpty(),
        role = UserProfile.ROLE_SUPER_ADMIN,
        facultyId = null,
        departmentId = null,
        isActive = true,
        createdBy = "system_bootstrap",
      )
    }

    null
  }

  /**
   * Verifies whether the currently authenticated user is an active authorized Wachemo University Administrator
   * (either Super Admin or Department Admin).
   */
  suspend fun verifyAdminAuthorization(): Result<Boolean> = runCatching {
    val adminRecord = resolveAdminAccountRecord().getOrNull()
    adminRecord != null && adminRecord.isActive && (adminRecord.isSuperAdmin || adminRecord.isDepartmentAdmin)
  }

  companion object {
    const val COLLECTION_USERS = "users"
    const val COLLECTION_ADMINS = "admins"
    const val BOOTSTRAPPED_ADMIN_EMAIL = "abelbimrew868@gmail.com"
  }
}
