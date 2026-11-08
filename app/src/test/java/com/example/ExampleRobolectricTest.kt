package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.UserProfile
import com.example.ui.auth.AuthSessionState
import com.example.ui.auth.RoleAccessGuard
import com.example.ui.navigation.WcuMajorUiSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("WCU Evaluation", appName)
  }

  @Test
  fun `eight major UI sections are organized with strict auth student and admin separation`() {
    val sections = WcuMajorUiSection.entries
    assertEquals(8, sections.size)

    val authSections = sections.filter { !it.requiresAuthentication }
    val studentSections = sections.filter { it.requiredRole == UserProfile.ROLE_STUDENT }
    val adminSections = sections.filter { it.requiredRole == UserProfile.ROLE_ADMIN }

    assertEquals(
      listOf(
        WcuMajorUiSection.SPLASH_SCREEN,
        WcuMajorUiSection.WELCOME_ROLE_SELECTION,
        WcuMajorUiSection.STUDENT_REGISTRATION,
        WcuMajorUiSection.LOGIN,
        WcuMajorUiSection.FORGOT_PASSWORD,
      ),
      authSections,
    )
    assertEquals(
      listOf(
        WcuMajorUiSection.STUDENT_DASHBOARD,
        WcuMajorUiSection.TEACHER_EVALUATION,
      ),
      studentSections,
    )
    assertEquals(
      listOf(WcuMajorUiSection.ADMINISTRATOR_DASHBOARD),
      adminSections,
    )
  }

  @Test
  fun `role access guard prevents students from accessing admin screens and admins from student permissions`() {
    val activeStudent =
      UserProfile(
        id = "stu_1",
        uid = "stu_1",
        fullName = "Abebe Kebede",
        email = "abebe@wcu.edu.et",
        studentId = "WCU/1420/16",
        role = UserProfile.ROLE_STUDENT,
        facultyId = "fac_eng_tech",
        departmentId = "dep_se",
        isActive = true,
      )
    val activeAdmin =
      UserProfile(
        id = "adm_1",
        uid = "adm_1",
        fullName = "WCU QA Admin",
        email = "admin@wcu.edu.et",
        studentId = "",
        role = UserProfile.ROLE_ADMIN,
        facultyId = "WCU-ADMIN",
        departmentId = "QA-OFFICE",
        isActive = true,
      )

    val studentSession = AuthSessionState.AuthenticatedStudent(activeStudent, emailVerified = true)
    val adminSession = AuthSessionState.AuthenticatedAdmin(activeAdmin, emailVerified = true)

    // Student can access Student graph, NEVER Admin graph
    assertTrue(RoleAccessGuard.canAccessStudentGraph(studentSession))
    assertFalse(RoleAccessGuard.canAccessAdminGraph(studentSession))

    // Admin can access Admin graph, NEVER Student graph
    assertTrue(RoleAccessGuard.canAccessAdminGraph(adminSession))
    assertFalse(RoleAccessGuard.canAccessStudentGraph(adminSession))

    // Unauthenticated or Loading states cannot access either authenticated graph
    assertFalse(RoleAccessGuard.canAccessStudentGraph(AuthSessionState.Unauthenticated))
    assertFalse(RoleAccessGuard.canAccessAdminGraph(AuthSessionState.Unauthenticated))
    assertFalse(RoleAccessGuard.canAccessStudentGraph(AuthSessionState.Loading))
    assertFalse(RoleAccessGuard.canAccessAdminGraph(AuthSessionState.Loading))
  }
}
