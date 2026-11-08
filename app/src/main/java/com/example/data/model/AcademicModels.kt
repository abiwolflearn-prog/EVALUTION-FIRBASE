package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Faculty(
  val id: String = "",
  val name: String = "",
  val description: String = "",
  @field:JvmField val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Department(
  val id: String = "",
  val name: String = "",
  val facultyId: String = "",
  val description: String = "",
  @field:JvmField val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Teacher(
  val id: String = "",
  val fullName: String = "",
  val email: String = "",
  val profileImage: String = "",
  val facultyId: String = "",
  val departmentId: String = "",
  val specialization: String = "",
  @field:JvmField val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Course(
  val id: String = "",
  val courseCode: String = "",
  val courseName: String = "",
  val teacherId: String = "",
  val departmentId: String = "",
  val semesterId: String = "",
  val academicYear: String = "",
  @field:JvmField val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Semester(
  val id: String = "",
  val name: String = "",
  val academicYear: String = "",
  @field:JvmField val isActive: Boolean = true,
  @field:JvmField val isEvaluationOpen: Boolean = true,
  val startDate: String = "",
  val endDate: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class TeacherEvaluation(
  @DocumentId val id: String = "",
  val evaluationId: String = "",
  val studentId: String = "",
  val teacherId: String = "",
  val courseId: String = "",
  val departmentId: String = "",
  val facultyId: String = "",
  val semesterId: String = "",
  val academicYear: String = "",
  val categoryRatings: Map<String, Double> = emptyMap(),
  val questionRatings: Map<String, Int> = emptyMap(),
  val overallScore: Double = 0.0,
  val comment: String = "",
  @ServerTimestamp val submittedAt: Timestamp? = null,
  val status: String = STATUS_SUBMITTED,
) {
  companion object {
    const val STATUS_SUBMITTED = "submitted"

    fun buildCompositeId(
      studentUid: String,
      teacherId: String,
      courseId: String,
      semesterId: String,
    ): String = "${studentUid}_${teacherId}_${courseId}_${semesterId}"
  }
}

data class PendingCourseEvaluationItem(
  val course: Course,
  val teacher: Teacher,
  val semesterName: String,
)

data class RecentEvaluationActivityItem(
  val evaluationId: String,
  val teacherId: String,
  val teacherName: String,
  val courseId: String,
  val courseCode: String,
  val courseName: String,
  val semesterId: String,
  val academicYear: String,
  val overallScore: Double,
  val comment: String,
  val submittedAtMillis: Long?,
  val status: String,
)

data class StudentDashboardSummary(
  val profile: UserProfile,
  val faculty: Faculty?,
  val department: Department?,
  val facultyDisplayName: String,
  val departmentDisplayName: String,
  val activeSemester: Semester?,
  val totalAvailableTeachers: Int,
  val availableTeachers: List<Teacher>,
  val totalEligibleCourses: Int,
  val pendingEvaluationsCount: Int,
  val completedEvaluationsCount: Int,
  val evaluationProgressPercent: Int,
  val evaluationProgressFraction: Float,
  val pendingEvaluations: List<PendingCourseEvaluationItem>,
  val recentEvaluationActivity: List<RecentEvaluationActivityItem>,
)

object WcuAcademicCatalogFallback {
  private val facultyNames =
    mapOf(
      "fac_eng_tech" to "College of Engineering and Technology (CET)",
      "fac_nat_comp" to "College of Natural and Computational Sciences (CNCS)",
      "fac_med_health" to "College of Medicine and Health Sciences (CMHS)",
      "fac_bus_econ" to "College of Business and Economics (CBE)",
      "fac_soc_hum" to "College of Social Sciences and Humanities (CSSH)",
      "fac_agri" to "College of Agricultural Sciences (CANR)",
    )

  private val departmentNames =
    mapOf(
      "dep_se" to "Software Engineering (SEng)",
      "dep_cs" to "Computer Science (CS)",
      "dep_it" to "Information Technology (IT)",
      "dep_ece" to "Electrical and Computer Engineering (ECE)",
      "dep_civil" to "Civil Engineering (CEng)",
      "dep_math" to "Mathematics (MATH)",
      "dep_stat" to "Statistics (STAT)",
      "dep_nurs" to "Comprehensive Nursing (NURS)",
      "dep_ph" to "Public Health Officer (PH)",
      "dep_acct" to "Accounting and Finance (ACCT)",
      "dep_mgmt" to "Management (MGMT)",
      "dep_law" to "School of Law (LAW)",
      "dep_plant" to "Plant Science (PLSC)",
    )

  fun resolveFacultyName(faculty: Faculty?, facultyId: String): String {
    if (faculty != null && faculty.name.isNotBlank()) return faculty.name
    return facultyNames[facultyId] ?: facultyId.ifBlank { "Unassigned Faculty" }
  }

  fun resolveDepartmentName(department: Department?, departmentId: String): String {
    if (department != null && department.name.isNotBlank()) return department.name
    return departmentNames[departmentId] ?: departmentId.ifBlank { "Unassigned Department" }
  }
}
