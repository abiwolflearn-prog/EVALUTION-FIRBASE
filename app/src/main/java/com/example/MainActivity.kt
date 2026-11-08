package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.GoogleCredentialAuthHelper
import com.example.data.model.UserProfile
import com.example.ui.auth.AuthSessionState
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.RoleAccessGuard
import com.example.ui.auth.WcuSplashScreen
import com.example.ui.navigation.AdminNavigationGraph
import com.example.ui.navigation.AuthNavigationGraph
import com.example.ui.navigation.StudentNavigationGraph
import com.example.ui.navigation.WcuRoutes
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WcuBlue100
import com.example.ui.theme.WcuGold500
import com.example.ui.theme.WcuNavy800
import com.example.ui.theme.WcuNavy900
import com.example.ui.theme.WcuWhite

enum class Phase1Screen {
  WELCOME,
  LOGIN,
  REGISTER,
  FORGOT_PASSWORD,
  ARCHITECTURE,
}

private val EVALUATION_PILLARS = listOf(
  "01" to "Teaching Quality",
  "02" to "Communication Skills",
  "03" to "Punctuality & Preparation",
  "04" to "Course Management",
  "05" to "Student Support",
  "06" to "Professionalism & Ethics",
)

private val FACULTIES_AND_DEPTS = mapOf(
  "fac_eng_tech" to ("College of Engineering and Technology (CET)" to listOf(
    "dep_se" to "Software Engineering (SEng)",
    "dep_cs" to "Computer Science (CS)",
    "dep_it" to "Information Technology (IT)",
    "dep_ece" to "Electrical and Computer Engineering (ECE)",
    "dep_civil" to "Civil Engineering (CEng)",
  )),
  "fac_nat_comp" to ("College of Natural and Computational Sciences (CNCS)" to listOf(
    "dep_math" to "Mathematics (MATH)",
    "dep_stat" to "Statistics (STAT)",
  )),
  "fac_med_health" to ("College of Medicine and Health Sciences (CMHS)" to listOf(
    "dep_nurs" to "Comprehensive Nursing (NURS)",
    "dep_ph" to "Public Health Officer (PH)",
  )),
  "fac_bus_econ" to ("College of Business and Economics (CBE)" to listOf(
    "dep_acct" to "Accounting and Finance (ACCT)",
    "dep_mgmt" to "Management (MGMT)",
  )),
  "fac_soc_hum" to ("College of Social Sciences and Humanities (CSSH)" to listOf(
    "dep_law" to "School of Law (LAW)",
  )),
  "fac_agri" to ("College of Agricultural Sciences (CANR)" to listOf(
    "dep_plant" to "Plant Science (PLSC)",
  )),
)

private val PHASE2_FILES = listOf(
  "app/google-services.json" to "Provisioned Firebase Android config (project: gen-lang-client-0559010770)",
  "app/src/main/res/values/firebase_applet_config.xml" to "Provisioned Firestore enterprise database ID",
  "firebase-blueprint.json & firestore.rules" to "Zero-trust Firestore Security Rules enforcing student role & PII isolation",
  "app/src/main/java/com/example/data/model/UserProfile.kt" to "Firestore UserProfile model (@DocumentId, @ServerTimestamp, defaults)",
  "app/src/main/java/com/example/data/repository/UserProfileRepository.kt" to "Named-database Firestore repository with structured error logging",
  "app/src/main/java/com/example/auth/GoogleCredentialAuthHelper.kt" to "Google Sign-In & Sign-Out via Jetpack Credential Manager",
  "app/src/main/java/com/example/ui/auth/AuthViewModel.kt" to "Centralized Auth state management & role verification",
  "app/src/test/java/com/example/data/repository/UserProfileRepositoryRuleTest.kt" to "Robolectric + Emulator integration tests for all 13 security scenarios",
)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val systemDark = isSystemInDarkTheme()
      var isDarkOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
      val useDark = isDarkOverride ?: systemDark

      MyApplicationTheme(darkTheme = useDark, dynamicColor = false) {
        WcuPhase2AppRoot(
          isDark = useDark,
          onToggleDark = { isDarkOverride = !useDark },
        )
      }
    }
  }
}

@Composable
fun WcuPhase2AppRoot(
  isDark: Boolean,
  onToggleDark: () -> Unit,
  authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()
  val isBusy by authViewModel.isBusy.collectAsStateWithLifecycle()
  val feedbackBanner by authViewModel.feedbackBanner.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) {
    GoogleCredentialAuthHelper.attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = { user ->
        authViewModel.resolveAuthenticatedUserSession(user)
      },
      onUnauthenticated = {},
      scope = coroutineScope,
    )
  }

  when (val state = sessionState) {
    AuthSessionState.Loading -> {
      WcuSplashScreen()
    }

    is AuthSessionState.AuthenticatedStudent -> {
      if (RoleAccessGuard.canAccessStudentGraph(state)) {
        key(state.profile.uid) {
          StudentNavigationGraph(
            profile = state.profile,
            emailVerified = state.emailVerified,
            isDark = isDark,
            onToggleDark = onToggleDark,
            feedbackBanner = feedbackBanner,
            onClearBanner = authViewModel::clearBanner,
            onSendVerification = authViewModel::sendEmailVerification,
            onSignOut = {
              authViewModel.signOutCurrentSession()
              GoogleCredentialAuthHelper.signOut(
                credentialManager = credentialManager,
                scope = coroutineScope,
                onSignOutComplete = {
                  authViewModel.setSuccessBanner("You have signed out safely.")
                },
              )
            },
          )
        }
      }
    }

    is AuthSessionState.AuthenticatedAdmin -> {
      if (RoleAccessGuard.canAccessAdminGraph(state)) {
        key(state.profile.uid, state.adminRecord.role, state.adminRecord.departmentId) {
          AdminNavigationGraph(
            profile = state.profile,
            adminRecord = state.adminRecord,
            emailVerified = state.emailVerified,
            isDark = isDark,
            onToggleDark = onToggleDark,
            feedbackBanner = feedbackBanner,
            onClearBanner = authViewModel::clearBanner,
            onSignOut = {
              authViewModel.signOutCurrentSession()
              GoogleCredentialAuthHelper.signOut(
                credentialManager = credentialManager,
                scope = coroutineScope,
                onSignOutComplete = {
                  authViewModel.setSuccessBanner("Administrator session signed out.")
                },
              )
            },
          )
        }
      }
    }

    is AuthSessionState.NeedsStudentOnboarding -> {
      AuthNavigationGraph(
        isDark = isDark,
        onToggleDark = onToggleDark,
        startDestination = WcuRoutes.ROUTE_STUDENT_REGISTRATION,
        onboardingState = state,
        isBusy = isBusy,
        feedbackBanner = feedbackBanner,
        onClearBanner = authViewModel::clearBanner,
        onGoogleSignIn = { targetRole ->
          GoogleCredentialAuthHelper.onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { user ->
              authViewModel.resolveAuthenticatedUserSession(user, targetRole)
            },
            onAuthError = authViewModel::setErrorBanner,
            scope = coroutineScope,
          )
        },
        onCompleteRegistration = authViewModel::completeStudentRegistration,
        onSendPasswordReset = authViewModel::sendPasswordResetEmail,
        onDemoStudentLogin = authViewModel::loginAsDemoStudent,
        onDemoAdminLogin = authViewModel::loginAsDemoDepartmentAdmin,
        onDemoSuperAdminLogin = authViewModel::loginAsDemoSuperAdmin,
      )
    }

    AuthSessionState.Unauthenticated -> {
      AuthNavigationGraph(
        isDark = isDark,
        onToggleDark = onToggleDark,
        startDestination = WcuRoutes.ROUTE_WELCOME,
        onboardingState = null,
        isBusy = isBusy,
        feedbackBanner = feedbackBanner,
        onClearBanner = authViewModel::clearBanner,
        onGoogleSignIn = { targetRole ->
          GoogleCredentialAuthHelper.onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { user ->
              authViewModel.resolveAuthenticatedUserSession(user, targetRole)
            },
            onAuthError = authViewModel::setErrorBanner,
            scope = coroutineScope,
          )
        },
        onCompleteRegistration = authViewModel::completeStudentRegistration,
        onSendPasswordReset = authViewModel::sendPasswordResetEmail,
        onDemoStudentLogin = authViewModel::loginAsDemoStudent,
        onDemoAdminLogin = authViewModel::loginAsDemoDepartmentAdmin,
        onDemoSuperAdminLogin = authViewModel::loginAsDemoSuperAdmin,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WcuUnauthenticatedShell(
  isDark: Boolean,
  onToggleDark: () -> Unit,
  initialScreen: Phase1Screen,
  onboardingState: AuthSessionState.NeedsStudentOnboarding?,
  isBusy: Boolean,
  feedbackBanner: Pair<Boolean, String>?,
  onClearBanner: () -> Unit,
  onGoogleSignIn: (String) -> Unit,
  onCompleteRegistration: (String, String, String, String, String, Int) -> Unit,
  onSendPasswordReset: (String) -> Unit,
) {
  var currentScreen by rememberSaveable(initialScreen) { mutableStateOf(initialScreen) }
  var loginRole by rememberSaveable { mutableStateOf(UserProfile.ROLE_STUDENT) }

  if (currentScreen != Phase1Screen.WELCOME) {
    BackHandler { currentScreen = Phase1Screen.WELCOME }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        navigationIcon = {
          if (currentScreen != Phase1Screen.WELCOME) {
            IconButton(
              onClick = { currentScreen = Phase1Screen.WELCOME },
              modifier = Modifier.testTag("nav_back_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = WcuWhite,
              )
            }
          }
        },
        title = {
          Column {
            Text(
              text = when (currentScreen) {
                Phase1Screen.WELCOME -> stringResource(R.string.university_name)
                Phase1Screen.LOGIN -> "Sign In"
                Phase1Screen.REGISTER -> "Student Registration"
                Phase1Screen.FORGOT_PASSWORD -> "Reset Password"
                Phase1Screen.ARCHITECTURE -> "Phase 2 Firebase Auth & Rules"
              },
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = WcuWhite,
            )
            Text(
              text = when (currentScreen) {
                Phase1Screen.WELCOME -> "Firebase Auth & Cloud Firestore Connected"
                Phase1Screen.LOGIN -> "Google Sign-In & Role Verification"
                Phase1Screen.REGISTER -> "Creates Verified /users/{uid} Profile"
                Phase1Screen.FORGOT_PASSWORD -> "Firebase Password Recovery"
                Phase1Screen.ARCHITECTURE -> "Zero-Trust Firestore Security Rules"
              },
              style = MaterialTheme.typography.labelSmall,
              color = WcuBlue100,
            )
          }
        },
        actions = {
          IconButton(
            onClick = onToggleDark,
            modifier = Modifier.testTag("theme_toggle_button"),
          ) {
            Icon(
              imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
              contentDescription = stringResource(R.string.switch_theme),
              tint = WcuWhite,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = WcuNavy900),
      )
    },
    bottomBar = {
      NavigationBar {
        NavigationBarItem(
          selected = currentScreen == Phase1Screen.WELCOME,
          onClick = { currentScreen = Phase1Screen.WELCOME },
          icon = { Icon(Icons.Default.Home, contentDescription = "Welcome") },
          label = { Text("Welcome") },
          modifier = Modifier.testTag("tab_welcome"),
        )
        NavigationBarItem(
          selected = currentScreen == Phase1Screen.LOGIN || currentScreen == Phase1Screen.FORGOT_PASSWORD,
          onClick = { currentScreen = Phase1Screen.LOGIN },
          icon = { Icon(Icons.AutoMirrored.Filled.Login, contentDescription = "Login") },
          label = { Text("Login") },
          modifier = Modifier.testTag("tab_login"),
        )
        NavigationBarItem(
          selected = currentScreen == Phase1Screen.REGISTER,
          onClick = { currentScreen = Phase1Screen.REGISTER },
          icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Register") },
          label = { Text("Register") },
          modifier = Modifier.testTag("tab_register"),
        )
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      when (currentScreen) {
        Phase1Screen.WELCOME -> WelcomePreviewScreen(
          onStudentLogin = {
            loginRole = UserProfile.ROLE_STUDENT
            currentScreen = Phase1Screen.LOGIN
          },
          onStudentRegister = { currentScreen = Phase1Screen.REGISTER },
          onAdminLogin = {
            loginRole = UserProfile.ROLE_ADMIN
            currentScreen = Phase1Screen.LOGIN
          },
        )

        Phase1Screen.LOGIN -> LoginPreviewScreen(
          selectedRole = loginRole,
          isBusy = isBusy,
          feedbackBanner = feedbackBanner,
          onClearBanner = onClearBanner,
          onRoleChange = { loginRole = it },
          onGoogleSignIn = { onGoogleSignIn(loginRole) },
          onForgotPassword = { currentScreen = Phase1Screen.FORGOT_PASSWORD },
          onGoToRegister = { currentScreen = Phase1Screen.REGISTER },
        )

        Phase1Screen.REGISTER -> RegisterPreviewScreen(
          onboardingState = onboardingState,
          isBusy = isBusy,
          feedbackBanner = feedbackBanner,
          onClearBanner = onClearBanner,
          onGoogleSignInFirst = { onGoogleSignIn(UserProfile.ROLE_STUDENT) },
          onSubmitProfile = onCompleteRegistration,
          onBackToLogin = {
            loginRole = UserProfile.ROLE_STUDENT
            currentScreen = Phase1Screen.LOGIN
          },
        )

        Phase1Screen.FORGOT_PASSWORD -> ForgotPasswordPreviewScreen(
          isBusy = isBusy,
          feedbackBanner = feedbackBanner,
          onSendPasswordReset = onSendPasswordReset,
          onBackToLogin = { currentScreen = Phase1Screen.LOGIN },
        )

        Phase1Screen.ARCHITECTURE -> Phase2ArchitectureScreen()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedStudentDashboard(
  profile: UserProfile,
  emailVerified: Boolean,
  isDark: Boolean,
  onToggleDark: () -> Unit,
  feedbackBanner: Pair<Boolean, String>?,
  onClearBanner: () -> Unit,
  onSendVerification: () -> Unit,
  onSignOut: () -> Unit,
) {
  var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

  if (showLogoutDialog) {
    AlertDialog(
      onDismissRequest = { showLogoutDialog = false },
      title = { Text("Confirm Sign Out") },
      text = {
        Text("Are you sure you want to sign out of your Wachemo University Student Portal session?")
      },
      confirmButton = {
        Button(
          onClick = {
            showLogoutDialog = false
            onSignOut()
          },
          modifier = Modifier.testTag("confirm_logout_button"),
        ) {
          Text("Sign Out")
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutDialog = false }) {
          Text("Cancel")
        }
      },
    )
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "WCU Student Portal",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = WcuWhite,
            )
            Text(
              text = "${profile.studentId} • Verified Student Session",
              style = MaterialTheme.typography.labelSmall,
              color = WcuBlue100,
            )
          }
        },
        actions = {
          IconButton(onClick = onToggleDark) {
            Icon(
              imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
              contentDescription = stringResource(R.string.switch_theme),
              tint = WcuWhite,
            )
          }
          IconButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.testTag("student_logout_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Sign Out",
              tint = WcuWhite,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = WcuNavy900),
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item { Spacer(modifier = Modifier.height(4.dp)) }

      item {
        WcuUniversityBanner(
          title = "Welcome, ${profile.fullName}",
          subtitle = "Authenticated Student Profile (/users/${profile.uid.take(8)}...)",
        )
      }

      if (feedbackBanner != null) {
        item {
          FeedbackBannerCard(
            isError = feedbackBanner.first,
            message = feedbackBanner.second,
            onDismiss = onClearBanner,
          )
        }
      }

      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "Firestore Student Profile Record",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
            }
            Text("UID: ${profile.uid}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            Text("Full Name: ${profile.fullName}", style = MaterialTheme.typography.bodyMedium)
            Text("Student ID: ${profile.studentId}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("Email: ${profile.email}", style = MaterialTheme.typography.bodyMedium)
            Text("Role: ${profile.role.uppercase()} (Locked by Firestore Security Rules)", style = MaterialTheme.typography.bodySmall)
            Text("Faculty ID: ${profile.facultyId} • Department ID: ${profile.departmentId}", style = MaterialTheme.typography.bodySmall)
            Text("Year of Study: Year ${profile.yearOfStudy} • Active: ${profile.isActive}", style = MaterialTheme.typography.bodySmall)
          }
        }
      }

      if (!emailVerified) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = "Email Verification",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Verify your institutional email address to receive evaluation notifications.",
                style = MaterialTheme.typography.bodySmall,
              )
              OutlinedButton(
                onClick = onSendVerification,
                modifier = Modifier.testTag("send_email_verification_button"),
              ) {
                Icon(Icons.Default.MarkEmailRead, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Verification Email")
              }
            }
          }
        }
      }

      item {
        OutlinedButton(
          onClick = { showLogoutDialog = true },
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("student_sign_out_Action_button"),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Sign Out of Student Portal")
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedAdminDashboard(
  profile: UserProfile,
  emailVerified: Boolean,
  isDark: Boolean,
  onToggleDark: () -> Unit,
  feedbackBanner: Pair<Boolean, String>?,
  onClearBanner: () -> Unit,
  onSignOut: () -> Unit,
) {
  var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

  if (showLogoutDialog) {
    AlertDialog(
      onDismissRequest = { showLogoutDialog = false },
      title = { Text("Confirm Administrator Sign Out") },
      text = {
        Text("Sign out of the Wachemo University Administrator Portal?")
      },
      confirmButton = {
        Button(
          onClick = {
            showLogoutDialog = false
            onSignOut()
          },
          modifier = Modifier.testTag("confirm_admin_logout_button"),
        ) {
          Text("Sign Out")
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutDialog = false }) {
          Text("Cancel")
        }
      },
    )
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "WCU Administrator Console",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = WcuWhite,
            )
            Text(
              text = "Verified Admin Role (${profile.email})",
              style = MaterialTheme.typography.labelSmall,
              color = WcuGold500,
            )
          }
        },
        actions = {
          IconButton(onClick = onToggleDark) {
            Icon(
              imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
              contentDescription = stringResource(R.string.switch_theme),
              tint = WcuWhite,
            )
          }
          IconButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.testTag("admin_logout_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Sign Out",
              tint = WcuWhite,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = WcuNavy900),
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item { Spacer(modifier = Modifier.height(4.dp)) }

      item {
        WcuUniversityBanner(
          title = "Administrator Portal: ${profile.fullName}",
          subtitle = "Role Verified via Firestore /admins Registry & Token Verification",
        )
      }

      if (feedbackBanner != null) {
        item {
          FeedbackBannerCard(
            isError = feedbackBanner.first,
            message = feedbackBanner.second,
            onDismiss = onClearBanner,
          )
        }
      }

      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "Verified Administrator Session",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text("UID: ${profile.uid}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            Text("Email: ${profile.email} (Verified: $emailVerified)", style = MaterialTheme.typography.bodyMedium)
            Text("Role: ${profile.role.uppercase()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
          }
        }
      }

      item {
        OutlinedButton(
          onClick = { showLogoutDialog = true },
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Sign Out of Administrator Portal")
        }
      }
    }
  }
}

@Composable
fun FeedbackBannerCard(
  isError: Boolean,
  message: String,
  onDismiss: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
      } else {
        MaterialTheme.colorScheme.primaryContainer
      },
    ),
    shape = RoundedCornerShape(12.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) {
          MaterialTheme.colorScheme.onErrorContainer
        } else {
          MaterialTheme.colorScheme.onPrimaryContainer
        },
        modifier = Modifier.weight(1f),
      )
      TextButton(onClick = onDismiss) {
        Text("Dismiss")
      }
    }
  }
}

@Composable
fun WcuUniversityBanner(
  title: String = "Wachemo University",
  subtitle: String = "Teacher Evaluation & Academic Quality Assurance System",
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = WcuNavy900),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(WcuNavy800)
            .border(1.dp, WcuGold500.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.School,
            contentDescription = null,
            tint = WcuGold500,
          )
        }
        Surface(
          color = WcuNavy800,
          shape = RoundedCornerShape(999.dp),
        ) {
          Text(
            text = stringResource(R.string.campus_tag),
            style = MaterialTheme.typography.labelSmall,
            color = WcuGold500,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = WcuWhite,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = WcuBlue100,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomePreviewScreen(
  onDemoStudentLogin: () -> Unit = {},
  onDemoAdminLogin: () -> Unit = {},
  onDemoSuperAdminLogin: () -> Unit = {},
  onStudentLogin: () -> Unit = {},
  onStudentRegister: () -> Unit = {},
  onAdminLogin: () -> Unit = {},
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 600.dp)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item { Spacer(modifier = Modifier.height(4.dp)) }

    item { WcuUniversityBanner() }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = stringResource(R.string.welcome_portal_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = stringResource(R.string.welcome_portal_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "3-Tier Role Access: Student, Department Admin & Super Admin",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }

    // Login & Registration Pages Section
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Account Login & Registration",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          Button(
            onClick = onStudentLogin,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("welcome_student_login_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.student_sign_in))
          }

          OutlinedButton(
            onClick = onStudentRegister,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("welcome_student_register_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.register_student_account))
          }

          OutlinedButton(
            onClick = onAdminLogin,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("welcome_admin_login_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.admin_sign_in))
          }
        }
      }
    }

    // Quick Demo Login Section (Preserved)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Instant Demo Access",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          Button(
            onClick = onDemoStudentLogin,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("demo_student_login_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Default.School, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Demo Student Login")
          }

          Button(
            onClick = onDemoAdminLogin,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("demo_admin_login_button"),
            colors = ButtonDefaults.buttonColors(containerColor = WcuNavy800, contentColor = WcuWhite),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Demo Admin Login (Department Admin)")
          }

          Button(
            onClick = onDemoSuperAdminLogin,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("demo_super_admin_login_button"),
            colors = ButtonDefaults.buttonColors(containerColor = WcuNavy900, contentColor = WcuGold500),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Default.Security, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Demo Super Admin Login")
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
fun LoginPreviewScreen(
  selectedRole: String,
  isBusy: Boolean,
  feedbackBanner: Pair<Boolean, String>?,
  onClearBanner: () -> Unit,
  onRoleChange: (String) -> Unit,
  onGoogleSignIn: () -> Unit,
  onForgotPassword: () -> Unit,
  onGoToRegister: () -> Unit,
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 600.dp)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item { Spacer(modifier = Modifier.height(4.dp)) }

    item {
      WcuUniversityBanner(
        title = if (selectedRole == UserProfile.ROLE_ADMIN) {
          "Administrator Portal Sign In"
        } else {
          "Student Portal Sign In"
        },
        subtitle = "Authenticate via Firebase Auth & Google Credential Manager",
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = selectedRole == UserProfile.ROLE_STUDENT,
          onClick = {
            onRoleChange(UserProfile.ROLE_STUDENT)
            onClearBanner()
          },
          label = { Text("Student Portal") },
          modifier = Modifier
            .weight(1f)
            .testTag("login_role_student_tab"),
        )
        FilterChip(
          selected = selectedRole == UserProfile.ROLE_ADMIN,
          onClick = {
            onRoleChange(UserProfile.ROLE_ADMIN)
            onClearBanner()
          },
          label = { Text("Administrator Portal") },
          modifier = Modifier
            .weight(1f)
            .testTag("login_role_admin_tab"),
        )
      }
    }

    if (selectedRole == UserProfile.ROLE_ADMIN) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
          ),
          shape = RoundedCornerShape(12.dp),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
              text = "Restricted Access: Administrator accounts are verified against Firestore /admins/{uid} and verified admin tokens. Unauthorized accounts are automatically denied.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }
      }
    }

    if (feedbackBanner != null) {
      item {
        FeedbackBannerCard(
          isError = feedbackBanner.first,
          message = feedbackBanner.second,
          onDismiss = onClearBanner,
        )
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = if (selectedRole == UserProfile.ROLE_ADMIN) {
              "Sign in with your authorized Wachemo University Administrator Google Account"
            } else {
              "Sign in with your Wachemo University Google Account"
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
          )

          Button(
            onClick = onGoogleSignIn,
            enabled = !isBusy,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("google_sign_in_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            if (isBusy) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
              )
            } else {
              Icon(Icons.Default.AccountCircle, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                if (selectedRole == UserProfile.ROLE_ADMIN) {
                  "Sign in with Google (Verify Admin)"
                } else {
                  "Sign in with Google"
                },
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
          ) {
            TextButton(
              onClick = onForgotPassword,
              modifier = Modifier.testTag("login_forgot_password_link"),
            ) {
              Text(stringResource(R.string.forgot_password))
            }
          }
        }
      }
    }

    if (selectedRole == UserProfile.ROLE_STUDENT) {
      item {
        OutlinedButton(
          onClick = onGoToRegister,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("login_go_to_register_button"),
          shape = RoundedCornerShape(12.dp),
        ) {
          Text("Complete Student Profile Registration")
        }
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPreviewScreen(
  onboardingState: AuthSessionState.NeedsStudentOnboarding?,
  isBusy: Boolean,
  feedbackBanner: Pair<Boolean, String>?,
  onClearBanner: () -> Unit,
  onGoogleSignInFirst: () -> Unit,
  onSubmitProfile: (String, String, String, String, String, Int) -> Unit,
  onBackToLogin: () -> Unit,
) {
  var fullName by rememberSaveable(onboardingState?.displayName) {
    mutableStateOf(onboardingState?.displayName.orEmpty())
  }
  var studentId by rememberSaveable { mutableStateOf("") }
  var email by rememberSaveable(onboardingState?.email) {
    mutableStateOf(onboardingState?.email.orEmpty())
  }
  val facultyKeys = FACULTIES_AND_DEPTS.keys.toList()
  var selectedFacultyId by rememberSaveable { mutableStateOf(facultyKeys.first()) }
  val availableDepts = FACULTIES_AND_DEPTS[selectedFacultyId]?.second.orEmpty()
  var selectedDepartmentId by remember(selectedFacultyId) {
    mutableStateOf(availableDepts.firstOrNull()?.first.orEmpty())
  }
  var selectedYear by rememberSaveable { mutableStateOf(3) }

  var facultyExpanded by remember { mutableStateOf(false) }
  var deptExpanded by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 600.dp)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item { Spacer(modifier = Modifier.height(4.dp)) }

    item {
      WcuUniversityBanner(
        title = "Student Profile Registration",
        subtitle = "Creates a verified 'student' role document at /users/{uid}",
      )
    }

    if (feedbackBanner != null) {
      item {
        FeedbackBannerCard(
          isError = feedbackBanner.first,
          message = feedbackBanner.second,
          onDismiss = onClearBanner,
        )
      }
    }

    if (onboardingState == null) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
          ),
          shape = RoundedCornerShape(14.dp),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Step 1: Authenticate with Google",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
              text = "Authenticate your Google account first via Credential Manager, then submit your Wachemo University Student ID and academic placement to Cloud Firestore.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Button(
              onClick = onGoogleSignInFirst,
              enabled = !isBusy,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("register_google_sign_in_button"),
            ) {
              Icon(Icons.Default.AccountCircle, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Sign in with Google to Register")
            }
          }
        }
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "Student Academic Profile (/users/{uid})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )

          OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            placeholder = { Text("e.g., Abebe Kebede") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("register_full_name_input"),
          )

          OutlinedTextField(
            value = studentId,
            onValueChange = { studentId = it },
            label = { Text("Wachemo University Student ID") },
            placeholder = { Text("e.g., WCU/1420/16") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("register_student_id_input"),
          )

          OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Institutional Email Address") },
            placeholder = { Text("student.name@wcu.edu.et") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("register_email_input"),
          )

          ExposedDropdownMenuBox(
            expanded = facultyExpanded,
            onExpandedChange = { facultyExpanded = !facultyExpanded },
          ) {
            OutlinedTextField(
              value = FACULTIES_AND_DEPTS[selectedFacultyId]?.first.orEmpty(),
              onValueChange = {},
              readOnly = true,
              label = { Text("College / Faculty") },
              trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = facultyExpanded)
              },
              modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            )
            ExposedDropdownMenu(
              expanded = facultyExpanded,
              onDismissRequest = { facultyExpanded = false },
            ) {
              FACULTIES_AND_DEPTS.forEach { (facId, pair) ->
                DropdownMenuItem(
                  text = { Text(pair.first) },
                  onClick = {
                    selectedFacultyId = facId
                    facultyExpanded = false
                  },
                )
              }
            }
          }

          ExposedDropdownMenuBox(
            expanded = deptExpanded,
            onExpandedChange = { deptExpanded = !deptExpanded },
          ) {
            val selectedDeptLabel =
              availableDepts.firstOrNull { it.first == selectedDepartmentId }?.second.orEmpty()
            OutlinedTextField(
              value = selectedDeptLabel,
              onValueChange = {},
              readOnly = true,
              label = { Text("Academic Department") },
              trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded)
              },
              modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            )
            ExposedDropdownMenu(
              expanded = deptExpanded,
              onDismissRequest = { deptExpanded = false },
            ) {
              availableDepts.forEach { (depId, depName) ->
                DropdownMenuItem(
                  text = { Text(depName) },
                  onClick = {
                    selectedDepartmentId = depId
                    deptExpanded = false
                  },
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            (1..5).forEach { yr ->
              FilterChip(
                selected = selectedYear == yr,
                onClick = { selectedYear = yr },
                label = { Text("Yr $yr") },
              )
            }
          }

          Button(
            onClick = {
              onSubmitProfile(
                fullName,
                email,
                studentId,
                selectedFacultyId,
                selectedDepartmentId,
                selectedYear,
              )
            },
            enabled = !isBusy,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("register_submit_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            if (isBusy) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
              )
            } else {
              Text("Save Student Profile to Cloud Firestore")
            }
          }
        }
      }
    }

    item {
      OutlinedButton(
        onClick = onBackToLogin,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("register_back_to_login_button"),
        shape = RoundedCornerShape(12.dp),
      ) {
        Text("Back to Student Sign In")
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
fun ForgotPasswordPreviewScreen(
  isBusy: Boolean,
  feedbackBanner: Pair<Boolean, String>?,
  onSendPasswordReset: (String) -> Unit,
  onBackToLogin: () -> Unit,
) {
  var email by rememberSaveable { mutableStateOf("") }

  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 600.dp)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item { Spacer(modifier = Modifier.height(4.dp)) }

    item {
      WcuUniversityBanner(
        title = "Password Recovery",
        subtitle = "Send a Firebase Authentication password reset email",
      )
    }

    if (feedbackBanner != null) {
      item {
        FeedbackBannerCard(
          isError = feedbackBanner.first,
          message = feedbackBanner.second,
          onDismiss = {},
        )
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Registered Email Address") },
            placeholder = { Text("student.name@wcu.edu.et") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("forgot_password_email_input"),
          )

          Button(
            onClick = { onSendPasswordReset(email) },
            enabled = !isBusy,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("forgot_password_submit_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Text("Send Password Reset Link")
          }
        }
      }
    }

    item {
      OutlinedButton(
        onClick = onBackToLogin,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("forgot_password_back_button"),
        shape = RoundedCornerShape(12.dp),
      ) {
        Text("Return to Sign In")
      }
    }
  }
}

@Composable
fun Phase2ArchitectureScreen() {
  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 600.dp)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item { Spacer(modifier = Modifier.height(4.dp)) }

    item {
      WcuUniversityBanner(
        title = "Phase 2 — Firebase Auth & Firestore Security",
        subtitle = "Provisioned Project: gen-lang-client-0559010770",
      )
    }

    items(PHASE2_FILES) { (path, desc) ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.Top,
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = path,
              style = MaterialTheme.typography.bodyMedium,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = desc,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}
