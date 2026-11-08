package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ForgotPasswordPreviewScreen
import com.example.LoginPreviewScreen
import com.example.Phase2ArchitectureScreen
import com.example.R
import com.example.RegisterPreviewScreen
import com.example.WelcomePreviewScreen
import com.example.data.model.UserProfile
import com.example.ui.auth.AuthSessionState
import com.example.ui.theme.WcuBlue100
import com.example.ui.theme.WcuNavy900
import com.example.ui.theme.WcuWhite

/**
 * Unauthenticated Navigation Graph (Sections 2–5):
 * - Section 2: Welcome / Role Selection ([WcuRoutes.ROUTE_WELCOME])
 * - Section 3: Student Registration ([WcuRoutes.ROUTE_STUDENT_REGISTRATION])
 * - Section 4: Login ([WcuRoutes.ROUTE_LOGIN])
 * - Section 5: Forgot Password ([WcuRoutes.ROUTE_FORGOT_PASSWORD])
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthNavigationGraph(
  isDark: Boolean,
  onToggleDark: () -> Unit,
  startDestination: String = WcuRoutes.ROUTE_WELCOME,
  onboardingState: AuthSessionState.NeedsStudentOnboarding?,
  isBusy: Boolean,
  feedbackBanner: Pair<Boolean, String>?,
  onClearBanner: () -> Unit,
  onGoogleSignIn: (String) -> Unit,
  onCompleteRegistration: (String, String, String, String, String, Int) -> Unit,
  onSendPasswordReset: (String) -> Unit,
  onDemoStudentLogin: () -> Unit = {},
  onDemoAdminLogin: () -> Unit = {},
  onDemoSuperAdminLogin: () -> Unit = {},
) {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: startDestination
  var loginRole by rememberSaveable { mutableStateOf(UserProfile.ROLE_STUDENT) }

  LaunchedEffect(onboardingState) {
    if (onboardingState != null && currentRoute != WcuRoutes.ROUTE_STUDENT_REGISTRATION) {
      navController.navigate(WcuRoutes.ROUTE_STUDENT_REGISTRATION) {
        launchSingleTop = true
      }
    }
  }

  fun navigateSingleTop(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .testTag("auth_navigation_graph"),
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        navigationIcon = {
          if (currentRoute != WcuRoutes.ROUTE_WELCOME) {
            IconButton(
              onClick = {
                if (!navController.popBackStack()) {
                  navigateSingleTop(WcuRoutes.ROUTE_WELCOME)
                }
              },
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
              text =
                when (currentRoute) {
                  WcuRoutes.ROUTE_WELCOME -> stringResource(R.string.university_name)
                  WcuRoutes.ROUTE_LOGIN -> "Sign In"
                  WcuRoutes.ROUTE_STUDENT_REGISTRATION -> "Student Registration"
                  WcuRoutes.ROUTE_FORGOT_PASSWORD -> "Reset Password"
                  else -> stringResource(R.string.university_name)
                },
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = WcuWhite,
            )
            Text(
              text =
                when (currentRoute) {
                  WcuRoutes.ROUTE_WELCOME -> "Role Selection • Student, Dept Admin & Super Admin"
                  WcuRoutes.ROUTE_LOGIN -> "Google Sign-In & Role Verification"
                  WcuRoutes.ROUTE_STUDENT_REGISTRATION -> "Creates Verified /users/{uid} Profile"
                  WcuRoutes.ROUTE_FORGOT_PASSWORD -> "Firebase Password Recovery"
                  else -> "Zero-Trust Firestore Security Rules"
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
          selected = currentRoute == WcuRoutes.ROUTE_WELCOME,
          onClick = { navigateSingleTop(WcuRoutes.ROUTE_WELCOME) },
          icon = { Icon(Icons.Default.Home, contentDescription = "Welcome") },
          label = { Text("Welcome") },
          modifier = Modifier.testTag("tab_welcome"),
        )
        NavigationBarItem(
          selected =
            currentRoute == WcuRoutes.ROUTE_LOGIN ||
              currentRoute == WcuRoutes.ROUTE_FORGOT_PASSWORD,
          onClick = { navigateSingleTop(WcuRoutes.ROUTE_LOGIN) },
          icon = { Icon(Icons.AutoMirrored.Filled.Login, contentDescription = "Login") },
          label = { Text("Login") },
          modifier = Modifier.testTag("tab_login"),
        )
        NavigationBarItem(
          selected = currentRoute == WcuRoutes.ROUTE_STUDENT_REGISTRATION,
          onClick = { navigateSingleTop(WcuRoutes.ROUTE_STUDENT_REGISTRATION) },
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
      NavHost(
        navController = navController,
        startDestination = startDestination,
      ) {
        // Section 2: Welcome / Role Selection (includes Demo Login + Login & Registration navigation)
        composable(WcuRoutes.ROUTE_WELCOME) {
          WelcomePreviewScreen(
            onDemoStudentLogin = onDemoStudentLogin,
            onDemoAdminLogin = onDemoAdminLogin,
            onDemoSuperAdminLogin = onDemoSuperAdminLogin,
            onStudentLogin = {
              loginRole = UserProfile.ROLE_STUDENT
              navigateSingleTop(WcuRoutes.ROUTE_LOGIN)
            },
            onStudentRegister = {
              navigateSingleTop(WcuRoutes.ROUTE_STUDENT_REGISTRATION)
            },
            onAdminLogin = {
              loginRole = UserProfile.ROLE_ADMIN
              navigateSingleTop(WcuRoutes.ROUTE_LOGIN)
            },
          )
        }

        // Section 3: Student Registration
        composable(WcuRoutes.ROUTE_STUDENT_REGISTRATION) {
          RegisterPreviewScreen(
            onboardingState = onboardingState,
            isBusy = isBusy,
            feedbackBanner = feedbackBanner,
            onClearBanner = onClearBanner,
            onGoogleSignInFirst = { onGoogleSignIn(UserProfile.ROLE_STUDENT) },
            onSubmitProfile = onCompleteRegistration,
            onBackToLogin = {
              loginRole = UserProfile.ROLE_STUDENT
              navigateSingleTop(WcuRoutes.ROUTE_LOGIN)
            },
          )
        }

        // Section 4: Login
        composable(WcuRoutes.ROUTE_LOGIN) {
          LoginPreviewScreen(
            selectedRole = loginRole,
            isBusy = isBusy,
            feedbackBanner = feedbackBanner,
            onClearBanner = onClearBanner,
            onRoleChange = { loginRole = it },
            onGoogleSignIn = { onGoogleSignIn(loginRole) },
            onForgotPassword = {
              navController.navigate(WcuRoutes.ROUTE_FORGOT_PASSWORD)
            },
            onGoToRegister = {
              navigateSingleTop(WcuRoutes.ROUTE_STUDENT_REGISTRATION)
            },
          )
        }

        // Section 5: Forgot Password
        composable(WcuRoutes.ROUTE_FORGOT_PASSWORD) {
          ForgotPasswordPreviewScreen(
            isBusy = isBusy,
            feedbackBanner = feedbackBanner,
            onSendPasswordReset = onSendPasswordReset,
            onBackToLogin = {
              if (!navController.popBackStack()) {
                navigateSingleTop(WcuRoutes.ROUTE_LOGIN)
              }
            },
          )
        }

        composable(WcuRoutes.ROUTE_ARCHITECTURE_INFO) {
          Phase2ArchitectureScreen()
        }
      }
    }
  }
}
