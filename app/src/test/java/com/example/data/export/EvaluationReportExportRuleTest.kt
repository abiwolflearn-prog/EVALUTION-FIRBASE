package com.example.data.export

import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AdminAccountRecord
import com.example.data.model.EvaluationReportExportFilter
import com.example.data.model.ExportFormat
import com.example.data.model.UserProfile
import com.example.data.model.WcuEvaluationRubricCategories
import com.example.data.repository.AdminManagementRepository
import com.example.data.repository.StudentDashboardRepository
import com.example.data.repository.UserProfileRepository
import com.example.ui.auth.AuthSessionState
import com.example.ui.auth.RoleAccessGuard
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive Robolectric + Firebase Emulator integration test suite covering:
 * - Student 6-Category, 18-Question evaluation submission & duplicate prevention
 * - Role-based export authorization (Student denied, Department Admin scoped, Super Admin university-wide/filtered, Inactive Admin denied)
 * - Native PDF (.pdf) & OpenXML Excel (.xlsx) 3-sheet workbook generation
 * - Empty dataset resilience
 * - Export audit log creation in `/audit_logs/{logId}`
 */
class EvaluationReportExportRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun `1 to 11 full evaluation submission and phase 5 pdf and excel export authorization and verification`() =
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<android.content.Context>()
      val exportDir = File(context.cacheDir, "test_exports_${UUID.randomUUID().toString().take(6)}").apply { mkdirs() }

      val suffix = UUID.randomUUID().toString().take(6)
      val facEngId = "fac_exp_$suffix"
      val deptSeId = "dep_se_exp_$suffix"
      val deptCsId = "dep_cs_exp_$suffix"
      val semId = "sem_exp_$suffix"
      val tchSeId = "tch_se_exp_$suffix"
      val tchCsId = "tch_cs_exp_$suffix"
      val crsSeId = "crs_se_exp_$suffix"
      val crsCsId = "crs_cs_exp_$suffix"

      // 1. Sign in as Super Admin and seed Faculty, 2 Departments, Semester, 2 Teachers, and 2 Courses
      val superAdminUid = signInVerifiedAdminTestUser(UserProfileRepository.BOOTSTRAPPED_ADMIN_EMAIL)
      val adminRepo = AdminManagementRepository(firestore, auth)
      val userRepo = UserProfileRepository(firestore, auth)
      val studentRepo = StudentDashboardRepository(firestore, auth)

      withTimeout(DEFAULT_TIMEOUT_MS) {
        adminRepo.upsertFaculty(facEngId, "College of Engineering", "Engineering & Tech").getOrThrow()
        adminRepo.upsertDepartment(deptSeId, "Software Engineering", facEngId, "SE Dept").getOrThrow()
        adminRepo.upsertDepartment(deptCsId, "Computer Science", facEngId, "CS Dept").getOrThrow()
        adminRepo.upsertSemester(semId, "Semester I", "2026/27", isActive = true, isEvaluationOpen = true).getOrThrow()

        adminRepo.upsertTeacher(
          teacherId = tchSeId,
          fullName = "Dr. Kassahun Melesse",
          email = "kassahun.$suffix@wcu.edu.et",
          facultyId = facEngId,
          departmentId = deptSeId,
          specialization = "Distributed Systems",
          actorRole = UserProfile.ROLE_SUPER_ADMIN,
        ).getOrThrow()

        adminRepo.upsertTeacher(
          teacherId = tchCsId,
          fullName = "Dr. Rahel Mekonnen",
          email = "rahel.$suffix@wcu.edu.et",
          facultyId = facEngId,
          departmentId = deptCsId,
          specialization = "Machine Learning",
          actorRole = UserProfile.ROLE_SUPER_ADMIN,
        ).getOrThrow()

        adminRepo.upsertCourse(
          courseId = crsSeId,
          courseCode = "SENG3011",
          courseName = "Software Architecture",
          teacherId = tchSeId,
          departmentId = deptSeId,
          semesterId = semId,
          academicYear = "2026/27",
          actorRole = UserProfile.ROLE_SUPER_ADMIN,
        ).getOrThrow()

        adminRepo.upsertCourse(
          courseId = crsCsId,
          courseCode = "COSC3021",
          courseName = "Analysis of Algorithms",
          teacherId = tchCsId,
          departmentId = deptCsId,
          semesterId = semId,
          academicYear = "2026/27",
          actorRole = UserProfile.ROLE_SUPER_ADMIN,
        ).getOrThrow()
      }

      // 2. Create SE Student & CS Student and submit 6-category, 18-question evaluations via StudentDashboardRepository
      val seStudentEmail = "stu_se_exp_$suffix@wcu.edu.et"
      val seStudentUid = signInTestUser(seStudentEmail)
      val seStudentProfile =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          userRepo.createStudentProfile(
            fullName = "Abebe Kebede (SE)",
            email = seStudentEmail,
            studentId = "WCU/1420/16",
            facultyId = facEngId,
            departmentId = deptSeId,
            yearOfStudy = 3,
          ).getOrThrow()
        }

      val seQuestionRatings = (1..18).associate { "q_$it" to 5 }
      val submittedSeEval =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          studentRepo.submitTeacherEvaluation(
            studentUid = seStudentUid,
            teacherId = tchSeId,
            courseId = crsSeId,
            questionRatings = seQuestionRatings,
            comment = "Outstanding Software Architecture course.",
          ).getOrThrow()
        }
      assertEquals(5.0, submittedSeEval.overallScore, 0.01)
      assertEquals(6, submittedSeEval.categoryRatings.size)
      assertEquals(18, submittedSeEval.questionRatings.size)

      // Verify duplicate submission by the same student for the same course/teacher/semester is prevented
      val duplicateSeEvalResult =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          studentRepo.submitTeacherEvaluation(
            studentUid = seStudentUid,
            teacherId = tchSeId,
            courseId = crsSeId,
            questionRatings = seQuestionRatings,
            comment = "Duplicate attempt",
          )
        }
      assertTrue("Duplicate evaluation submission must be rejected", duplicateSeEvalResult.isFailure)

      // Verify student export denial (RoleAccessGuard + Repository layer)
      val studentSession = AuthSessionState.AuthenticatedStudent(seStudentProfile, emailVerified = true)
      assertFalse(
        "Students must never pass RoleAccessGuard.canExportEvaluationReport",
        RoleAccessGuard.canExportEvaluationReport(studentSession),
      )
      val studentExportAttempt =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo.prepareAuthorizedExportDataset(
            filter = EvaluationReportExportFilter(),
          )
        }
      assertTrue("Student export dataset request must fail with SecurityException", studentExportAttempt.isFailure)
      assertTrue(studentExportAttempt.exceptionOrNull() is SecurityException)

      // Create CS student and submit CS evaluation
      val csStudentEmail = "stu_cs_exp_$suffix@wcu.edu.et"
      val csStudentUid = signInTestUser(csStudentEmail)
      withTimeout(DEFAULT_TIMEOUT_MS) {
        userRepo.createStudentProfile(
          fullName = "Hanna Alemu (CS)",
          email = csStudentEmail,
          studentId = "WCU/2104/16",
          facultyId = facEngId,
          departmentId = deptCsId,
          yearOfStudy = 4,
        ).getOrThrow()

        val csQuestionRatings = (1..18).associate { "q_$it" to 4 }
        studentRepo.submitTeacherEvaluation(
          studentUid = csStudentUid,
          teacherId = tchCsId,
          courseId = crsCsId,
          questionRatings = csQuestionRatings,
          comment = "Clear algorithm explanations.",
        ).getOrThrow()
      }

      // 3. Create SE Department Admin and provision via Super Admin
      val deptAdminEmail = "dept_admin_exp_$suffix@wcu.edu.et"
      val deptAdminUid = signInTestUser(deptAdminEmail)

      signInVerifiedAdminTestUser(UserProfileRepository.BOOTSTRAPPED_ADMIN_EMAIL)
      val createdDeptAdminRecord =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo.createDepartmentAdminAccount(
            targetUid = deptAdminUid,
            fullName = "Dr. Dawit Bekele",
            email = deptAdminEmail,
            facultyId = facEngId,
            departmentId = deptSeId,
          ).getOrThrow()
        }

      // 4. Sign in as SE Department Admin: verify own-department export succeeds & cross-department export is denied
      signInTestUser(deptAdminEmail)
      val deptAdminProfile =
        UserProfile(
          id = deptAdminUid,
          uid = deptAdminUid,
          fullName = createdDeptAdminRecord.fullName,
          email = createdDeptAdminRecord.email,
          role = UserProfile.ROLE_DEPARTMENT_ADMIN,
          facultyId = facEngId,
          departmentId = deptSeId,
          isActive = true,
        )
      val deptAdminSession =
        AuthSessionState.AuthenticatedAdmin(
          profile = deptAdminProfile,
          emailVerified = true,
          adminRecord = createdDeptAdminRecord,
        )
      assertTrue(RoleAccessGuard.canExportEvaluationReport(deptAdminSession))

      val deptFilter = EvaluationReportExportFilter(facultyId = facEngId, departmentId = deptSeId)
      val deptDataset =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo.prepareAuthorizedExportDataset(
            filter = deptFilter,
          ).getOrThrow()
        }
      assertEquals("DEPT_${deptSeId.uppercase()}", deptDataset.reportScope)
      assertEquals(1, deptDataset.totalEvaluationsCount)
      assertEquals(1, deptDataset.teacherCourseRows.size)
      assertEquals(tchSeId, deptDataset.teacherCourseRows.first().teacherId)
      assertEquals(5.0, deptDataset.overallAverageRating, 0.01)

      // Department Admin cross-department export attempt must be rejected with SecurityException
      val crossDeptExportAttempt =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo.prepareAuthorizedExportDataset(
            filter = EvaluationReportExportFilter(departmentId = deptCsId),
          )
        }
      assertTrue(
        "Department Admin must be blocked from exporting another department's evaluations",
        crossDeptExportAttempt.isFailure,
      )
      assertTrue(crossDeptExportAttempt.exceptionOrNull() is SecurityException)

      // Record Department Admin export audit log and verify in Firestore
      val deptExportAuditLog =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo.recordExportAuditLog(
            actorRole = UserProfile.ROLE_DEPARTMENT_ADMIN,
            assignedDepartmentId = deptSeId,
            exportFormat = ExportFormat.PDF,
            reportScope = deptDataset.reportScope,
            filter = deptFilter,
            exportedRecordCount = deptDataset.totalEvaluationsCount,
          ).getOrThrow()
        }
      assertEquals("PDF", deptExportAuditLog.exportFormat)
      assertEquals(1, deptExportAuditLog.exportedRecordCount)

      // 5. Super Admin deactivates Department Admin -> Inactive Department Admin export is denied
      signInVerifiedAdminTestUser(UserProfileRepository.BOOTSTRAPPED_ADMIN_EMAIL)
      withTimeout(DEFAULT_TIMEOUT_MS) {
        adminRepo.setAdministratorActiveStatus(deptAdminUid, isActive = false).getOrThrow()
      }

      signInTestUser(deptAdminEmail)
      val inactiveExportAttempt =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo.prepareAuthorizedExportDataset(
            filter = EvaluationReportExportFilter(departmentId = deptSeId),
          )
        }
      assertTrue(
        "Deactivated Department Admin must be denied export access immediately",
        inactiveExportAttempt.isFailure,
      )

      // 6. Super Admin university-wide and filtered export + PDF & Excel (.xlsx) generation verification
      signInVerifiedAdminTestUser(UserProfileRepository.BOOTSTRAPPED_ADMIN_EMAIL)

      // University-wide export dataset
      val uniFilter = EvaluationReportExportFilter(facultyId = facEngId)
      val uniDataset =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo.prepareAuthorizedExportDataset(
            filter = uniFilter,
          ).getOrThrow()
        }
      assertEquals(2, uniDataset.totalEvaluationsCount)
      assertEquals(2, uniDataset.teacherCourseRows.size)
      assertEquals(4.50, uniDataset.overallAverageRating, 0.01)

      // Super Admin filtered export dataset (filter by CS teacher only)
      val filteredCsDataset =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo.prepareAuthorizedExportDataset(
            filter = EvaluationReportExportFilter(departmentId = deptCsId, teacherId = tchCsId),
          ).getOrThrow()
        }
      assertEquals(1, filteredCsDataset.totalEvaluationsCount)
      assertEquals(tchCsId, filteredCsDataset.teacherCourseRows.first().teacherId)
      assertEquals(4.0, filteredCsDataset.overallAverageRating, 0.01)

      // 7. Verify PDF Report Generation (%PDF-1.4 header, non-zero byte size, Wachemo University branding & content)
      val pdfReport =
        EvaluationReportExporter.generatePdfReport(exportDir, uniDataset).getOrThrow()
      val pdfFile = File(pdfReport.filePath)
      assertTrue("Generated PDF file must exist", pdfFile.exists())
      assertTrue("Generated PDF file must be non-empty", pdfFile.length() > 500L)
      val pdfContent = pdfFile.readText(Charsets.ISO_8859_1)
      assertTrue("PDF must start with %PDF-1.4 header", pdfContent.startsWith("%PDF-1.4"))
      assertTrue("PDF must include WACHEMO UNIVERSITY branding", pdfContent.contains("WACHEMO UNIVERSITY"))
      assertTrue("PDF must include evaluated teacher name", pdfContent.contains("Dr. Kassahun Melesse"))
      assertTrue("PDF must include category breakdown", pdfContent.contains("Teaching Quality"))

      // 8. Verify Excel (.xlsx) Workbook Generation (valid OpenXML ZIP with 3 worksheets)
      val xlsxReport =
        EvaluationReportExporter.generateExcelReport(exportDir, uniDataset).getOrThrow()
      val xlsxFile = File(xlsxReport.filePath)
      assertTrue("Generated XLSX file must exist", xlsxFile.exists())
      assertTrue("Generated XLSX file must be non-empty", xlsxFile.length() > 500L)
      val workbookEntries = EvaluationReportExporter.inspectExcelWorkbook(xlsxFile)
      assertTrue(workbookEntries.containsKey("[Content_Types].xml"))
      assertTrue(workbookEntries.containsKey("xl/workbook.xml"))
      assertTrue(workbookEntries.containsKey("xl/worksheets/sheet1.xml"))
      assertTrue(workbookEntries.containsKey("xl/worksheets/sheet2.xml"))
      assertTrue(workbookEntries.containsKey("xl/worksheets/sheet3.xml"))

      val workbookXml = workbookEntries.getValue("xl/workbook.xml")
      assertTrue(workbookXml.contains("Summary &amp; Metadata"))
      assertTrue(workbookXml.contains("Teacher Evaluations"))
      assertTrue(workbookXml.contains("Category Rubric Scores"))

      val sheet2Xml = workbookEntries.getValue("xl/worksheets/sheet2.xml")
      assertTrue(sheet2Xml.contains("Dr. Kassahun Melesse"))
      assertTrue(sheet2Xml.contains("Dr. Rahel Mekonnen"))
      assertTrue(sheet2Xml.contains("SENG3011"))
      assertTrue(sheet2Xml.contains("COSC3021"))

      val sheet3Xml = workbookEntries.getValue("xl/worksheets/sheet3.xml")
      WcuEvaluationRubricCategories.ALL_CATEGORIES.forEach { (_, categoryTitle) ->
        val xmlSafeTitle = categoryTitle.replace("&", "&amp;")
        assertTrue(
          "Category Breakdown sheet must include '$categoryTitle'",
          sheet3Xml.contains(xmlSafeTitle),
        )
      }

      // 9. Verify Empty Dataset Handling in both PDF and Excel exports
      val emptyDataset =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo.prepareAuthorizedExportDataset(
            filter = EvaluationReportExportFilter(courseId = "crs_nonexistent_$suffix"),
          ).getOrThrow()
        }
      assertEquals(0, emptyDataset.totalEvaluationsCount)
      assertTrue(emptyDataset.teacherCourseRows.isEmpty())

      val emptyPdf = EvaluationReportExporter.generatePdfReport(exportDir, emptyDataset).getOrThrow()
      val emptyPdfText = File(emptyPdf.filePath).readText(Charsets.ISO_8859_1)
      assertTrue(
        "Empty PDF report must include clear empty-dataset message",
        emptyPdfText.contains("No evaluation records matched the selected filter criteria"),
      )

      val emptyXlsx = EvaluationReportExporter.generateExcelReport(exportDir, emptyDataset).getOrThrow()
      val emptyXlsxEntries = EvaluationReportExporter.inspectExcelWorkbook(File(emptyXlsx.filePath))
      assertTrue(
        "Empty Excel sheet2 must include clear empty-dataset message",
        emptyXlsxEntries.getValue("xl/worksheets/sheet2.xml")
          .contains("No matching evaluation records for the selected filters"),
      )

      // 10. Record Super Admin XLSX export audit log and verify in Firestore
      val superExportLog =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo.recordExportAuditLog(
            actorRole = UserProfile.ROLE_SUPER_ADMIN,
            assignedDepartmentId = "",
            exportFormat = ExportFormat.XLSX,
            reportScope = uniDataset.reportScope,
            filter = uniFilter,
            exportedRecordCount = uniDataset.totalEvaluationsCount,
          ).getOrThrow()
        }
      assertNotNull(superExportLog.id)
      assertEquals("XLSX", superExportLog.exportFormat)
      assertEquals(2, superExportLog.exportedRecordCount)
    }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
  }
}
