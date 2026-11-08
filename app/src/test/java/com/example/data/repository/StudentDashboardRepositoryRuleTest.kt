package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.TeacherEvaluation
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class StudentDashboardRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun `1 student dashboard loads profile faculty department teachers courses and calculates pending and completed metrics from Firestore`() =
    runBlocking {
      // 1. Sign in verified bootstrapped admin to populate Firestore academic collections
      val adminUid = signInVerifiedAdminTestUser(UserProfileRepository.BOOTSTRAPPED_ADMIN_EMAIL)
      assertNotNull(adminUid)

      val deptId = "dep_se_${UUID.randomUUID().toString().take(6)}"
      val facId = "fac_eng_${UUID.randomUUID().toString().take(6)}"
      val semId = "sem_2026_1"
      val teacher1Id = "tch_se_1_${UUID.randomUUID().toString().take(6)}"
      val teacher2Id = "tch_se_2_${UUID.randomUUID().toString().take(6)}"
      val inactiveTeacherId = "tch_se_inactive_${UUID.randomUUID().toString().take(6)}"
      val course1Id = "crs_se_1_${UUID.randomUUID().toString().take(6)}"
      val course2Id = "crs_se_2_${UUID.randomUUID().toString().take(6)}"

      // Create Faculty, Department, Semester, Teachers (2 active, 1 inactive), and 2 Courses as Admin
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore
          .collection(StudentDashboardRepository.COLLECTION_FACULTIES)
          .document(facId)
          .set(
            mapOf(
              "id" to facId,
              "name" to "College of Engineering and Technology",
              "description" to "WCU Engineering & Computing Faculty",
              "isActive" to true,
            )
          )
          .await()

        firestore
          .collection(StudentDashboardRepository.COLLECTION_DEPARTMENTS)
          .document(deptId)
          .set(
            mapOf(
              "id" to deptId,
              "name" to "Software Engineering",
              "facultyId" to facId,
              "description" to "Department of Software Engineering",
              "isActive" to true,
            )
          )
          .await()

        firestore
          .collection(StudentDashboardRepository.COLLECTION_SEMESTERS)
          .document(semId)
          .set(
            mapOf(
              "id" to semId,
              "name" to "Semester I",
              "academicYear" to "2026/27",
              "isActive" to true,
              "isEvaluationOpen" to true,
              "startDate" to "2026-10-01",
              "endDate" to "2027-01-31",
            )
          )
          .await()

        firestore
          .collection(StudentDashboardRepository.COLLECTION_TEACHERS)
          .document(teacher1Id)
          .set(
            mapOf(
              "id" to teacher1Id,
              "fullName" to "Dr. Tesfaye Worku",
              "email" to "tesfaye.worku@wcu.edu.et",
              "profileImage" to "",
              "facultyId" to facId,
              "departmentId" to deptId,
              "specialization" to "Software Architecture",
              "isActive" to true,
            )
          )
          .await()

        firestore
          .collection(StudentDashboardRepository.COLLECTION_TEACHERS)
          .document(teacher2Id)
          .set(
            mapOf(
              "id" to teacher2Id,
              "fullName" to "Asst. Prof. Selamawit Girma",
              "email" to "selamawit.girma@wcu.edu.et",
              "profileImage" to "",
              "facultyId" to facId,
              "departmentId" to deptId,
              "specialization" to "Distributed Systems",
              "isActive" to true,
            )
          )
          .await()

        firestore
          .collection(StudentDashboardRepository.COLLECTION_TEACHERS)
          .document(inactiveTeacherId)
          .set(
            mapOf(
              "id" to inactiveTeacherId,
              "fullName" to "Former Instructor",
              "email" to "former@wcu.edu.et",
              "profileImage" to "",
              "facultyId" to facId,
              "departmentId" to deptId,
              "specialization" to "Legacy Systems",
              "isActive" to false,
            )
          )
          .await()

        firestore
          .collection(StudentDashboardRepository.COLLECTION_COURSES)
          .document(course1Id)
          .set(
            mapOf(
              "id" to course1Id,
              "courseCode" to "SEng3011",
              "courseName" to "Software Architecture & Design",
              "teacherId" to teacher1Id,
              "departmentId" to deptId,
              "semesterId" to semId,
              "academicYear" to "2026/27",
              "isActive" to true,
            )
          )
          .await()

        firestore
          .collection(StudentDashboardRepository.COLLECTION_COURSES)
          .document(course2Id)
          .set(
            mapOf(
              "id" to course2Id,
              "courseCode" to "SEng3042",
              "courseName" to "Distributed Computing",
              "teacherId" to teacher2Id,
              "departmentId" to deptId,
              "semesterId" to semId,
              "academicYear" to "2026/27",
              "isActive" to true,
            )
          )
          .await()
      }

      // 2. Sign in as a Student in that department and create profile
      val studentEmail = "student_3a_${UUID.randomUUID().toString().take(8)}@wcu.edu.et"
      val studentUid = signInTestUser(studentEmail)
      val userRepo = UserProfileRepository(firestore, auth)
      val dashboardRepo = StudentDashboardRepository(firestore, auth)

      val createdProfile =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          userRepo
            .createStudentProfile(
              fullName = "Bereketeab Alemu",
              email = studentEmail,
              studentId = "WCU/4410/16",
              facultyId = facId,
              departmentId = deptId,
              yearOfStudy = 3,
            )
            .getOrThrow()
        }

      // 3. Load Student Dashboard before any evaluation is submitted
      val initialSummary =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          dashboardRepo.loadStudentDashboard(studentUid).getOrThrow()
        }

      assertEquals("Bereketeab Alemu", initialSummary.profile.fullName)
      assertEquals("WCU/4410/16", initialSummary.profile.studentId)
      assertEquals("College of Engineering and Technology", initialSummary.facultyDisplayName)
      assertEquals("Software Engineering", initialSummary.departmentDisplayName)
      // Only the 2 active teachers must be counted (inactive teacher excluded)
      assertEquals(2, initialSummary.totalAvailableTeachers)
      assertEquals(2, initialSummary.pendingEvaluationsCount)
      assertEquals(0, initialSummary.completedEvaluationsCount)
      assertEquals(0, initialSummary.evaluationProgressPercent)
      assertTrue(initialSummary.recentEvaluationActivity.isEmpty())

      // 4. Submit 1 valid evaluation for course1Id and verify dashboard updates to 50% progress
      val evalId =
        TeacherEvaluation.buildCompositeId(
          studentUid = studentUid,
          teacherId = teacher1Id,
          courseId = course1Id,
          semesterId = semId,
        )
      val questionMap = (1..18).associate { "q_$it" to 5 }
      val categoryMap = (1..6).associate { "cat_$it" to 5.0 }

      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore
          .collection(StudentDashboardRepository.COLLECTION_EVALUATIONS)
          .document(evalId)
          .set(
            mapOf(
              "evaluationId" to evalId,
              "studentId" to studentUid,
              "teacherId" to teacher1Id,
              "courseId" to course1Id,
              "departmentId" to deptId,
              "facultyId" to facId,
              "semesterId" to semId,
              "academicYear" to "2026/27",
              "categoryRatings" to categoryMap,
              "questionRatings" to questionMap,
              "overallScore" to 5.0,
              "comment" to "Excellent instruction and timely feedback.",
              "submittedAt" to FieldValue.serverTimestamp(),
              "status" to TeacherEvaluation.STATUS_SUBMITTED,
            )
          )
          .await()
      }

      val updatedSummary =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          dashboardRepo.loadStudentDashboard(studentUid).getOrThrow()
        }

      assertEquals(2, updatedSummary.totalAvailableTeachers)
      assertEquals(1, updatedSummary.pendingEvaluationsCount)
      assertEquals(1, updatedSummary.completedEvaluationsCount)
      assertEquals(50, updatedSummary.evaluationProgressPercent)
      assertEquals(1, updatedSummary.recentEvaluationActivity.size)
      assertEquals("Dr. Tesfaye Worku", updatedSummary.recentEvaluationActivity.first().teacherName)
      assertEquals("SEng3011", updatedSummary.recentEvaluationActivity.first().courseCode)

      // Also verify real-time Flow emission
      val streamedSummary =
        withTimeout(FLOW_TIMEOUT_MS) {
          dashboardRepo.observeStudentDashboard(createdProfile).first()
        }
      assertEquals(1, streamedSummary.completedEvaluationsCount)
      assertEquals(1, streamedSummary.pendingEvaluationsCount)
    }

  @Test
  fun `2 student cannot create or modify academic directory records in Firestore`() = runBlocking {
    val studentEmail = "student_nowrite_${UUID.randomUUID().toString().take(8)}@wcu.edu.et"
    signInTestUser(studentEmail)

    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore
          .collection(StudentDashboardRepository.COLLECTION_TEACHERS)
          .document("unauthorized_teacher")
          .set(
            mapOf(
              "id" to "unauthorized_teacher",
              "fullName" to "Unauthorized Teacher",
              "email" to "unauth@wcu.edu.et",
              "profileImage" to "",
              "facultyId" to "fac_eng_tech",
              "departmentId" to "dep_se",
              "specialization" to "Security",
              "isActive" to true,
            )
          )
          .await()
      }
      fail("Expected PERMISSION_DENIED when student writes to /teachers")
    } catch (e: Exception) {
      val fsEx = checkNotNull(e.unwrapFirestoreException())
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
    }
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
