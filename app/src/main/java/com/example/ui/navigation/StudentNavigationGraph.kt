package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.UserProfile
import com.example.ui.student.StudentDashboardViewModel
import com.example.ui.student.StudentPortalScreen
import com.example.ui.student.TeacherEvaluationFormScreen

/**
 * Separate Student Navigation Graph (Sections 6–7):
 * - Section 6: Student Dashboard ([WcuRoutes.ROUTE_STUDENT_DASHBOARD])
 * - Section 7: Teacher Evaluation ([WcuRoutes.ROUTE_TEACHER_EVALUATION_WITH_ARGS] & [WcuRoutes.ROUTE_TEACHER_EVALUATION])
 *
 * Strictly requires a verified active Student profile (`role == "student"`).
 * Administrators never enter this graph or receive student evaluation permissions.
 */
@Composable
fun StudentNavigationGraph(
  profile: UserProfile,
  emailVerified: Boolean,
  isDark: Boolean,
  onToggleDark: () -> Unit,
  feedbackBanner: Pair<Boolean, String>?,
  onClearBanner: () -> Unit,
  onSendVerification: () -> Unit,
  onSignOut: () -> Unit,
) {
  require(profile.isStudentRole && profile.role == UserProfile.ROLE_STUDENT && !profile.isAdminRole) {
    "Unauthorized access to StudentNavigationGraph: user role '${profile.role}' is not a verified student."
  }

  val navController = rememberNavController()
  val dashboardViewModel: StudentDashboardViewModel =
    viewModel(
      key = "student_dashboard_${profile.uid}",
      factory = StudentDashboardViewModel.provideFactory(profile),
    )

  NavHost(
    navController = navController,
    startDestination = WcuRoutes.ROUTE_STUDENT_DASHBOARD,
    modifier = Modifier
      .fillMaxSize()
      .testTag("student_navigation_graph"),
  ) {
    // Section 6: Student Dashboard
    composable(WcuRoutes.ROUTE_STUDENT_DASHBOARD) {
      StudentPortalScreen(
        profile = profile,
        emailVerified = emailVerified,
        isDark = isDark,
        onToggleDark = onToggleDark,
        feedbackBanner = feedbackBanner,
        onClearBanner = onClearBanner,
        onSendVerification = onSendVerification,
        onSignOut = onSignOut,
        onNavigateToTeacherEvaluation = { courseId, teacherId ->
          navController.navigate(WcuRoutes.teacherEvaluationRoute(courseId, teacherId))
        },
        dashboardViewModel = dashboardViewModel,
      )
    }

    // Section 7: Interactive 6-Category, 18-Question Teacher Evaluation Form
    composable(
      route = WcuRoutes.ROUTE_TEACHER_EVALUATION_WITH_ARGS,
      arguments =
        listOf(
          navArgument("courseId") { type = NavType.StringType },
          navArgument("teacherId") { type = NavType.StringType },
        ),
    ) { backStackEntry ->
      val courseId = backStackEntry.arguments?.getString("courseId").orEmpty()
      val teacherId = backStackEntry.arguments?.getString("teacherId").orEmpty()
      TeacherEvaluationFormScreen(
        studentProfile = profile,
        courseId = courseId,
        teacherId = teacherId,
        dashboardViewModel = dashboardViewModel,
        onBackToDashboard = { navController.popBackStack() },
      )
    }
  }
}
