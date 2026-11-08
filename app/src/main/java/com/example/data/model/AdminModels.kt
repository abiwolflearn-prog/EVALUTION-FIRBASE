package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Trusted administrator authorization record stored at `/admins/{uid}`.
 *
 * - For `super_admin`: `facultyId` and `departmentId` are null/empty.
 * - For `department_admin`: `facultyId` and `departmentId` specify the single assigned department.
 */
@IgnoreExtraProperties
data class AdminAccountRecord(
  val uid: String = "",
  val fullName: String = "",
  val email: String = "",
  val role: String = UserProfile.ROLE_DEPARTMENT_ADMIN,
  val facultyId: String? = null,
  val departmentId: String? = null,
  @field:JvmField val isActive: Boolean = true,
  val createdBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val isSuperAdmin: Boolean
    get() =
      isActive &&
        (role == UserProfile.ROLE_SUPER_ADMIN || role == UserProfile.ROLE_ADMIN || role.isBlank())

  val isDepartmentAdmin: Boolean
    get() =
      isActive &&
        role == UserProfile.ROLE_DEPARTMENT_ADMIN &&
        !facultyId.isNullOrBlank() &&
        !departmentId.isNullOrBlank()
}

/**
 * Immutable administrator activity audit log stored at `/audit_logs/{id}`.
 */
@IgnoreExtraProperties
data class AdminAuditLog(
  val id: String = "",
  val actorUid: String = "",
  val actorRole: String = "",
  val assignedDepartmentId: String = "",
  val action: String = "",
  val targetCollection: String = "",
  val targetDocumentId: String = "",
  val exportFormat: String = "",
  val reportScope: String = "",
  val selectedFilters: String = "",
  val exportedRecordCount: Int = 0,
  val details: String = "",
  @ServerTimestamp val timestamp: Timestamp? = null,
)

/**
 * Supported export file formats for Phase 5 Administrator Evaluation Reports.
 */
enum class ExportFormat(
  val code: String,
  val extension: String,
  val mimeType: String,
  val displayName: String,
) {
  PDF(
    code = "PDF",
    extension = "pdf",
    mimeType = "application/pdf",
    displayName = "PDF Report (.pdf)",
  ),
  XLSX(
    code = "XLSX",
    extension = "xlsx",
    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    displayName = "Excel Workbook (.xlsx)",
  ),
}

data class WcuEvaluationQuestion(
  val questionId: String,
  val categoryId: String,
  val questionNumber: Int,
  val prompt: String,
)

data class WcuEvaluationCategoryRubric(
  val categoryId: String,
  val title: String,
  val questions: List<WcuEvaluationQuestion>,
)

/**
 * Official 6 Wachemo University Teacher Evaluation Rubric Categories & 18 Questions.
 */
object WcuEvaluationRubricCategories {
  val ALL_CATEGORIES: List<Pair<String, String>> =
    listOf(
      "cat_1" to "Teaching Quality",
      "cat_2" to "Communication Skills",
      "cat_3" to "Punctuality & Preparation",
      "cat_4" to "Course Management",
      "cat_5" to "Student Support",
      "cat_6" to "Professionalism & Ethics",
    )

  val CATEGORIES_WITH_QUESTIONS: List<WcuEvaluationCategoryRubric> =
    listOf(
      WcuEvaluationCategoryRubric(
        categoryId = "cat_1",
        title = "Teaching Quality",
        questions =
          listOf(
            WcuEvaluationQuestion(
              questionId = "q_1",
              categoryId = "cat_1",
              questionNumber = 1,
              prompt = "Demonstrates deep subject-matter mastery and connects theory to real-world practice.",
            ),
            WcuEvaluationQuestion(
              questionId = "q_2",
              categoryId = "cat_1",
              questionNumber = 2,
              prompt = "Uses effective instructional methods, practical examples, and structured explanations.",
            ),
            WcuEvaluationQuestion(
              questionId = "q_3",
              categoryId = "cat_1",
              questionNumber = 3,
              prompt = "Encourages critical thinking, active student participation, and analytical problem-solving.",
            ),
          ),
      ),
      WcuEvaluationCategoryRubric(
        categoryId = "cat_2",
        title = "Communication Skills",
        questions =
          listOf(
            WcuEvaluationQuestion(
              questionId = "q_4",
              categoryId = "cat_2",
              questionNumber = 4,
              prompt = "Presents course concepts clearly, audibly, and at an appropriate pace for students.",
            ),
            WcuEvaluationQuestion(
              questionId = "q_5",
              categoryId = "cat_2",
              questionNumber = 5,
              prompt = "Answers student questions thoughtfully and clarifies complex topics effectively.",
            ),
            WcuEvaluationQuestion(
              questionId = "q_6",
              categoryId = "cat_2",
              questionNumber = 6,
              prompt = "Communicates course expectations, learning outcomes, and grading standards transparently.",
            ),
          ),
      ),
      WcuEvaluationCategoryRubric(
        categoryId = "cat_3",
        title = "Punctuality & Preparation",
        questions =
          listOf(
            WcuEvaluationQuestion(
              questionId = "q_7",
              categoryId = "cat_3",
              questionNumber = 7,
              prompt = "Arrives on time and starts/concludes class sessions according to the university timetable.",
            ),
            WcuEvaluationQuestion(
              questionId = "q_8",
              categoryId = "cat_3",
              questionNumber = 8,
              prompt = "Comes well-prepared with organized lecture notes, slides, and laboratory materials.",
            ),
            WcuEvaluationQuestion(
              questionId = "q_9",
              categoryId = "cat_3",
              questionNumber = 9,
              prompt = "Utilizes scheduled instructional hours productively and covers the syllabus systematically.",
            ),
          ),
      ),
      WcuEvaluationCategoryRubric(
        categoryId = "cat_4",
        title = "Course Management",
        questions =
          listOf(
            WcuEvaluationQuestion(
              questionId = "q_10",
              categoryId = "cat_4",
              questionNumber = 10,
              prompt = "Provides a comprehensive course outline aligned with Wachemo University curriculum standards.",
            ),
            WcuEvaluationQuestion(
              questionId = "q_11",
              categoryId = "cat_4",
              questionNumber = 11,
              prompt = "Designs fair, relevant assignments, projects, and exams that reflect taught material.",
            ),
            WcuEvaluationQuestion(
              questionId = "q_12",
              categoryId = "cat_4",
              questionNumber = 12,
              prompt = "Provides timely, constructive feedback and transparent grading on assessments.",
            ),
          ),
      ),
      WcuEvaluationCategoryRubric(
        categoryId = "cat_5",
        title = "Student Support",
        questions =
          listOf(
            WcuEvaluationQuestion(
              questionId = "q_13",
              categoryId = "cat_5",
              questionNumber = 13,
              prompt = "Maintains regular consultation hours and is approachable for academic advising.",
            ),
            WcuEvaluationQuestion(
              questionId = "q_14",
              categoryId = "cat_5",
              questionNumber = 14,
              prompt = "Supports students experiencing academic difficulties with patience and guidance.",
            ),
            WcuEvaluationQuestion(
              questionId = "q_15",
              categoryId = "cat_5",
              questionNumber = 15,
              prompt = "Provides helpful reference materials, textbooks, and supplementary learning resources.",
            ),
          ),
      ),
      WcuEvaluationCategoryRubric(
        categoryId = "cat_6",
        title = "Professionalism & Ethics",
        questions =
          listOf(
            WcuEvaluationQuestion(
              questionId = "q_16",
              categoryId = "cat_6",
              questionNumber = 16,
              prompt = "Treats all students fairly, respectfully, and impartially regardless of background.",
            ),
            WcuEvaluationQuestion(
              questionId = "q_17",
              categoryId = "cat_6",
              questionNumber = 17,
              prompt = "Upholds Wachemo University academic integrity, professional ethics, and classroom discipline.",
            ),
            WcuEvaluationQuestion(
              questionId = "q_18",
              categoryId = "cat_6",
              questionNumber = 18,
              prompt = "Serves as a positive professional role model and is receptive to constructive feedback.",
            ),
          ),
      ),
    )

  val ALL_QUESTIONS: List<WcuEvaluationQuestion> =
    CATEGORIES_WITH_QUESTIONS.flatMap { it.questions }

  fun labelFor(categoryId: String): String =
    ALL_CATEGORIES.firstOrNull { it.first == categoryId }?.second ?: categoryId

  /**
   * Validates all 18 question ratings (1..5) and returns `(categoryRatings, overallScore)`.
   */
  fun computeScoresFromQuestionRatings(
    questionRatings: Map<String, Int>,
  ): Pair<Map<String, Double>, Double> {
    require(questionRatings.size == 18) {
      "All 18 evaluation questions must be rated before submission (rated ${questionRatings.size}/18)."
    }
    for (q in ALL_QUESTIONS) {
      val score =
        questionRatings[q.questionId]
          ?: throw IllegalArgumentException("Missing rating for question ${q.questionNumber} (${q.questionId}).")
      require(score in 1..5) {
        "Rating for question ${q.questionNumber} must be between 1 and 5 stars (got $score)."
      }
    }

    val categoryRatings = linkedMapOf<String, Double>()
    for (category in CATEGORIES_WITH_QUESTIONS) {
      val avg =
        category.questions.map { questionRatings.getValue(it.questionId) }.average()
      val rounded = kotlin.math.round(avg * 100.0) / 100.0
      categoryRatings[category.categoryId] = rounded
    }

    val totalAvg = questionRatings.values.average()
    val overallScore = (kotlin.math.round(totalAvg * 100.0) / 100.0).coerceIn(1.0, 5.0)
    return categoryRatings to overallScore
  }
}

/**
 * Filter criteria for Administrator Evaluation Report Export.
 *
 * - For `department_admin`: `facultyId` and `departmentId` are locked to the administrator's
 *   trusted `/admins/{uid}` assignment. Attempting to specify another department throws `SecurityException`.
 * - For `super_admin`: all filters are optional (`null` or blank means university-wide / all).
 */
data class EvaluationReportExportFilter(
  val facultyId: String? = null,
  val departmentId: String? = null,
  val teacherId: String? = null,
  val courseId: String? = null,
  val semesterId: String? = null,
  val startDateMillis: Long? = null,
  val endDateMillis: Long? = null,
) {
  fun toAuditSummaryString(): String {
    val parts = mutableListOf<String>()
    facultyId?.takeIf { it.isNotBlank() }?.let { parts += "faculty=$it" }
    departmentId?.takeIf { it.isNotBlank() }?.let { parts += "department=$it" }
    teacherId?.takeIf { it.isNotBlank() }?.let { parts += "teacher=$it" }
    courseId?.takeIf { it.isNotBlank() }?.let { parts += "course=$it" }
    semesterId?.takeIf { it.isNotBlank() }?.let { parts += "semester=$it" }
    startDateMillis?.let { parts += "start=$it" }
    endDateMillis?.let { parts += "end=$it" }
    return if (parts.isEmpty()) "ALL (Unfiltered)" else parts.joinToString(", ")
  }
}

/**
 * Aggregated evaluation performance row per Teacher + Course + Semester for PDF & Excel exports.
 */
data class TeacherEvaluationReportRow(
  val teacherId: String,
  val teacherName: String,
  val teacherEmail: String,
  val specialization: String,
  val facultyId: String,
  val facultyName: String,
  val departmentId: String,
  val departmentName: String,
  val courseId: String,
  val courseCode: String,
  val courseName: String,
  val semesterId: String,
  val semesterLabel: String,
  val academicYear: String,
  val evaluationCount: Int,
  val averageRating: Double,
  val categoryScores: Map<String, Double>,
  val performanceClassification: String,
)

/**
 * Complete validated dataset prepared by the authorization/repository layer for PDF or Excel export.
 */
data class EvaluationExportDataset(
  val reportTitle: String,
  val universityName: String = "Wachemo University",
  val generatedAtIso: String,
  val generatedByUid: String,
  val generatedByName: String,
  val generatedByRole: String,
  val reportScope: String,
  val facultyLabel: String,
  val departmentLabel: String,
  val semesterLabel: String,
  val academicYearLabel: String,
  val appliedFilterSummary: String,
  val totalEvaluationsCount: Int,
  val totalTeachersEvaluated: Int,
  val totalCoursesEvaluated: Int,
  val overallAverageRating: Double,
  val categoryAverages: Map<String, Double>,
  val teacherCourseRows: List<TeacherEvaluationReportRow>,
  val departmentComparisonRows: List<DepartmentPerformanceRow> = emptyList(),
)

/**
 * Metadata and file handle for a generated PDF or Excel evaluation report.
 */
data class GeneratedReportFile(
  val filePath: String,
  val fileName: String,
  val format: ExportFormat,
  val byteSize: Long,
  val exportedRecordCount: Int,
  val reportScope: String,
  val generatedAtIso: String,
)

/**
 * Department performance comparison row for the Super Admin Dashboard.
 */
data class DepartmentPerformanceRow(
  val departmentId: String,
  val departmentName: String,
  val facultyId: String,
  val facultyName: String,
  val studentCount: Int,
  val activeTeacherCount: Int,
  val activeCourseCount: Int,
  val submittedEvaluationCount: Int,
  val averageScore: Double,
  val completionPercentage: Int,
)

/**
 * University-wide statistics and analytics loaded from Firestore for the Super Admin Dashboard.
 */
data class SuperAdminDashboardSummary(
  val adminProfile: UserProfile,
  val totalFaculties: Int,
  val totalDepartments: Int,
  val totalStudents: Int,
  val totalTeachers: Int,
  val totalActiveCourses: Int,
  val totalAdministrators: Int,
  val totalSubmittedEvaluations: Int,
  val totalPendingEvaluations: Int,
  val completionPercentage: Int,
  val overallUniversityRating: Double,
  val faculties: List<Faculty>,
  val departments: List<Department>,
  val teachers: List<Teacher>,
  val courses: List<Course>,
  val students: List<UserProfile>,
  val administrators: List<AdminAccountRecord>,
  val semesters: List<Semester>,
  val departmentComparisons: List<DepartmentPerformanceRow>,
  val recentAuditLogs: List<AdminAuditLog>,
)

/**
 * Department-scoped statistics and records loaded from Firestore for a Department Admin Dashboard.
 */
data class DepartmentAdminDashboardSummary(
  val adminRecord: AdminAccountRecord,
  val department: Department?,
  val faculty: Faculty?,
  val departmentDisplayName: String,
  val facultyDisplayName: String,
  val totalStudents: Int,
  val totalActiveTeachers: Int,
  val totalActiveCourses: Int,
  val totalSubmittedEvaluations: Int,
  val pendingEvaluationsCount: Int,
  val completionPercentage: Int,
  val departmentAverageRating: Double,
  val departmentStudents: List<UserProfile>,
  val departmentTeachers: List<Teacher>,
  val departmentCourses: List<Course>,
  val departmentEvaluations: List<TeacherEvaluation>,
  val recentAuditLogs: List<AdminAuditLog>,
)

