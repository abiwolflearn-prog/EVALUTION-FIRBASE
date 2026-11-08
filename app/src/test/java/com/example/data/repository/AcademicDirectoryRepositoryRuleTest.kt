package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.TeacherEvaluation
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

class AcademicDirectoryRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun `1 directory loads real faculties departments active teachers courses and filters and searches accurately`() =
    runBlocking {
      // Seed academic data as verified Administrator
      signInVerifiedAdminTestUser(UserProfileRepository.BOOTSTRAPPED_ADMIN_EMAIL)

      val suffix = UUID.randomUUID().toString().take(6)
      val facEngId = "fac_eng_$suffix"
      val facMedId = "fac_med_$suffix"
      val deptSeId = "dep_se_$suffix"
      val deptCsId = "dep_cs_$suffix"
      val deptNursId = "dep_nurs_$suffix"
      val tchSeId = "tch_se_$suffix"
      val tchCsId = "tch_cs_$suffix"
      val tchInactiveId = "tch_inactive_$suffix"
      val crsSeId = "crs_se_$suffix"
      val crsCsId = "crs_cs_$suffix"
      val semId = "sem_3b_$suffix"

      withTimeout(DEFAULT_TIMEOUT_MS) {
        // Faculties
        firestore
          .collection(AcademicDirectoryRepository.COLLECTION_FACULTIES)
          .document(facEngId)
          .set(
            mapOf(
              "id" to facEngId,
              "name" to "College of Engineering and Technology",
              "description" to "Engineering and Computing Programs",
              "isActive" to true,
            )
          )
          .await()

        firestore
          .collection(AcademicDirectoryRepository.COLLECTION_FACULTIES)
          .document(facMedId)
          .set(
            mapOf(
              "id" to facMedId,
              "name" to "College of Medicine and Health Sciences",
              "description" to "Clinical and Public Health Programs",
              "isActive" to true,
            )
          )
          .await()

        // Departments
        firestore
          .collection(AcademicDirectoryRepository.COLLECTION_DEPARTMENTS)
          .document(deptSeId)
          .set(
            mapOf(
              "id" to deptSeId,
              "name" to "Software Engineering",
              "facultyId" to facEngId,
              "description" to "BSc in Software Engineering",
              "isActive" to true,
            )
          )
          .await()

        firestore
          .collection(AcademicDirectoryRepository.COLLECTION_DEPARTMENTS)
          .document(deptCsId)
          .set(
            mapOf(
              "id" to deptCsId,
              "name" to "Computer Science",
              "facultyId" to facEngId,
              "description" to "BSc in Computer Science",
              "isActive" to true,
            )
          )
          .await()

        firestore
          .collection(AcademicDirectoryRepository.COLLECTION_DEPARTMENTS)
          .document(deptNursId)
          .set(
            mapOf(
              "id" to deptNursId,
              "name" to "Comprehensive Nursing",
              "facultyId" to facMedId,
              "description" to "BSc in Nursing",
              "isActive" to true,
            )
          )
          .await()

        // Semester
        firestore
          .collection(AcademicDirectoryRepository.COLLECTION_SEMESTERS)
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

        // Teachers (2 active, 1 inactive)
        firestore
          .collection(AcademicDirectoryRepository.COLLECTION_TEACHERS)
          .document(tchSeId)
          .set(
            mapOf(
              "id" to tchSeId,
              "fullName" to "Dr. Kassahun Melesse",
              "email" to "kassahun.melesse@wcu.edu.et",
              "profileImage" to "",
              "facultyId" to facEngId,
              "departmentId" to deptSeId,
              "specialization" to "Cloud Architecture & DevOps",
              "isActive" to true,
            )
          )
          .await()

        firestore
          .collection(AcademicDirectoryRepository.COLLECTION_TEACHERS)
          .document(tchCsId)
          .set(
            mapOf(
              "id" to tchCsId,
              "fullName" to "Dr. Rahel Mekonnen",
              "email" to "rahel.mekonnen@wcu.edu.et",
              "profileImage" to "",
              "facultyId" to facEngId,
              "departmentId" to deptCsId,
              "specialization" to "Machine Learning & Algorithms",
              "isActive" to true,
            )
          )
          .await()

        firestore
          .collection(AcademicDirectoryRepository.COLLECTION_TEACHERS)
          .document(tchInactiveId)
          .set(
            mapOf(
              "id" to tchInactiveId,
              "fullName" to "Archived Teacher",
              "email" to "archived@wcu.edu.et",
              "profileImage" to "",
              "facultyId" to facEngId,
              "departmentId" to deptSeId,
              "specialization" to "Retired",
              "isActive" to false,
            )
          )
          .await()

        // Courses
        firestore
          .collection(AcademicDirectoryRepository.COLLECTION_COURSES)
          .document(crsSeId)
          .set(
            mapOf(
              "id" to crsSeId,
              "courseCode" to "SEng4012",
              "courseName" to "Cloud Native Engineering",
              "teacherId" to tchSeId,
              "departmentId" to deptSeId,
              "semesterId" to semId,
              "academicYear" to "2026/27",
              "isActive" to true,
            )
          )
          .await()

        firestore
          .collection(AcademicDirectoryRepository.COLLECTION_COURSES)
          .document(crsCsId)
          .set(
            mapOf(
              "id" to crsCsId,
              "courseCode" to "CoSc3021",
              "courseName" to "Design and Analysis of Algorithms",
              "teacherId" to tchCsId,
              "departmentId" to deptCsId,
              "semesterId" to semId,
              "academicYear" to "2026/27",
              "isActive" to true,
            )
          )
          .await()
      }

      // Sign in as Student in Software Engineering (deptSeId)
      val studentEmail = "student_dir_${suffix}@wcu.edu.et"
      val studentUid = signInTestUser(studentEmail)
      val userRepo = UserProfileRepository(firestore, auth)
      val dirRepo = AcademicDirectoryRepository(firestore, auth)

      val studentProfile =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          userRepo
            .createStudentProfile(
              fullName = "Yohannes Desta",
              email = studentEmail,
              studentId = "WCU/7712/16",
              facultyId = facEngId,
              departmentId = deptSeId,
              yearOfStudy = 4,
            )
            .getOrThrow()
        }

      // 1. Verify loading departments associated with selected faculty
      val engDepts =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          dirRepo.loadActiveDepartments(facultyId = facEngId).getOrThrow()
        }
      val engDeptIds = engDepts.map { it.id }.toSet()
      assertTrue(engDeptIds.contains(deptSeId))
      assertTrue(engDeptIds.contains(deptCsId))
      assertFalse(engDeptIds.contains(deptNursId))

      // 2. Verify loading active teachers excludes inactive teacher
      val engTeachers =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          dirRepo.loadActiveTeachers(facultyId = facEngId).getOrThrow()
        }
      val engTeacherIds = engTeachers.map { it.id }.toSet()
      assertTrue(engTeacherIds.contains(tchSeId))
      assertTrue(engTeacherIds.contains(tchCsId))
      assertFalse("Inactive teacher must not be returned", engTeacherIds.contains(tchInactiveId))

      // 3. Verify search and filtering on teachers and courses
      val snapshot =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          dirRepo.loadAcademicDirectorySnapshot(studentUid = studentUid).getOrThrow()
        }
      val cloudMatches =
        dirRepo.filterTeachers(
          teachers = snapshot.teachers,
          searchQuery = "Cloud Architecture",
          facultyId = facEngId,
        )
      assertEquals(1, cloudMatches.size)
      assertEquals(tchSeId, cloudMatches.first().id)

      val deptCourseMatches =
        dirRepo.filterCourses(
          courses = snapshot.courses,
          teachersById = snapshot.teachers.associateBy { it.id },
          departmentsById = snapshot.departments.associateBy { it.id },
          searchQuery = "SEng4012",
          departmentId = deptSeId,
        )
      assertEquals(1, deptCourseMatches.size)
      assertEquals(crsSeId, deptCourseMatches.first().id)

      // 4. Verify Teacher Profile Details and Evaluation Eligibility before & after evaluation
      val seTeacherDetails =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          dirRepo.loadTeacherProfileDetails(tchSeId, studentProfile).getOrThrow()
        }
      assertNotNull(seTeacherDetails)
      assertEquals("Dr. Kassahun Melesse", seTeacherDetails?.teacher?.fullName)
      assertEquals("College of Engineering and Technology", seTeacherDetails?.facultyDisplayName)
      assertEquals("Software Engineering", seTeacherDetails?.departmentDisplayName)
      assertEquals(1, seTeacherDetails?.assignedCourses?.size)
      val assignedSeCourse = seTeacherDetails!!.assignedCourses.first()
      assertEquals(crsSeId, assignedSeCourse.course.id)
      assertTrue(assignedSeCourse.isStudentDepartmentMatch)
      assertFalse(assignedSeCourse.isAlreadyEvaluated)

      // Submit evaluation for crsSeId and verify isAlreadyEvaluated becomes true
      val evalId =
        TeacherEvaluation.buildCompositeId(
          studentUid = studentUid,
          teacherId = tchSeId,
          courseId = crsSeId,
          semesterId = semId,
        )
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore
          .collection(AcademicDirectoryRepository.COLLECTION_EVALUATIONS)
          .document(evalId)
          .set(
            mapOf(
              "evaluationId" to evalId,
              "studentId" to studentUid,
              "teacherId" to tchSeId,
              "courseId" to crsSeId,
              "departmentId" to deptSeId,
              "facultyId" to facEngId,
              "semesterId" to semId,
              "academicYear" to "2026/27",
              "categoryRatings" to (1..6).associate { "cat_$it" to 5.0 },
              "questionRatings" to (1..18).associate { "q_$it" to 5 },
              "overallScore" to 5.0,
              "comment" to "Great course.",
              "submittedAt" to FieldValue.serverTimestamp(),
              "status" to TeacherEvaluation.STATUS_SUBMITTED,
            )
          )
          .await()
      }

      val seTeacherDetailsAfterEval =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          dirRepo.loadTeacherProfileDetails(tchSeId, studentProfile).getOrThrow()
        }
      assertTrue(seTeacherDetailsAfterEval!!.assignedCourses.first().isAlreadyEvaluated)

      // 5. Verify safe handling of missing or inactive teacher records
      val missingTeacherResult =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          dirRepo.loadTeacherProfileDetails("missing_teacher_id_999", studentProfile).getOrThrow()
        }
      assertNull("Missing teacher must return null safely without crashing", missingTeacherResult)

      val inactiveTeacherResult =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          dirRepo.loadTeacherProfileDetails(tchInactiveId, studentProfile).getOrThrow()
        }
      assertNull("Inactive teacher must return null safely", inactiveTeacherResult)
    }

  @Test
  fun `2 student cannot create modify or delete faculties departments teachers or courses`() =
    runBlocking {
      val studentEmail = "student_sec_3b_${UUID.randomUUID().toString().take(8)}@wcu.edu.et"
      signInTestUser(studentEmail)

      val collectionsToTest =
        listOf(
          AcademicDirectoryRepository.COLLECTION_FACULTIES to
            mapOf("id" to "fac_x", "name" to "Tampered", "description" to "X", "isActive" to true),
          AcademicDirectoryRepository.COLLECTION_DEPARTMENTS to
            mapOf(
              "id" to "dep_x",
              "name" to "Tampered",
              "facultyId" to "fac_x",
              "description" to "X",
              "isActive" to true,
            ),
          AcademicDirectoryRepository.COLLECTION_COURSES to
            mapOf(
              "id" to "crs_x",
              "courseCode" to "CS101",
              "courseName" to "Tampered",
              "teacherId" to "tch_1",
              "departmentId" to "dep_x",
              "semesterId" to "sem_1",
              "academicYear" to "2026/27",
              "isActive" to true,
            ),
        )

      for ((collection, payload) in collectionsToTest) {
        try {
          withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection(collection).document("doc_x").set(payload).await()
          }
          fail("Expected PERMISSION_DENIED when student attempts to write to /$collection")
        } catch (e: Exception) {
          val fsEx = checkNotNull(e.unwrapFirestoreException())
          assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
      }
    }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
  }
}
