package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AdminAccountRecord
import com.example.data.model.TeacherEvaluation
import com.example.data.model.UserProfile
import com.example.ui.auth.AuthSessionState
import com.example.ui.auth.RoleAccessGuard
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Comprehensive Robolectric + Firebase Emulator integration test suite covering all 14 required
 * Super Admin, Department Admin, and Student authorization & functional scenarios.
 */
class AdminHierarchyRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun `1 to 12 super admin and department admin hierarchy enforces department isolation and blocks cross-department access`() =
    runBlocking {
      val suffix = UUID.randomUUID().toString().take(6)
      val facEngId = "fac_eng_$suffix"
      val deptSeId = "dep_se_$suffix"
      val deptCsId = "dep_cs_$suffix"
      val semId = "sem_adm_$suffix"
      val tchSeId = "tch_se_$suffix"
      val tchCsId = "tch_cs_$suffix"
      val crsSeId = "crs_se_$suffix"
      val crsCsId = "crs_cs_$suffix"

      // Sign in as verified Super Admin (bootstrapped admin email)
      val superAdminUid = signInVerifiedAdminTestUser(UserProfileRepository.BOOTSTRAPPED_ADMIN_EMAIL)
      val adminRepo = AdminManagementRepository(firestore, auth)
      val userRepo = UserProfileRepository(firestore, auth)

      // Scenario 1: Super Admin can access Super Admin dashboard & RoleAccessGuard verifies Level 1 access
      val resolvedSuperAdminRecord =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          userRepo.resolveAdminAccountRecord().getOrThrow()
        }
      assertNotNull(resolvedSuperAdminRecord)
      assertTrue(resolvedSuperAdminRecord!!.isSuperAdmin)
      assertFalse(resolvedSuperAdminRecord.isDepartmentAdmin)

      val superAdminProfile =
        UserProfile(
          id = superAdminUid,
          uid = superAdminUid,
          fullName = "WCU Super Admin",
          email = UserProfileRepository.BOOTSTRAPPED_ADMIN_EMAIL,
          role = UserProfile.ROLE_SUPER_ADMIN,
          facultyId = "WCU-SUPER-ADMIN",
          departmentId = "UNIVERSITY-WIDE",
          isActive = true,
        )
      val superAdminSession =
        AuthSessionState.AuthenticatedAdmin(
          profile = superAdminProfile,
          emailVerified = true,
          adminRecord = resolvedSuperAdminRecord,
        )
      assertTrue(RoleAccessGuard.canAccessSuperAdminDestination(superAdminSession))
      assertFalse(RoleAccessGuard.canAccessDepartmentAdminDestination(superAdminSession))
      assertFalse(RoleAccessGuard.canAccessStudentGraph(superAdminSession))

      // Super Admin creates Faculty and two Departments (Software Engineering & Computer Science)
      withTimeout(DEFAULT_TIMEOUT_MS) {
        adminRepo
          .upsertFaculty(
            facultyId = facEngId,
            name = "College of Engineering and Technology",
            description = "Engineering & Computing",
          )
          .getOrThrow()

        adminRepo
          .upsertDepartment(
            departmentId = deptSeId,
            name = "Software Engineering",
            facultyId = facEngId,
            description = "SE Department",
          )
          .getOrThrow()

        adminRepo
          .upsertDepartment(
            departmentId = deptCsId,
            name = "Computer Science",
            facultyId = facEngId,
            description = "CS Department",
          )
          .getOrThrow()

        firestore
          .collection(AdminManagementRepository.COLLECTION_SEMESTERS)
          .document(semId)
          .set(
            mapOf(
              "id" to semId,
              "name" to "Semester I",
              "academicYear" to "2026/27",
              "isActive" to true,
              "isEvaluationOpen" to true,
            )
          )
          .await()
      }

      // Create two students: one in Software Engineering (deptSeId), one in Computer Science (deptCsId)
      val seStudentEmail = "stu_se_$suffix@wcu.edu.et"
      val seStudentUid = signInTestUser(seStudentEmail)
      withTimeout(DEFAULT_TIMEOUT_MS) {
        userRepo
          .createStudentProfile(
            fullName = "Dawit Tadesse (SE)",
            email = seStudentEmail,
            studentId = "WCU/1001/16",
            facultyId = facEngId,
            departmentId = deptSeId,
            yearOfStudy = 3,
          )
          .getOrThrow()
      }

      val csStudentEmail = "stu_cs_$suffix@wcu.edu.et"
      val csStudentUid = signInTestUser(csStudentEmail)
      withTimeout(DEFAULT_TIMEOUT_MS) {
        userRepo
          .createStudentProfile(
            fullName = "Hanna Alemu (CS)",
            email = csStudentEmail,
            studentId = "WCU/2002/16",
            facultyId = facEngId,
            departmentId = deptCsId,
            yearOfStudy = 3,
          )
          .getOrThrow()
      }

      // Create user auth account for Department Admin (assigned to Software Engineering: deptSeId)
      val deptAdminEmail = "dept_admin_se_$suffix@wcu.edu.et"
      val deptAdminUid = signInTestUser(deptAdminEmail)

      // Switch back to Super Admin session to provision the Department Admin record in /admins/{deptAdminUid}
      signInVerifiedAdminTestUser(UserProfileRepository.BOOTSTRAPPED_ADMIN_EMAIL)

      // Scenario 2: Super Admin can create and assign a Department Admin without losing Super Admin session
      val createdDeptAdminRecord =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo
            .createDepartmentAdminAccount(
              targetUid = deptAdminUid,
              fullName = "Dr. Samuel Girma",
              email = deptAdminEmail,
              facultyId = facEngId,
              departmentId = deptSeId,
            )
            .getOrThrow()
        }
      assertEquals(deptAdminUid, createdDeptAdminRecord.uid)
      assertEquals(UserProfile.ROLE_DEPARTMENT_ADMIN, createdDeptAdminRecord.role)
      assertEquals(facEngId, createdDeptAdminRecord.facultyId)
      assertEquals(deptSeId, createdDeptAdminRecord.departmentId)
      assertTrue(createdDeptAdminRecord.isActive)
      assertEquals("Super Admin session must remain active", superAdminUid, auth.currentUser?.uid)

      // Super Admin seeds initial CS teacher & course so we can test cross-department protections
      withTimeout(DEFAULT_TIMEOUT_MS) {
        adminRepo
          .upsertTeacher(
            teacherId = tchCsId,
            fullName = "Dr. CS Instructor",
            email = "cs.inst@wcu.edu.et",
            facultyId = facEngId,
            departmentId = deptCsId,
            specialization = "AI",
            actorRole = UserProfile.ROLE_SUPER_ADMIN,
          )
          .getOrThrow()
        adminRepo
          .upsertCourse(
            courseId = crsCsId,
            courseCode = "COSC301",
            courseName = "Artificial Intelligence",
            teacherId = tchCsId,
            departmentId = deptCsId,
            semesterId = semId,
            academicYear = "2026/27",
            actorRole = UserProfile.ROLE_SUPER_ADMIN,
          )
          .getOrThrow()
      }

      // Scenario 3: Department Admin logs in, resolves trusted /admins/{uid} record, and is routed to Department Admin dashboard
      signInTestUser(deptAdminEmail)
      val resolvedDeptAdmin =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          userRepo.resolveAdminAccountRecord().getOrThrow()
        }
      assertNotNull(resolvedDeptAdmin)
      assertTrue(resolvedDeptAdmin!!.isDepartmentAdmin)
      assertFalse(resolvedDeptAdmin.isSuperAdmin)
      assertEquals(deptSeId, resolvedDeptAdmin.departmentId)

      val deptAdminProfile =
        UserProfile(
          id = deptAdminUid,
          uid = deptAdminUid,
          fullName = resolvedDeptAdmin.fullName,
          email = resolvedDeptAdmin.email,
          role = UserProfile.ROLE_DEPARTMENT_ADMIN,
          facultyId = facEngId,
          departmentId = deptSeId,
          isActive = true,
        )
      val deptAdminSession =
        AuthSessionState.AuthenticatedAdmin(
          profile = deptAdminProfile,
          emailVerified = true,
          adminRecord = resolvedDeptAdmin,
        )
      assertTrue(RoleAccessGuard.canAccessDepartmentAdminDestination(deptAdminSession))
      assertFalse(
        "Department Admin must never access Super Admin destinations",
        RoleAccessGuard.canAccessSuperAdminDestination(deptAdminSession),
      )
      assertFalse(RoleAccessGuard.canAccessStudentGraph(deptAdminSession))

      // Scenario 6 & 8: Department Admin can manage teachers and courses in their assigned department (deptSeId)
      withTimeout(DEFAULT_TIMEOUT_MS) {
        val createdTeacher =
          adminRepo
            .upsertTeacher(
              teacherId = tchSeId,
              fullName = "Dr. SE Instructor",
              email = "se.inst@wcu.edu.et",
              facultyId = facEngId,
              departmentId = deptSeId,
              specialization = "Software Architecture",
              actorRole = UserProfile.ROLE_DEPARTMENT_ADMIN,
              actorAssignedDepartmentId = deptSeId,
            )
            .getOrThrow()
        assertEquals(tchSeId, createdTeacher.id)

        val createdCourse =
          adminRepo
            .upsertCourse(
              courseId = crsSeId,
              courseCode = "SENG301",
              courseName = "Software Architecture",
              teacherId = tchSeId,
              departmentId = deptSeId,
              semesterId = semId,
              academicYear = "2026/27",
              actorRole = UserProfile.ROLE_DEPARTMENT_ADMIN,
              actorAssignedDepartmentId = deptSeId,
            )
            .getOrThrow()
        assertEquals(crsSeId, createdCourse.id)
      }

      // Scenario 7 & 9: Department Admin CANNOT manage teachers or courses in another department (deptCsId)
      val crossDeptTeacherResult =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo.upsertTeacher(
            teacherId = "tch_unauthorized_$suffix",
            fullName = "Unauthorized CS Teacher",
            email = "unauth.tch@wcu.edu.et",
            facultyId = facEngId,
            departmentId = deptCsId,
            specialization = "Forbidden",
            actorRole = UserProfile.ROLE_DEPARTMENT_ADMIN,
            actorAssignedDepartmentId = deptSeId,
          )
        }
      assertTrue(
        "Department Admin must be blocked from creating teacher in another department",
        crossDeptTeacherResult.isFailure,
      )

      val crossDeptCourseResult =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo.upsertCourse(
            courseId = "crs_unauthorized_$suffix",
            courseCode = "COSC999",
            courseName = "Unauthorized CS Course",
            teacherId = tchCsId,
            departmentId = deptCsId,
            semesterId = semId,
            academicYear = "2026/27",
            actorRole = UserProfile.ROLE_DEPARTMENT_ADMIN,
            actorAssignedDepartmentId = deptSeId,
          )
        }
      assertTrue(
        "Department Admin must be blocked from creating course in another department",
        crossDeptCourseResult.isFailure,
      )

      // Have SE student and CS student each submit an evaluation in their respective department
      signInTestUser(seStudentEmail)
      val seEvalId = TeacherEvaluation.buildCompositeId(seStudentUid, tchSeId, crsSeId, semId)
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore
          .collection(AdminManagementRepository.COLLECTION_EVALUATIONS)
          .document(seEvalId)
          .set(
            mapOf(
              "evaluationId" to seEvalId,
              "studentId" to seStudentUid,
              "teacherId" to tchSeId,
              "courseId" to crsSeId,
              "departmentId" to deptSeId,
              "facultyId" to facEngId,
              "semesterId" to semId,
              "academicYear" to "2026/27",
              "categoryRatings" to (1..6).associate { "cat_$it" to 5.0 },
              "questionRatings" to (1..18).associate { "q_$it" to 5 },
              "overallScore" to 5.0,
              "comment" to "Excellent SE course.",
              "submittedAt" to FieldValue.serverTimestamp(),
              "status" to TeacherEvaluation.STATUS_SUBMITTED,
            )
          )
          .await()
      }

      signInTestUser(csStudentEmail)
      val csEvalId = TeacherEvaluation.buildCompositeId(csStudentUid, tchCsId, crsCsId, semId)
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore
          .collection(AdminManagementRepository.COLLECTION_EVALUATIONS)
          .document(csEvalId)
          .set(
            mapOf(
              "evaluationId" to csEvalId,
              "studentId" to csStudentUid,
              "teacherId" to tchCsId,
              "courseId" to crsCsId,
              "departmentId" to deptCsId,
              "facultyId" to facEngId,
              "semesterId" to semId,
              "academicYear" to "2026/27",
              "categoryRatings" to (1..6).associate { "cat_$it" to 4.0 },
              "questionRatings" to (1..18).associate { "q_$it" to 4 },
              "overallScore" to 4.0,
              "comment" to "Good CS course.",
              "submittedAt" to FieldValue.serverTimestamp(),
              "status" to TeacherEvaluation.STATUS_SUBMITTED,
            )
          )
          .await()
      }

      // Switch back to SE Department Admin
      signInTestUser(deptAdminEmail)

      // Scenario 4 & 10: Department Admin can view students and evaluations in their assigned department (deptSeId)
      val deptDashboard =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo.loadDepartmentAdminDashboardSummary(resolvedDeptAdmin).getOrThrow()
        }
      assertEquals(1, deptDashboard.totalStudents)
      assertEquals(seStudentUid, deptDashboard.departmentStudents.first().uid)
      assertEquals(1, deptDashboard.totalSubmittedEvaluations)
      assertEquals(seEvalId, deptDashboard.departmentEvaluations.first().evaluationId)

      // Scenario 5 & 11: Department Admin CANNOT view students or evaluations from another department (deptCsId)
      try {
        withTimeout(DEFAULT_TIMEOUT_MS) {
          firestore
            .collection(AdminManagementRepository.COLLECTION_USERS)
            .document(csStudentUid)
            .get()
            .await()
        }
        fail("Expected PERMISSION_DENIED when Department Admin reads student from another department")
      } catch (e: Exception) {
        val fsEx = checkNotNull(e.unwrapFirestoreException())
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
      }

      try {
        withTimeout(DEFAULT_TIMEOUT_MS) {
          firestore
            .collection(AdminManagementRepository.COLLECTION_EVALUATIONS)
            .document(csEvalId)
            .get()
            .await()
        }
        fail("Expected PERMISSION_DENIED when Department Admin reads evaluation from another department")
      } catch (e: Exception) {
        val fsEx = checkNotNull(e.unwrapFirestoreException())
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
      }

      // Scenario 12: Department Admin CANNOT create or modify other administrators
      val illegalCreateAdmin =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo.createDepartmentAdminAccount(
            targetUid = "illegal_admin_$suffix",
            fullName = "Illegal Admin",
            email = "illegal@wcu.edu.et",
            facultyId = facEngId,
            departmentId = deptSeId,
          )
        }
      assertTrue(
        "Department Admin must be blocked from creating administrators",
        illegalCreateAdmin.isFailure,
      )
    }

  @Test
  fun `13 and 14 student cannot access admin screens and deactivated administrator loses all access immediately`() =
    runBlocking {
      val suffix = UUID.randomUUID().toString().take(6)
      val facId = "fac_deact_$suffix"
      val deptId = "dep_deact_$suffix"

      // Scenario 13: Student cannot access Super Admin or Department Admin screens
      val studentProfile =
        UserProfile(
          id = "stu_$suffix",
          uid = "stu_$suffix",
          fullName = "Meron Tsegaye",
          email = "meron@wcu.edu.et",
          studentId = "WCU/9090/16",
          role = UserProfile.ROLE_STUDENT,
          facultyId = facId,
          departmentId = deptId,
          isActive = true,
        )
      val studentSession = AuthSessionState.AuthenticatedStudent(studentProfile, emailVerified = true)
      assertTrue(RoleAccessGuard.canAccessStudentGraph(studentSession))
      assertFalse(RoleAccessGuard.canAccessAdminGraph(studentSession))
      assertFalse(RoleAccessGuard.canAccessSuperAdminDestination(studentSession))
      assertFalse(RoleAccessGuard.canAccessDepartmentAdminDestination(studentSession))

      // Scenario 14: Super Admin creates a Department Admin, then deactivates them; inactive admin cannot access routes or perform actions
      val deptAdminEmail = "deact_admin_$suffix@wcu.edu.et"
      val deptAdminUid = signInTestUser(deptAdminEmail)

      signInVerifiedAdminTestUser(UserProfileRepository.BOOTSTRAPPED_ADMIN_EMAIL)
      val adminRepo = AdminManagementRepository(firestore, auth)
      val userRepo = UserProfileRepository(firestore, auth)

      withTimeout(DEFAULT_TIMEOUT_MS) {
        adminRepo.upsertFaculty(facId, "Engineering", "Engineering").getOrThrow()
        adminRepo.upsertDepartment(deptId, "Software Eng", facId, "SE").getOrThrow()
        adminRepo
          .createDepartmentAdminAccount(
            targetUid = deptAdminUid,
            fullName = "Temporary Admin",
            email = deptAdminEmail,
            facultyId = facId,
            departmentId = deptId,
          )
          .getOrThrow()

        // Deactivate the Department Admin
        val deactivated =
          adminRepo
            .setAdministratorActiveStatus(
              targetAdminUid = deptAdminUid,
              isActive = false,
            )
            .getOrThrow()
        assertFalse(deactivated.isActive)
      }

      // Sign in as the deactivated Department Admin
      signInTestUser(deptAdminEmail)
      val resolvedRecord =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          userRepo.resolveAdminAccountRecord().getOrThrow()
        }
      assertNull(
        "Deactivated administrator must return null from resolveAdminAccountRecord()",
        resolvedRecord,
      )

      val inactiveSession =
        AuthSessionState.AuthenticatedAdmin(
          profile =
            UserProfile(
              id = deptAdminUid,
              uid = deptAdminUid,
              role = UserProfile.ROLE_DEPARTMENT_ADMIN,
              facultyId = facId,
              departmentId = deptId,
              isActive = false,
            ),
          emailVerified = true,
          adminRecord =
            AdminAccountRecord(
              uid = deptAdminUid,
              role = UserProfile.ROLE_DEPARTMENT_ADMIN,
              facultyId = facId,
              departmentId = deptId,
              isActive = false,
            ),
        )
      assertFalse(RoleAccessGuard.canAccessAdminGraph(inactiveSession))
      assertFalse(RoleAccessGuard.canAccessDepartmentAdminDestination(inactiveSession))
      assertFalse(RoleAccessGuard.canAccessSuperAdminDestination(inactiveSession))

      // Attempt to write a teacher as the deactivated Department Admin -> must fail with PERMISSION_DENIED
      val writeAttempt =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo.upsertTeacher(
            teacherId = "tch_blocked_$suffix",
            fullName = "Blocked Teacher",
            email = "blocked@wcu.edu.et",
            facultyId = facId,
            departmentId = deptId,
            specialization = "Blocked",
            actorRole = UserProfile.ROLE_DEPARTMENT_ADMIN,
            actorAssignedDepartmentId = deptId,
          )
        }
      assertTrue(
        "Inactive administrator must be denied Firestore writes by security rules",
        writeAttempt.isFailure,
      )
    }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
  }
}
