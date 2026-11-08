package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.model.AdminAccountRecord
import com.example.data.model.UserProfile
import com.example.ui.admin.DepartmentAdminDashboardScreen
import com.example.ui.admin.SuperAdminDashboardScreen

/**
 * Separate Administrator Navigation Graph (Section 8):
 * - Super Admin Experience ([WcuRoutes.ROUTE_SUPER_ADMIN_DASHBOARD])
 * - Department Admin Experience ([WcuRoutes.ROUTE_DEPARTMENT_ADMIN_DASHBOARD])
 *
 * Strictly requires an active, verified Administrator profile (`super_admin` or `department_admin`).
 * - Students can NEVER access this navigation graph or any administrator screen.
 * - Department Admins can NEVER access [WcuRoutes.ROUTE_SUPER_ADMIN_DASHBOARD].
 * - Inactive administrators (`isActive == false`) are immediately rejected.
 */
@Composable
fun AdminNavigationGraph(
  profile: UserProfile,
  emailVerified: Boolean,
  isDark: Boolean,
  onToggleDark: () -> Unit,
  feedbackBanner: Pair<Boolean, String>?,
  onClearBanner: () -> Unit,
  onSignOut: () -> Unit,
  adminRecord: AdminAccountRecord =
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
) {
  require(profile.isAdminRole && profile.isActive && adminRecord.isActive && !profile.isStudentRole) {
    "Unauthorized access to AdminNavigationGraph: user role '${profile.role}' (active=${adminRecord.isActive}) is not an active verified administrator."
  }

  val startRoute =
    if (adminRecord.isDepartmentAdmin && profile.isDepartmentAdminRole) {
      WcuRoutes.ROUTE_DEPARTMENT_ADMIN_DASHBOARD
    } else {
      WcuRoutes.ROUTE_SUPER_ADMIN_DASHBOARD
    }

  val navController = rememberNavController()

  NavHost(
    navController = navController,
    startDestination = startRoute,
    modifier = Modifier
      .fillMaxSize()
      .testTag("admin_navigation_graph"),
  ) {
    // Level 1: Super Admin Dashboard
    composable(WcuRoutes.ROUTE_SUPER_ADMIN_DASHBOARD) {
      require(adminRecord.isSuperAdmin && profile.isSuperAdminRole && !profile.isDepartmentAdminRole) {
        "Unauthorized access to SuperAdminDashboardScreen by role '${profile.role}'."
      }
      SuperAdminDashboardScreen(
        profile = profile,
        adminRecord = adminRecord,
        emailVerified = emailVerified,
        isDark = isDark,
        onToggleDark = onToggleDark,
        feedbackBanner = feedbackBanner,
        onClearBanner = onClearBanner,
        onSignOut = onSignOut,
      )
    }

    // Level 2: Department Admin Dashboard
    composable(WcuRoutes.ROUTE_DEPARTMENT_ADMIN_DASHBOARD) {
      require(adminRecord.isDepartmentAdmin && profile.isDepartmentAdminRole) {
        "Unauthorized access to DepartmentAdminDashboardScreen by role '${profile.role}'."
      }
      DepartmentAdminDashboardScreen(
        profile = profile,
        adminRecord = adminRecord,
        emailVerified = emailVerified,
        isDark = isDark,
        onToggleDark = onToggleDark,
        feedbackBanner = feedbackBanner,
        onClearBanner = onClearBanner,
        onSignOut = onSignOut,
      )
    }
  }
}
