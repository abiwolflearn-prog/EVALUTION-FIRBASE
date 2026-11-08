package com.example.ui.navigation

/**
 * Defines the eight major UI sections of the Wachemo University Teacher Evaluation Application.
 *
 * Authentication sections (1..5) are strictly isolated in [AuthNavigationGraph].
 * Student sections (6..7) are strictly isolated in [StudentNavigationGraph].
 * Administrator section (8) is strictly isolated in [AdminNavigationGraph].
 */
enum class WcuMajorUiSection(
  val sectionNumber: Int,
  val title: String,
  val route: String,
  val requiresAuthentication: Boolean,
  val requiredRole: String?,
) {
  SPLASH_SCREEN(
    sectionNumber = 1,
    title = "Splash Screen",
    route = WcuRoutes.ROUTE_SPLASH,
    requiresAuthentication = false,
    requiredRole = null,
  ),
  WELCOME_ROLE_SELECTION(
    sectionNumber = 2,
    title = "Welcome / Role Selection",
    route = WcuRoutes.ROUTE_WELCOME,
    requiresAuthentication = false,
    requiredRole = null,
  ),
  STUDENT_REGISTRATION(
    sectionNumber = 3,
    title = "Student Registration",
    route = WcuRoutes.ROUTE_STUDENT_REGISTRATION,
    requiresAuthentication = false,
    requiredRole = null,
  ),
  LOGIN(
    sectionNumber = 4,
    title = "Login",
    route = WcuRoutes.ROUTE_LOGIN,
    requiresAuthentication = false,
    requiredRole = null,
  ),
  FORGOT_PASSWORD(
    sectionNumber = 5,
    title = "Forgot Password",
    route = WcuRoutes.ROUTE_FORGOT_PASSWORD,
    requiresAuthentication = false,
    requiredRole = null,
  ),
  STUDENT_DASHBOARD(
    sectionNumber = 6,
    title = "Student Dashboard",
    route = WcuRoutes.ROUTE_STUDENT_DASHBOARD,
    requiresAuthentication = true,
    requiredRole = "student",
  ),
  TEACHER_EVALUATION(
    sectionNumber = 7,
    title = "Teacher Evaluation",
    route = WcuRoutes.ROUTE_TEACHER_EVALUATION,
    requiresAuthentication = true,
    requiredRole = "student",
  ),
  ADMINISTRATOR_DASHBOARD(
    sectionNumber = 8,
    title = "Administrator Dashboard",
    route = WcuRoutes.ROUTE_ADMIN_DASHBOARD,
    requiresAuthentication = true,
    requiredRole = "admin",
  ),
}

object WcuRoutes {
  // Unauthenticated Auth Navigation Graph Routes (Sections 1-5)
  const val ROUTE_SPLASH = "splash_screen"
  const val ROUTE_WELCOME = "welcome_role_selection"
  const val ROUTE_STUDENT_REGISTRATION = "student_registration"
  const val ROUTE_LOGIN = "login"
  const val ROUTE_FORGOT_PASSWORD = "forgot_password"
  const val ROUTE_ARCHITECTURE_INFO = "phase2_architecture_info"

  // Authenticated Student Navigation Graph Routes (Sections 6-7)
  const val ROUTE_STUDENT_DASHBOARD = "student_dashboard"
  const val ROUTE_TEACHER_EVALUATION = "teacher_evaluation"
  const val ROUTE_TEACHER_EVALUATION_WITH_ARGS = "teacher_evaluation/{courseId}/{teacherId}"

  fun teacherEvaluationRoute(courseId: String, teacherId: String): String {
    return "teacher_evaluation/$courseId/$teacherId"
  }

  // Authenticated Administrator Navigation Graph Routes (Section 8)
  const val ROUTE_ADMIN_DASHBOARD = "administrator_dashboard"
  const val ROUTE_SUPER_ADMIN_DASHBOARD = "super_admin_dashboard"
  const val ROUTE_DEPARTMENT_ADMIN_DASHBOARD = "department_admin_dashboard"
}
