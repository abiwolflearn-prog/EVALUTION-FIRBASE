package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AdminAccountRecord
import com.example.data.model.AdminAuditLog
import com.example.data.model.Course
import com.example.data.model.Department
import com.example.data.model.DepartmentAdminDashboardSummary
import com.example.data.model.DepartmentPerformanceRow
import com.example.data.model.EvaluationExportDataset
import com.example.data.model.EvaluationReportExportFilter
import com.example.data.model.ExportFormat
import com.example.data.model.Faculty
import com.example.data.model.Semester
import com.example.data.model.SuperAdminDashboardSummary
import com.example.data.model.Teacher
import com.example.data.model.TeacherEvaluation
import com.example.data.model.TeacherEvaluationReportRow
import com.example.data.model.UserProfile
import com.example.data.model.WcuAcademicCatalogFallback
import com.example.data.model.WcuEvaluationRubricCategories
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class AdminManagementRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw SecurityException("Administrator must be signed in before executing admin operations.")
  }

  /**
   * Appends an immutable administrator activity record to `/audit_logs/{logId}` using `FieldValue.serverTimestamp()`.
   */
  suspend fun recordAuditLog(
    actorRole: String,
    assignedDepartmentId: String,
    action: String,
    targetCollection: String,
    targetDocumentId: String,
    details: String,
  ): Result<AdminAuditLog> = runCatching {
    val actorUid = requireUserId()
    val logId = "log_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    val docRef = db.collection(COLLECTION_AUDIT_LOGS).document(logId)

    val payload =
      mapOf(
        "id" to logId,
        "actorUid" to actorUid,
        "actorRole" to actorRole,
        "assignedDepartmentId" to assignedDepartmentId,
        "action" to action,
        "targetCollection" to targetCollection,
        "targetDocumentId" to targetDocumentId,
        "details" to details.take(550),
        "timestamp" to FieldValue.serverTimestamp(),
      )

    try {
      docRef.set(payload).await()
      val snap = docRef.get().await()
      snap.toObject(
        AdminAuditLog::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
      ) ?: AdminAuditLog(
        id = logId,
        actorUid = actorUid,
        actorRole = actorRole,
        assignedDepartmentId = assignedDepartmentId,
        action = action,
        targetCollection = targetCollection,
        targetDocumentId = targetDocumentId,
        details = details,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  /**
   * Records a structured evaluation report export audit event in `/audit_logs/{logId}`.
   * Includes:
   * - Administrator UID (`actorUid`)
   * - Administrator role (`actorRole`)
   * - Export format (`exportFormat`: `"PDF"` or `"XLSX"`)
   * - Report scope (`reportScope`)
   * - Selected filters (`selectedFilters`)
   * - Export timestamp (`timestamp`)
   * - Number of records exported (`exportedRecordCount`)
   */
  suspend fun recordExportAuditLog(
    actorRole: String,
    assignedDepartmentId: String,
    exportFormat: ExportFormat,
    reportScope: String,
    filter: EvaluationReportExportFilter,
    exportedRecordCount: Int,
  ): Result<AdminAuditLog> = runCatching {
    val actorUid = requireUserId()
    val logId = "log_export_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    val docRef = db.collection(COLLECTION_AUDIT_LOGS).document(logId)
    val filterSummary = filter.toAuditSummaryString().take(480)
    val safeScope =
      reportScope.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim().ifBlank { "UNIVERSITY_WIDE" }.take(120)
    val safeTargetId =
      safeScope.replace(" ", "_").ifBlank { "export_report" }.take(100)
    val details =
      "Exported ${exportFormat.code} evaluation report ($safeScope) • $exportedRecordCount records • Filters: $filterSummary"
        .take(550)

    val payload =
      mapOf(
        "id" to logId,
        "actorUid" to actorUid,
        "actorRole" to actorRole,
        "assignedDepartmentId" to assignedDepartmentId,
        "action" to ACTION_EXPORT_EVALUATION_REPORT,
        "targetCollection" to COLLECTION_EVALUATIONS,
        "targetDocumentId" to safeTargetId,
        "exportFormat" to exportFormat.code,
        "reportScope" to safeScope,
        "selectedFilters" to filterSummary,
        "exportedRecordCount" to exportedRecordCount.coerceAtLeast(0),
        "details" to details,
        "timestamp" to FieldValue.serverTimestamp(),
      )

    try {
      docRef.set(payload).await()
      val snap = docRef.get().await()
      snap.toObject(
        AdminAuditLog::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
      ) ?: AdminAuditLog(
        id = logId,
        actorUid = actorUid,
        actorRole = actorRole,
        assignedDepartmentId = assignedDepartmentId,
        action = ACTION_EXPORT_EVALUATION_REPORT,
        targetCollection = COLLECTION_EVALUATIONS,
        targetDocumentId = safeTargetId,
        exportFormat = exportFormat.code,
        reportScope = safeScope,
        selectedFilters = filterSummary,
        exportedRecordCount = exportedRecordCount.coerceAtLeast(0),
        details = details,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  /**
   * Provisions a Department Admin authorization record at `/admins/{targetUid}`.
   *
   * SECURITY INVARIANT:
   * - Never signs in as the new administrator on the client (never replaces the active Super Admin session).
   * - Enforced by `firestore.rules` so ONLY an active `super_admin` can create `/admins/{targetUid}`,
   *   with server-side validation that `departmentId` exists and belongs to `facultyId`.
   */
  suspend fun createDepartmentAdminAccount(
    targetUid: String,
    fullName: String,
    email: String,
    facultyId: String,
    departmentId: String,
  ): Result<AdminAccountRecord> = runCatching {
    val callerUid = requireUserId()
    val cleanTargetUid =
      targetUid.trim().ifBlank {
        "dept_admin_${email.substringBefore("@").replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}_${UUID.randomUUID().toString().take(6)}"
      }

    require(cleanTargetUid != callerUid) {
      "Super Admin cannot overwrite their own authorization record."
    }
    require(fullName.trim().length >= 2) { "Administrator full name is required." }
    require(email.trim().contains("@")) { "A valid administrator email address is required." }
    require(facultyId.isNotBlank()) { "Faculty assignment is required." }
    require(departmentId.isNotBlank()) { "Department assignment is required." }

    val docRef = db.collection(COLLECTION_ADMINS).document(cleanTargetUid)
    val payload =
      mapOf(
        "uid" to cleanTargetUid,
        "fullName" to fullName.trim(),
        "email" to email.trim(),
        "role" to UserProfile.ROLE_DEPARTMENT_ADMIN,
        "facultyId" to facultyId.trim(),
        "departmentId" to departmentId.trim(),
        "isActive" to true,
        "createdBy" to callerUid,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )

    try {
      docRef.set(payload).await()
      recordAuditLog(
        actorRole = UserProfile.ROLE_SUPER_ADMIN,
        assignedDepartmentId = "",
        action = "CREATE_DEPARTMENT_ADMIN",
        targetCollection = COLLECTION_ADMINS,
        targetDocumentId = cleanTargetUid,
        details = "Created Department Admin ${email.trim()} assigned to $departmentId ($facultyId)",
      )
      val snap = docRef.get().await()
      snap.toObject(
        AdminAccountRecord::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
      ) ?: AdminAccountRecord(
        uid = cleanTargetUid,
        fullName = fullName.trim(),
        email = email.trim(),
        role = UserProfile.ROLE_DEPARTMENT_ADMIN,
        facultyId = facultyId.trim(),
        departmentId = departmentId.trim(),
        isActive = true,
        createdBy = callerUid,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  /**
   * Activates or deactivates an administrator account in `/admins/{targetAdminUid}`.
   * Only callable by a verified Super Admin.
   */
  suspend fun setAdministratorActiveStatus(
    targetAdminUid: String,
    isActive: Boolean,
  ): Result<AdminAccountRecord> = runCatching {
    requireUserId()
    val docRef = db.collection(COLLECTION_ADMINS).document(targetAdminUid.trim())
    try {
      val existingSnap = docRef.get().await()
      val existing =
        existingSnap.toObject(
          AdminAccountRecord::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ) ?: throw IllegalStateException("Administrator record $targetAdminUid not found.")

      val updates =
        mapOf(
          "uid" to existing.uid,
          "fullName" to existing.fullName,
          "email" to existing.email,
          "role" to existing.role,
          "facultyId" to existing.facultyId,
          "departmentId" to existing.departmentId,
          "isActive" to isActive,
          "createdBy" to existing.createdBy,
          "createdAt" to (existing.createdAt ?: FieldValue.serverTimestamp()),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(updates).await()

      recordAuditLog(
        actorRole = UserProfile.ROLE_SUPER_ADMIN,
        assignedDepartmentId = "",
        action = if (isActive) "ACTIVATE_ADMIN" else "DEACTIVATE_ADMIN",
        targetCollection = COLLECTION_ADMINS,
        targetDocumentId = targetAdminUid.trim(),
        details = "${if (isActive) "Activated" else "Deactivated"} administrator ${existing.email}",
      )

      val refreshed = docRef.get().await()
      refreshed.toObject(
        AdminAccountRecord::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
      ) ?: existing.copy(isActive = isActive)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  /**
   * Updates the assigned faculty and department of a Department Admin in `/admins/{targetAdminUid}`.
   * Only callable by a verified Super Admin.
   */
  suspend fun updateDepartmentAdminAssignment(
    targetAdminUid: String,
    newFacultyId: String,
    newDepartmentId: String,
  ): Result<AdminAccountRecord> = runCatching {
    requireUserId()
    val docRef = db.collection(COLLECTION_ADMINS).document(targetAdminUid.trim())
    try {
      val existingSnap = docRef.get().await()
      val existing =
        existingSnap.toObject(
          AdminAccountRecord::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ) ?: throw IllegalStateException("Administrator record $targetAdminUid not found.")

      val updates =
        mapOf(
          "uid" to existing.uid,
          "fullName" to existing.fullName,
          "email" to existing.email,
          "role" to UserProfile.ROLE_DEPARTMENT_ADMIN,
          "facultyId" to newFacultyId.trim(),
          "departmentId" to newDepartmentId.trim(),
          "isActive" to existing.isActive,
          "createdBy" to existing.createdBy,
          "createdAt" to (existing.createdAt ?: FieldValue.serverTimestamp()),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(updates).await()

      recordAuditLog(
        actorRole = UserProfile.ROLE_SUPER_ADMIN,
        assignedDepartmentId = "",
        action = "UPDATE_ADMIN_ASSIGNMENT",
        targetCollection = COLLECTION_ADMINS,
        targetDocumentId = targetAdminUid.trim(),
        details = "Reassigned ${existing.email} to department $newDepartmentId ($newFacultyId)",
      )

      val refreshed = docRef.get().await()
      refreshed.toObject(
        AdminAccountRecord::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
      ) ?: existing.copy(facultyId = newFacultyId.trim(), departmentId = newDepartmentId.trim())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  /**
   * Creates or updates a Faculty document in `/faculties/{facultyId}` (Super Admin only).
   */
  suspend fun upsertFaculty(
    facultyId: String,
    name: String,
    description: String,
    isActive: Boolean = true,
  ): Result<Faculty> = runCatching {
    requireUserId()
    val cleanId = facultyId.trim()
    val docRef = db.collection(COLLECTION_FACULTIES).document(cleanId)
    val payload =
      mapOf(
        "id" to cleanId,
        "name" to name.trim(),
        "description" to description.trim(),
        "isActive" to isActive,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      recordAuditLog(
        actorRole = UserProfile.ROLE_SUPER_ADMIN,
        assignedDepartmentId = "",
        action = "UPSERT_FACULTY",
        targetCollection = COLLECTION_FACULTIES,
        targetDocumentId = cleanId,
        details = "Saved faculty ${name.trim()} ($cleanId)",
      )
      Faculty(
        id = cleanId,
        name = name.trim(),
        description = description.trim(),
        isActive = isActive,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  /**
   * Creates or updates a Department document in `/departments/{departmentId}` (Super Admin only).
   */
  suspend fun upsertDepartment(
    departmentId: String,
    name: String,
    facultyId: String,
    description: String,
    isActive: Boolean = true,
  ): Result<Department> = runCatching {
    requireUserId()
    val cleanId = departmentId.trim()
    val docRef = db.collection(COLLECTION_DEPARTMENTS).document(cleanId)
    val payload =
      mapOf(
        "id" to cleanId,
        "name" to name.trim(),
        "facultyId" to facultyId.trim(),
        "description" to description.trim(),
        "isActive" to isActive,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      recordAuditLog(
        actorRole = UserProfile.ROLE_SUPER_ADMIN,
        assignedDepartmentId = "",
        action = "UPSERT_DEPARTMENT",
        targetCollection = COLLECTION_DEPARTMENTS,
        targetDocumentId = cleanId,
        details = "Saved department ${name.trim()} ($cleanId) under $facultyId",
      )
      Department(
        id = cleanId,
        name = name.trim(),
        facultyId = facultyId.trim(),
        description = description.trim(),
        isActive = isActive,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  /**
   * Creates or updates a Teacher document in `/teachers/{teacherId}`.
   * Permitted for Super Admin (any department) or Department Admin (ONLY their assigned `facultyId` & `departmentId`).
   */
  suspend fun upsertTeacher(
    teacherId: String,
    fullName: String,
    email: String,
    facultyId: String,
    departmentId: String,
    specialization: String,
    isActive: Boolean = true,
    actorRole: String,
    actorAssignedDepartmentId: String = "",
  ): Result<Teacher> = runCatching {
    requireUserId()
    val cleanId = teacherId.trim()
    val docRef = db.collection(COLLECTION_TEACHERS).document(cleanId)
    val payload =
      mapOf(
        "id" to cleanId,
        "fullName" to fullName.trim(),
        "email" to email.trim(),
        "profileImage" to "",
        "facultyId" to facultyId.trim(),
        "departmentId" to departmentId.trim(),
        "specialization" to specialization.trim(),
        "isActive" to isActive,
      )
    try {
      docRef.set(payload).await()
      recordAuditLog(
        actorRole = actorRole,
        assignedDepartmentId = actorAssignedDepartmentId,
        action = "UPSERT_TEACHER",
        targetCollection = COLLECTION_TEACHERS,
        targetDocumentId = cleanId,
        details = "Saved teacher ${fullName.trim()} ($cleanId) in $departmentId",
      )
      Teacher(
        id = cleanId,
        fullName = fullName.trim(),
        email = email.trim(),
        profileImage = "",
        facultyId = facultyId.trim(),
        departmentId = departmentId.trim(),
        specialization = specialization.trim(),
        isActive = isActive,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  /**
   * Creates or updates a Course document in `/courses/{courseId}`.
   * Permitted for Super Admin (any department) or Department Admin (ONLY their assigned `departmentId`).
   */
  suspend fun upsertCourse(
    courseId: String,
    courseCode: String,
    courseName: String,
    teacherId: String,
    departmentId: String,
    semesterId: String,
    academicYear: String,
    isActive: Boolean = true,
    actorRole: String,
    actorAssignedDepartmentId: String = "",
  ): Result<Course> = runCatching {
    requireUserId()
    val cleanId = courseId.trim()
    val docRef = db.collection(COLLECTION_COURSES).document(cleanId)
    val payload =
      mapOf(
        "id" to cleanId,
        "courseCode" to courseCode.trim().uppercase(),
        "courseName" to courseName.trim(),
        "teacherId" to teacherId.trim(),
        "departmentId" to departmentId.trim(),
        "semesterId" to semesterId.trim(),
        "academicYear" to academicYear.trim(),
        "isActive" to isActive,
      )
    try {
      docRef.set(payload).await()
      recordAuditLog(
        actorRole = actorRole,
        assignedDepartmentId = actorAssignedDepartmentId,
        action = "UPSERT_COURSE",
        targetCollection = COLLECTION_COURSES,
        targetDocumentId = cleanId,
        details = "Saved course ${courseCode.trim().uppercase()} ($cleanId) in $departmentId",
      )
      Course(
        id = cleanId,
        courseCode = courseCode.trim().uppercase(),
        courseName = courseName.trim(),
        teacherId = teacherId.trim(),
        departmentId = departmentId.trim(),
        semesterId = semesterId.trim(),
        academicYear = academicYear.trim(),
        isActive = isActive,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  /**
   * Creates or updates an academic semester record in `/semesters/{semesterId}`.
   * Requires Super Admin privileges.
   */
  suspend fun upsertSemester(
    semesterId: String,
    name: String,
    academicYear: String,
    isActive: Boolean = true,
    isEvaluationOpen: Boolean = true,
  ): Result<Semester> = runCatching {
    requireUserId()
    val cleanId = semesterId.trim()
    require(cleanId.isNotBlank()) { "Semester ID is required." }
    require(name.trim().length >= 2) { "Semester name must be at least 2 characters." }
    require(academicYear.trim().length >= 4) { "Academic year must be at least 4 characters (e.g., 2026/27)." }

    val docRef = db.collection(COLLECTION_SEMESTERS).document(cleanId)
    val payload =
      mapOf(
        "id" to cleanId,
        "name" to name.trim(),
        "academicYear" to academicYear.trim(),
        "isActive" to isActive,
        "isEvaluationOpen" to isEvaluationOpen,
      )
    try {
      docRef.set(payload).await()
      recordAuditLog(
        actorRole = UserProfile.ROLE_SUPER_ADMIN,
        assignedDepartmentId = "",
        action = "UPSERT_SEMESTER",
        targetCollection = COLLECTION_SEMESTERS,
        targetDocumentId = cleanId,
        details = "Saved semester ${name.trim()} (${academicYear.trim()}, open=$isEvaluationOpen)",
      )
      Semester(
        id = cleanId,
        name = name.trim(),
        academicYear = academicYear.trim(),
        isActive = isActive,
        isEvaluationOpen = isEvaluationOpen,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  /**
   * Loads the university-wide Super Admin Dashboard summary from Cloud Firestore.
   * Requires Super Admin privileges.
   */
  suspend fun loadSuperAdminDashboardSummary(
    adminProfile: UserProfile,
  ): Result<SuperAdminDashboardSummary> = runCatching {
    requireUserId()
    val faculties =
      db.collection(COLLECTION_FACULTIES)
        .get()
        .await()
        .toObjects(Faculty::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedBy { it.name }

    val departments =
      db.collection(COLLECTION_DEPARTMENTS)
        .get()
        .await()
        .toObjects(Department::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedBy { it.name }

    val teachers =
      db.collection(COLLECTION_TEACHERS)
        .get()
        .await()
        .toObjects(Teacher::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedBy { it.fullName }

    val courses =
      db.collection(COLLECTION_COURSES)
        .get()
        .await()
        .toObjects(Course::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedBy { it.courseCode }

    val allUsers =
      db.collection(COLLECTION_USERS)
        .get()
        .await()
        .toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

    val students = allUsers.filter { it.role == UserProfile.ROLE_STUDENT }

    val administrators =
      db.collection(COLLECTION_ADMINS)
        .get()
        .await()
        .toObjects(
          AdminAccountRecord::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
        .sortedBy { it.fullName }

    val semesters =
      db.collection(COLLECTION_SEMESTERS)
        .get()
        .await()
        .toObjects(Semester::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

    val evaluations =
      db.collection(COLLECTION_EVALUATIONS)
        .get()
        .await()
        .toObjects(
          TeacherEvaluation::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )

    val auditLogs =
      db.collection(COLLECTION_AUDIT_LOGS)
        .limit(50)
        .get()
        .await()
        .toObjects(AdminAuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }

    val facultiesById = faculties.associateBy { it.id }
    val activeCourses = courses.filter { it.isActive }
    val activeTeachers = teachers.filter { it.isActive }

    // Compute per-department performance comparison
    val deptComparisons =
      departments.map { dept ->
        val deptStudents = students.filter { it.departmentId == dept.id && it.isActive }
        val deptTeachers = activeTeachers.filter { it.departmentId == dept.id }
        val deptCourses = activeCourses.filter { it.departmentId == dept.id }
        val deptEvals = evaluations.filter { it.departmentId == dept.id }
        val expectedEvals = deptStudents.size * deptCourses.size
        val completionPct =
          if (expectedEvals > 0) {
            ((deptEvals.size.toDouble() / expectedEvals.toDouble()) * 100.0)
              .roundToInt()
              .coerceIn(0, 100)
          } else if (deptEvals.isNotEmpty()) {
            100
          } else {
            0
          }
        val avgScore =
          if (deptEvals.isNotEmpty()) {
            deptEvals.map { it.overallScore }.average()
          } else {
            0.0
          }
        DepartmentPerformanceRow(
          departmentId = dept.id,
          departmentName = dept.name,
          facultyId = dept.facultyId,
          facultyName =
            WcuAcademicCatalogFallback.resolveFacultyName(
              facultiesById[dept.facultyId],
              dept.facultyId,
            ),
          studentCount = deptStudents.size,
          activeTeacherCount = deptTeachers.size,
          activeCourseCount = deptCourses.size,
          submittedEvaluationCount = deptEvals.size,
          averageScore = avgScore,
          completionPercentage = completionPct,
        )
      }

    val totalExpectedUniversityEvals =
      departments.sumOf { dept ->
        val deptStudentCount = students.count { it.departmentId == dept.id && it.isActive }
        val deptCourseCount = activeCourses.count { it.departmentId == dept.id }
        deptStudentCount * deptCourseCount
      }
    val totalPending = (totalExpectedUniversityEvals - evaluations.size).coerceAtLeast(0)
    val universityCompletionPct =
      if (totalExpectedUniversityEvals > 0) {
        ((evaluations.size.toDouble() / totalExpectedUniversityEvals.toDouble()) * 100.0)
          .roundToInt()
          .coerceIn(0, 100)
      } else if (evaluations.isNotEmpty()) {
        100
      } else {
        0
      }

    val overallAvg =
      if (evaluations.isNotEmpty()) {
        evaluations.map { it.overallScore }.average()
      } else {
        0.0
      }

    SuperAdminDashboardSummary(
      adminProfile = adminProfile,
      totalFaculties = faculties.size,
      totalDepartments = departments.size,
      totalStudents = students.size,
      totalTeachers = activeTeachers.size,
      totalActiveCourses = activeCourses.size,
      totalAdministrators = administrators.size,
      totalSubmittedEvaluations = evaluations.size,
      totalPendingEvaluations = totalPending,
      completionPercentage = universityCompletionPct,
      overallUniversityRating = overallAvg,
      faculties = faculties,
      departments = departments,
      teachers = teachers,
      courses = courses,
      students = students,
      administrators = administrators,
      semesters = semesters,
      departmentComparisons = deptComparisons,
      recentAuditLogs = auditLogs,
    )
  }

  /**
   * Loads the strictly department-scoped dashboard summary for a Department Admin.
   *
   * IMPORTANT: Every Firestore query uses server-side `.whereEqualTo("departmentId", deptId)`
   * constraints so it satisfies the least-privilege `firestore.rules` without touching other departments.
   */
  suspend fun loadDepartmentAdminDashboardSummary(
    adminRecord: AdminAccountRecord,
  ): Result<DepartmentAdminDashboardSummary> = runCatching {
    requireUserId()
    val deptId =
      adminRecord.departmentId?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Department Admin record is missing assigned departmentId.")
    val facId =
      adminRecord.facultyId?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Department Admin record is missing assigned facultyId.")

    val faculty =
      db.collection(COLLECTION_FACULTIES)
        .document(facId)
        .get()
        .await()
        .toObject(Faculty::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

    val department =
      db.collection(COLLECTION_DEPARTMENTS)
        .document(deptId)
        .get()
        .await()
        .toObject(Department::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

    // Department-scoped query for students (satisfies /users list rule)
    val departmentStudents =
      db.collection(COLLECTION_USERS)
        .whereEqualTo("departmentId", deptId)
        .whereEqualTo("role", UserProfile.ROLE_STUDENT)
        .get()
        .await()
        .toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedBy { it.fullName }

    // Department-scoped query for teachers
    val departmentTeachers =
      db.collection(COLLECTION_TEACHERS)
        .whereEqualTo("departmentId", deptId)
        .get()
        .await()
        .toObjects(Teacher::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedBy { it.fullName }

    // Department-scoped query for courses
    val departmentCourses =
      db.collection(COLLECTION_COURSES)
        .whereEqualTo("departmentId", deptId)
        .get()
        .await()
        .toObjects(Course::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedBy { it.courseCode }

    // Department-scoped query for evaluations (satisfies /evaluations list rule)
    val departmentEvaluations =
      db.collection(COLLECTION_EVALUATIONS)
        .whereEqualTo("departmentId", deptId)
        .get()
        .await()
        .toObjects(
          TeacherEvaluation::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
        .sortedByDescending { it.submittedAt?.toDate()?.time ?: 0L }

    // Department-scoped query for audit logs (satisfies /audit_logs list rule)
    val departmentAuditLogs =
      db.collection(COLLECTION_AUDIT_LOGS)
        .whereEqualTo("assignedDepartmentId", deptId)
        .limit(30)
        .get()
        .await()
        .toObjects(AdminAuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }

    val activeStudents = departmentStudents.filter { it.isActive }
    val activeTeachers = departmentTeachers.filter { it.isActive }
    val activeCourses = departmentCourses.filter { it.isActive }

    val expectedEvaluations = activeStudents.size * activeCourses.size
    val pendingCount = (expectedEvaluations - departmentEvaluations.size).coerceAtLeast(0)
    val completionPct =
      if (expectedEvaluations > 0) {
        ((departmentEvaluations.size.toDouble() / expectedEvaluations.toDouble()) * 100.0)
          .roundToInt()
          .coerceIn(0, 100)
      } else if (departmentEvaluations.isNotEmpty()) {
        100
      } else {
        0
      }

    val avgScore =
      if (departmentEvaluations.isNotEmpty()) {
        departmentEvaluations.map { it.overallScore }.average()
      } else {
        0.0
      }

    DepartmentAdminDashboardSummary(
      adminRecord = adminRecord,
      department = department,
      faculty = faculty,
      departmentDisplayName =
        WcuAcademicCatalogFallback.resolveDepartmentName(department, deptId),
      facultyDisplayName = WcuAcademicCatalogFallback.resolveFacultyName(faculty, facId),
      totalStudents = activeStudents.size,
      totalActiveTeachers = activeTeachers.size,
      totalActiveCourses = activeCourses.size,
      totalSubmittedEvaluations = departmentEvaluations.size,
      pendingEvaluationsCount = pendingCount,
      completionPercentage = completionPct,
      departmentAverageRating = avgScore,
      departmentStudents = departmentStudents,
      departmentTeachers = departmentTeachers,
      departmentCourses = departmentCourses,
      departmentEvaluations = departmentEvaluations,
      recentAuditLogs = departmentAuditLogs,
    )
  }

  /**
   * Authorizes, fetches, filters, and aggregates evaluation data for PDF or Excel (.xlsx) export.
   *
   * SECURITY INVARIANTS (Stage B):
   * 1. Verifies authenticated user identity (`auth.currentUser`).
   * 2. Verifies user role and active status using `/users/{uid}` and `/admins/{uid}`.
   * 3. Rejects all Student accounts with `SecurityException`.
   * 4. Rejects inactive Department Admin or Super Admin accounts with `SecurityException`.
   * 5. For `department_admin`, validates `facultyId` and `departmentId` against trusted `/admins/{uid}`
   *    and rejects any attempt to export another department's data with `SecurityException`.
   * 6. For `super_admin`, supports university-wide or filtered exports across faculties, departments,
   *    teachers, courses, and semesters.
   */
  suspend fun prepareAuthorizedExportDataset(
    filter: EvaluationReportExportFilter = EvaluationReportExportFilter(),
  ): Result<EvaluationExportDataset> = runCatching {
    val currentUser =
      auth.currentUser
        ?: throw SecurityException("Export access denied: User must be signed in.")
    val callerUid = currentUser.uid

    // 1. Check /users/{uid} if present to block students or deactivated user profiles immediately
    val userProfile =
      try {
        val userSnap = db.collection(COLLECTION_USERS).document(callerUid).get().await()
        if (userSnap.exists()) {
          userSnap.toObject(
            UserProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        } else {
          null
        }
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        null
      }

    if (userProfile != null) {
      if (userProfile.role == UserProfile.ROLE_STUDENT) {
        throw SecurityException(
          "Export access denied: Students are not permitted to export evaluation reports."
        )
      }
      if (!userProfile.isActive) {
        throw SecurityException(
          "Export access denied: User profile ($callerUid) is inactive."
        )
      }
    }

    // 2. Fetch trusted /admins/{uid} document
    val adminRecord =
      try {
        val adminSnap = db.collection(COLLECTION_ADMINS).document(callerUid).get().await()
        if (adminSnap.exists()) {
          adminSnap.toObject(
            AdminAccountRecord::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        } else {
          null
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        // In firestore.rules, /admins/{uid} get is denied if the record does not exist or isActive == false
        val isBootstrapped =
          currentUser.email == BOOTSTRAPPED_SUPER_ADMIN_EMAIL && currentUser.isEmailVerified
        if (!isBootstrapped) {
          throw SecurityException(
            "Export access denied: Active administrator authorization record (/admins/$callerUid) is required.",
            e,
          )
        }
        null
      }

    if (adminRecord != null && !adminRecord.isActive) {
      throw SecurityException(
        "Export access denied: Administrator account ($callerUid) is deactivated."
      )
    }

    val isBootstrappedSuperAdmin =
      currentUser.email == BOOTSTRAPPED_SUPER_ADMIN_EMAIL &&
        currentUser.isEmailVerified &&
        (adminRecord == null || adminRecord.isActive)

    val isSuperAdmin = (adminRecord?.isSuperAdmin == true) || isBootstrappedSuperAdmin
    val isDepartmentAdmin = !isSuperAdmin && (adminRecord?.isDepartmentAdmin == true)

    if (!isSuperAdmin && !isDepartmentAdmin) {
      throw SecurityException(
        "Export access denied: Caller ($callerUid) does not hold active Department Admin or Super Admin privileges."
      )
    }

    if (isDepartmentAdmin) {
      val trustedAdmin =
        adminRecord
          ?: throw SecurityException("Export access denied: Missing /admins/$callerUid record.")
      val assignedDeptId =
        trustedAdmin.departmentId?.takeIf { it.isNotBlank() }
          ?: throw SecurityException("Export access denied: Department Admin has no assigned departmentId.")
      val assignedFacId =
        trustedAdmin.facultyId?.takeIf { it.isNotBlank() }
          ?: throw SecurityException("Export access denied: Department Admin has no assigned facultyId.")

      // Reject cross-department or cross-faculty filter attempts
      if (!filter.departmentId.isNullOrBlank() && filter.departmentId.trim() != assignedDeptId) {
        throw SecurityException(
          "Export access denied: Department Admin ($assignedDeptId) cannot export evaluation reports for another department (${filter.departmentId})."
        )
      }
      if (!filter.facultyId.isNullOrBlank() && filter.facultyId.trim() != assignedFacId) {
        throw SecurityException(
          "Export access denied: Department Admin ($assignedFacId) cannot export evaluation reports for another faculty (${filter.facultyId})."
        )
      }

      // Fetch department-scoped data from Firestore
      val faculty =
        db.collection(COLLECTION_FACULTIES)
          .document(assignedFacId)
          .get()
          .await()
          .toObject(Faculty::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      val department =
        db.collection(COLLECTION_DEPARTMENTS)
          .document(assignedDeptId)
          .get()
          .await()
          .toObject(Department::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      val semesters =
        db.collection(COLLECTION_SEMESTERS)
          .get()
          .await()
          .toObjects(Semester::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      val deptTeachers =
        db.collection(COLLECTION_TEACHERS)
          .whereEqualTo("departmentId", assignedDeptId)
          .get()
          .await()
          .toObjects(Teacher::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      val deptCourses =
        db.collection(COLLECTION_COURSES)
          .whereEqualTo("departmentId", assignedDeptId)
          .get()
          .await()
          .toObjects(Course::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      val deptEvaluations =
        db.collection(COLLECTION_EVALUATIONS)
          .whereEqualTo("departmentId", assignedDeptId)
          .get()
          .await()
          .toObjects(
            TeacherEvaluation::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
      val deptStudents =
        db.collection(COLLECTION_USERS)
          .whereEqualTo("departmentId", assignedDeptId)
          .whereEqualTo("role", UserProfile.ROLE_STUDENT)
          .get()
          .await()
          .toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

      val enforcedFilter =
        filter.copy(
          facultyId = assignedFacId,
          departmentId = assignedDeptId,
        )

      buildExportDatasetFromRecords(
        actorUid = callerUid,
        actorName =
          trustedAdmin.fullName.ifBlank {
            userProfile?.fullName?.ifBlank { trustedAdmin.email } ?: trustedAdmin.email
          },
        actorRole = UserProfile.ROLE_DEPARTMENT_ADMIN,
        assignedFacultyId = assignedFacId,
        assignedDepartmentId = assignedDeptId,
        filter = enforcedFilter,
        faculties = listOfNotNull(faculty),
        departments = listOfNotNull(department),
        semesters = semesters,
        teachers = deptTeachers,
        courses = deptCourses,
        students = deptStudents,
        evaluations = deptEvaluations,
      )
    } else {
      // Super Admin: load university-wide records and apply optional filters
      val faculties =
        db.collection(COLLECTION_FACULTIES)
          .get()
          .await()
          .toObjects(Faculty::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      val departments =
        db.collection(COLLECTION_DEPARTMENTS)
          .get()
          .await()
          .toObjects(Department::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      val semesters =
        db.collection(COLLECTION_SEMESTERS)
          .get()
          .await()
          .toObjects(Semester::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      val teachers =
        db.collection(COLLECTION_TEACHERS)
          .get()
          .await()
          .toObjects(Teacher::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      val courses =
        db.collection(COLLECTION_COURSES)
          .get()
          .await()
          .toObjects(Course::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      val students =
        db.collection(COLLECTION_USERS)
          .get()
          .await()
          .toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .filter { it.role == UserProfile.ROLE_STUDENT }
      val evaluations =
        db.collection(COLLECTION_EVALUATIONS)
          .get()
          .await()
          .toObjects(
            TeacherEvaluation::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )

      buildExportDatasetFromRecords(
        actorUid = callerUid,
        actorName =
          adminRecord?.fullName?.takeIf { it.isNotBlank() }
            ?: userProfile?.fullName?.takeIf { it.isNotBlank() }
            ?: currentUser.email.orEmpty().ifBlank { "WCU Super Admin" },
        actorRole = UserProfile.ROLE_SUPER_ADMIN,
        assignedFacultyId = null,
        assignedDepartmentId = null,
        filter = filter,
        faculties = faculties,
        departments = departments,
        semesters = semesters,
        teachers = teachers,
        courses = courses,
        students = students,
        evaluations = evaluations,
      )
    }
  }

  companion object {
    const val COLLECTION_USERS = "users"
    const val COLLECTION_ADMINS = "admins"
    const val COLLECTION_AUDIT_LOGS = "audit_logs"
    const val COLLECTION_FACULTIES = "faculties"
    const val COLLECTION_DEPARTMENTS = "departments"
    const val COLLECTION_TEACHERS = "teachers"
    const val COLLECTION_COURSES = "courses"
    const val COLLECTION_SEMESTERS = "semesters"
    const val COLLECTION_EVALUATIONS = "evaluations"
    const val ACTION_EXPORT_EVALUATION_REPORT = "EXPORT_EVALUATION_REPORT"
    const val BOOTSTRAPPED_SUPER_ADMIN_EMAIL = "abelbimrew868@gmail.com"

    fun classifyRating(score: Double, evaluationCount: Int): String {
      if (evaluationCount <= 0 || score <= 0.0) return "No Submissions Yet"
      return when {
        score >= 4.50 -> "Excellent (Distinction)"
        score >= 4.00 -> "Very Good"
        score >= 3.00 -> "Satisfactory"
        else -> "Needs Improvement"
      }
    }

    /**
     * Pure, deterministic aggregation builder that enforces role-based scope and filter matching
     * to produce an [EvaluationExportDataset].
     */
    fun buildExportDatasetFromRecords(
      actorUid: String,
      actorName: String,
      actorRole: String,
      assignedFacultyId: String?,
      assignedDepartmentId: String?,
      filter: EvaluationReportExportFilter,
      faculties: List<Faculty>,
      departments: List<Department>,
      semesters: List<Semester>,
      teachers: List<Teacher>,
      courses: List<Course>,
      students: List<UserProfile>,
      evaluations: List<TeacherEvaluation>,
    ): EvaluationExportDataset {
      if (actorRole == UserProfile.ROLE_STUDENT) {
        throw SecurityException("Export access denied: Students cannot export evaluation reports.")
      }

      val isSuperAdmin =
        actorRole == UserProfile.ROLE_SUPER_ADMIN || actorRole == UserProfile.ROLE_ADMIN
      val isDeptAdmin = actorRole == UserProfile.ROLE_DEPARTMENT_ADMIN

      if (!isSuperAdmin && !isDeptAdmin) {
        throw SecurityException("Export access denied: Unauthorized role '$actorRole'.")
      }

      val effectiveFacultyId =
        if (isDeptAdmin) {
          val assignedFac =
            assignedFacultyId?.takeIf { it.isNotBlank() }
              ?: throw SecurityException("Department Admin missing assigned facultyId.")
          if (!filter.facultyId.isNullOrBlank() && filter.facultyId.trim() != assignedFac) {
            throw SecurityException(
              "Department Admin ($assignedFac) cannot export faculty ${filter.facultyId}."
            )
          }
          assignedFac
        } else {
          filter.facultyId?.trim()?.takeIf { it.isNotBlank() }
        }

      val effectiveDepartmentId =
        if (isDeptAdmin) {
          val assignedDept =
            assignedDepartmentId?.takeIf { it.isNotBlank() }
              ?: throw SecurityException("Department Admin missing assigned departmentId.")
          if (!filter.departmentId.isNullOrBlank() && filter.departmentId.trim() != assignedDept) {
            throw SecurityException(
              "Department Admin ($assignedDept) cannot export department ${filter.departmentId}."
            )
          }
          assignedDept
        } else {
          filter.departmentId?.trim()?.takeIf { it.isNotBlank() }
        }

      val cleanTeacherId = filter.teacherId?.trim()?.takeIf { it.isNotBlank() }
      val cleanCourseId = filter.courseId?.trim()?.takeIf { it.isNotBlank() }
      val cleanSemesterId = filter.semesterId?.trim()?.takeIf { it.isNotBlank() }

      val facultiesById = faculties.associateBy { it.id }
      val departmentsById = departments.associateBy { it.id }
      val semestersById = semesters.associateBy { it.id }
      val teachersById = teachers.associateBy { it.id }
      val coursesById = courses.associateBy { it.id }

      // Filter evaluations according to authorized scope and user-selected filters
      val filteredEvaluations =
        evaluations.filter { eval ->
          val evalFacultyId =
            eval.facultyId.ifBlank {
              departmentsById[eval.departmentId]?.facultyId
                ?: teachersById[eval.teacherId]?.facultyId.orEmpty()
            }
          val matchesFaculty =
            effectiveFacultyId == null || evalFacultyId == effectiveFacultyId
          val matchesDepartment =
            effectiveDepartmentId == null || eval.departmentId == effectiveDepartmentId
          val matchesTeacher = cleanTeacherId == null || eval.teacherId == cleanTeacherId
          val matchesCourse = cleanCourseId == null || eval.courseId == cleanCourseId
          val matchesSemester = cleanSemesterId == null || eval.semesterId == cleanSemesterId
          val submittedMillis = eval.submittedAt?.toDate()?.time
          val matchesStartDate =
            filter.startDateMillis == null ||
              (submittedMillis != null && submittedMillis >= filter.startDateMillis)
          val matchesEndDate =
            filter.endDateMillis == null ||
              (submittedMillis != null && submittedMillis <= filter.endDateMillis)

          matchesFaculty &&
            matchesDepartment &&
            matchesTeacher &&
            matchesCourse &&
            matchesSemester &&
            matchesStartDate &&
            matchesEndDate
        }

      // Filter courses to include in the report table (matching scope & filters)
      val scopedCourses =
        courses.filter { course ->
          val courseFacId =
            departmentsById[course.departmentId]?.facultyId
              ?: teachersById[course.teacherId]?.facultyId.orEmpty()
          val matchesFaculty =
            effectiveFacultyId == null || courseFacId == effectiveFacultyId
          val matchesDepartment =
            effectiveDepartmentId == null || course.departmentId == effectiveDepartmentId
          val matchesTeacher = cleanTeacherId == null || course.teacherId == cleanTeacherId
          val matchesCourse = cleanCourseId == null || course.id == cleanCourseId
          val matchesSemester = cleanSemesterId == null || course.semesterId == cleanSemesterId
          matchesFaculty && matchesDepartment && matchesTeacher && matchesCourse && matchesSemester
        }

      // Build distinct (teacherId, courseId, semesterId) groups from both evaluations and scopedCourses
      data class GroupKey(
        val teacherId: String,
        val courseId: String,
        val semesterId: String,
        val departmentId: String,
      )

      val evalsByGroup =
        filteredEvaluations.groupBy { eval ->
          GroupKey(
            teacherId = eval.teacherId,
            courseId = eval.courseId,
            semesterId = eval.semesterId,
            departmentId = eval.departmentId,
          )
        }

      val allGroupKeys = linkedSetOf<GroupKey>()
      allGroupKeys.addAll(evalsByGroup.keys)
      scopedCourses.forEach { course ->
        val key =
          GroupKey(
            teacherId = course.teacherId,
            courseId = course.id,
            semesterId = course.semesterId,
            departmentId = course.departmentId,
          )
        // If date filter is active, only include courses that have matching evaluations in that date window
        if (filter.startDateMillis == null && filter.endDateMillis == null) {
          allGroupKeys.add(key)
        }
      }

      val reportRows =
        allGroupKeys
          .map { key ->
            val groupEvals = evalsByGroup[key].orEmpty()
            val teacher = teachersById[key.teacherId]
            val course = coursesById[key.courseId]
            val deptId =
              key.departmentId.ifBlank {
                course?.departmentId ?: teacher?.departmentId.orEmpty()
              }
            val dept = departmentsById[deptId]
            val facId =
              dept?.facultyId
                ?: teacher?.facultyId
                ?: groupEvals.firstOrNull()?.facultyId.orEmpty()
            val fac = facultiesById[facId]
            val sem = semestersById[key.semesterId]

            val avgScore =
              if (groupEvals.isNotEmpty()) {
                groupEvals.map { it.overallScore }.average()
              } else {
                0.0
              }

            val catAverages =
              WcuEvaluationRubricCategories.ALL_CATEGORIES.associate { (catId, _) ->
                val scores =
                  groupEvals.mapNotNull { eval ->
                    eval.categoryRatings[catId]
                      ?: eval.overallScore.takeIf { it > 0.0 }
                  }
                catId to (if (scores.isNotEmpty()) scores.average() else 0.0)
              }

            TeacherEvaluationReportRow(
              teacherId = key.teacherId,
              teacherName = teacher?.fullName ?: "Instructor (${key.teacherId})",
              teacherEmail = teacher?.email ?: "",
              specialization = teacher?.specialization ?: "Academic Staff",
              facultyId = facId,
              facultyName = WcuAcademicCatalogFallback.resolveFacultyName(fac, facId),
              departmentId = deptId,
              departmentName = WcuAcademicCatalogFallback.resolveDepartmentName(dept, deptId),
              courseId = key.courseId,
              courseCode = course?.courseCode ?: key.courseId.uppercase(),
              courseName = course?.courseName ?: "Course (${key.courseId})",
              semesterId = key.semesterId,
              semesterLabel = sem?.name ?: key.semesterId.ifBlank { "Active Semester" },
              academicYear =
                course?.academicYear?.takeIf { it.isNotBlank() }
                  ?: sem?.academicYear?.takeIf { it.isNotBlank() }
                  ?: groupEvals.firstOrNull()?.academicYear?.takeIf { it.isNotBlank() }
                  ?: "2026/27",
              evaluationCount = groupEvals.size,
              averageRating = avgScore,
              categoryScores = catAverages,
              performanceClassification = classifyRating(avgScore, groupEvals.size),
            )
          }
          .sortedWith(
            compareByDescending<TeacherEvaluationReportRow> { it.evaluationCount }
              .thenByDescending { it.averageRating }
              .thenBy { it.departmentName }
              .thenBy { it.teacherName }
          )

      val overallCategoryAverages =
        WcuEvaluationRubricCategories.ALL_CATEGORIES.associate { (catId, _) ->
          val scores =
            filteredEvaluations.mapNotNull { eval ->
              eval.categoryRatings[catId] ?: eval.overallScore.takeIf { it > 0.0 }
            }
          catId to (if (scores.isNotEmpty()) scores.average() else 0.0)
        }

      val overallAverageRating =
        if (filteredEvaluations.isNotEmpty()) {
          filteredEvaluations.map { it.overallScore }.average()
        } else {
          0.0
        }

      val scopedDepartments =
        departments.filter { dept ->
          (effectiveFacultyId == null || dept.facultyId == effectiveFacultyId) &&
            (effectiveDepartmentId == null || dept.id == effectiveDepartmentId)
        }

      val deptComparisons =
        scopedDepartments.map { dept ->
          val deptStudents = students.filter { it.departmentId == dept.id && it.isActive }
          val deptTeachers = teachers.filter { it.departmentId == dept.id && it.isActive }
          val deptCourses = courses.filter { it.departmentId == dept.id && it.isActive }
          val deptEvals = filteredEvaluations.filter { it.departmentId == dept.id }
          val expected = deptStudents.size * deptCourses.size
          val pct =
            if (expected > 0) {
              ((deptEvals.size.toDouble() / expected.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 100)
            } else if (deptEvals.isNotEmpty()) {
              100
            } else {
              0
            }
          val avg = if (deptEvals.isNotEmpty()) deptEvals.map { it.overallScore }.average() else 0.0
          DepartmentPerformanceRow(
            departmentId = dept.id,
            departmentName = dept.name,
            facultyId = dept.facultyId,
            facultyName =
              WcuAcademicCatalogFallback.resolveFacultyName(
                facultiesById[dept.facultyId],
                dept.facultyId,
              ),
            studentCount = deptStudents.size,
            activeTeacherCount = deptTeachers.size,
            activeCourseCount = deptCourses.size,
            submittedEvaluationCount = deptEvals.size,
            averageScore = avg,
            completionPercentage = pct,
          )
        }

      val activeSem =
        if (cleanSemesterId != null) {
          semestersById[cleanSemesterId]
        } else {
          semesters.firstOrNull { it.isActive } ?: semesters.firstOrNull()
        }

      val facultyLabel =
        if (effectiveFacultyId != null) {
          WcuAcademicCatalogFallback.resolveFacultyName(
            facultiesById[effectiveFacultyId],
            effectiveFacultyId,
          )
        } else {
          "All University Faculties (${faculties.size.coerceAtLeast(1)} Colleges)"
        }

      val departmentLabel =
        if (effectiveDepartmentId != null) {
          WcuAcademicCatalogFallback.resolveDepartmentName(
            departmentsById[effectiveDepartmentId],
            effectiveDepartmentId,
          )
        } else {
          "All Academic Departments (${scopedDepartments.size.coerceAtLeast(1)} Departments)"
        }

      val reportScope =
        when {
          isDeptAdmin -> "DEPT_${effectiveDepartmentId.orEmpty().uppercase()}"
          effectiveDepartmentId != null -> "FILTERED_DEPT_${effectiveDepartmentId.uppercase()}"
          effectiveFacultyId != null -> "FILTERED_FACULTY_${effectiveFacultyId.uppercase()}"
          else -> "UNIVERSITY_WIDE"
        }

      val reportTitle =
        if (isDeptAdmin) {
          "Wachemo University • Department Teacher Evaluation Report ($departmentLabel)"
        } else if (effectiveDepartmentId != null || effectiveFacultyId != null) {
          "Wachemo University • Filtered Institutional Evaluation Report"
        } else {
          "Wachemo University • University-Wide Teacher Evaluation Report"
        }

      val generatedAtIso =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'EAT'", Locale.US).format(Date())

      return EvaluationExportDataset(
        reportTitle = reportTitle,
        universityName = "Wachemo University (WCU)",
        generatedAtIso = generatedAtIso,
        generatedByUid = actorUid,
        generatedByName = actorName,
        generatedByRole = actorRole,
        reportScope = reportScope,
        facultyLabel = facultyLabel,
        departmentLabel = departmentLabel,
        semesterLabel = activeSem?.name ?: cleanSemesterId ?: "Semester I",
        academicYearLabel = activeSem?.academicYear ?: "2026/27",
        appliedFilterSummary = filter.toAuditSummaryString(),
        totalEvaluationsCount = filteredEvaluations.size,
        totalTeachersEvaluated =
          filteredEvaluations.map { it.teacherId }.distinct().size,
        totalCoursesEvaluated =
          filteredEvaluations.map { it.courseId }.distinct().size,
        overallAverageRating = overallAverageRating,
        categoryAverages = overallCategoryAverages,
        teacherCourseRows = reportRows,
        departmentComparisonRows = deptComparisons,
      )
    }
  }
}

