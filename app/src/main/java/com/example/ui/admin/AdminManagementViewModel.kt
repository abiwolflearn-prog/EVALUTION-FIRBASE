package com.example.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.export.EvaluationReportExporter
import com.example.data.model.AdminAccountRecord
import com.example.data.model.AdminAuditLog
import com.example.data.model.Course
import com.example.data.model.Department
import com.example.data.model.DepartmentAdminDashboardSummary
import com.example.data.model.EvaluationReportExportFilter
import com.example.data.model.ExportFormat
import com.example.data.model.Faculty
import com.example.data.model.GeneratedReportFile
import com.example.data.model.SuperAdminDashboardSummary
import com.example.data.model.Teacher
import com.example.data.model.UserProfile
import com.example.data.model.WcuDemoDataProvider
import com.example.data.repository.AdminManagementRepository
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SuperAdminUiState {
  data object Loading : SuperAdminUiState
  data class Success(val summary: SuperAdminDashboardSummary) : SuperAdminUiState
  data class Error(val message: String) : SuperAdminUiState
}

sealed interface DepartmentAdminUiState {
  data object Loading : DepartmentAdminUiState
  data class Success(val summary: DepartmentAdminDashboardSummary) : DepartmentAdminUiState
  data class Error(val message: String) : DepartmentAdminUiState
}

class AdminManagementViewModel(
  private val repository: AdminManagementRepository,
) : ViewModel() {

  private val _superAdminUiState =
    MutableStateFlow<SuperAdminUiState>(SuperAdminUiState.Loading)
  val superAdminUiState: StateFlow<SuperAdminUiState> = _superAdminUiState.asStateFlow()

  private val _departmentAdminUiState =
    MutableStateFlow<DepartmentAdminUiState>(DepartmentAdminUiState.Loading)
  val departmentAdminUiState: StateFlow<DepartmentAdminUiState> =
    _departmentAdminUiState.asStateFlow()

  private val _isMutating = MutableStateFlow(false)
  val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

  private val _isExporting = MutableStateFlow(false)
  val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

  private val _lastExportedReport = MutableStateFlow<GeneratedReportFile?>(null)
  val lastExportedReport: StateFlow<GeneratedReportFile?> = _lastExportedReport.asStateFlow()

  private val _actionBanner = MutableStateFlow<Pair<Boolean, String>?>(null)
  val actionBanner: StateFlow<Pair<Boolean, String>?> = _actionBanner.asStateFlow()

  // Interactive in-memory state when running in Demo Mode
  private var demoAdmins = WcuDemoDataProvider.demoAdministrators
  private var demoFaculties = WcuDemoDataProvider.demoFaculties
  private var demoDepartments = WcuDemoDataProvider.demoDepartments
  private var demoTeachers = WcuDemoDataProvider.demoTeachers
  private var demoCourses = WcuDemoDataProvider.demoCourses
  private var demoAuditLogs = WcuDemoDataProvider.demoAuditLogs

  fun clearActionBanner() {
    _actionBanner.value = null
  }

  fun clearLastExportedReport() {
    _lastExportedReport.value = null
  }

  fun loadSuperAdminDashboard(adminProfile: UserProfile) {
    viewModelScope.launch {
      if (WcuDemoDataProvider.isDemoUid(adminProfile.uid)) {
        _superAdminUiState.value =
          SuperAdminUiState.Success(
            WcuDemoDataProvider.buildSuperAdminDashboardSummary(
              adminProfile = adminProfile,
              faculties = demoFaculties,
              departments = demoDepartments,
              teachers = demoTeachers,
              courses = demoCourses,
              administrators = demoAdmins,
              auditLogs = demoAuditLogs,
            )
          )
        return@launch
      }

      _superAdminUiState.value = SuperAdminUiState.Loading
      repository.loadSuperAdminDashboardSummary(adminProfile)
        .onSuccess { summary ->
          _superAdminUiState.value = SuperAdminUiState.Success(summary)
        }
        .onFailure { error ->
          _superAdminUiState.value =
            SuperAdminUiState.Error(
              error.localizedMessage ?: "Failed to load Super Admin dashboard from Firestore."
            )
        }
    }
  }

  fun loadDepartmentAdminDashboard(adminRecord: AdminAccountRecord) {
    viewModelScope.launch {
      if (WcuDemoDataProvider.isDemoUid(adminRecord.uid)) {
        _departmentAdminUiState.value =
          DepartmentAdminUiState.Success(
            WcuDemoDataProvider.buildDepartmentAdminDashboardSummary(
              adminRecord = adminRecord,
              teachers = demoTeachers,
              courses = demoCourses,
              auditLogs = demoAuditLogs,
            )
          )
        return@launch
      }

      _departmentAdminUiState.value = DepartmentAdminUiState.Loading
      repository.loadDepartmentAdminDashboardSummary(adminRecord)
        .onSuccess { summary ->
          _departmentAdminUiState.value = DepartmentAdminUiState.Success(summary)
        }
        .onFailure { error ->
          _departmentAdminUiState.value =
            DepartmentAdminUiState.Error(
              error.localizedMessage ?: "Failed to load Department Admin dashboard from Firestore."
            )
        }
    }
  }

  fun createDepartmentAdmin(
    targetUid: String,
    fullName: String,
    email: String,
    facultyId: String,
    departmentId: String,
    adminProfile: UserProfile,
  ) {
    viewModelScope.launch {
      if (WcuDemoDataProvider.isDemoUid(adminProfile.uid)) {
        val newUid =
          targetUid.trim().ifBlank { "demo_dept_admin_${UUID.randomUUID().toString().take(6)}" }
        val newRecord =
          AdminAccountRecord(
            uid = newUid,
            fullName = fullName.trim(),
            email = email.trim(),
            role = UserProfile.ROLE_DEPARTMENT_ADMIN,
            facultyId = facultyId,
            departmentId = departmentId,
            isActive = true,
            createdBy = adminProfile.uid,
          )
        demoAdmins = demoAdmins + newRecord
        _actionBanner.value =
          false to "Demo Department Admin (${newRecord.email}) created and assigned to $departmentId."
        loadSuperAdminDashboard(adminProfile)
        return@launch
      }

      _isMutating.value = true
      _actionBanner.value = null
      repository.createDepartmentAdminAccount(
        targetUid = targetUid,
        fullName = fullName,
        email = email,
        facultyId = facultyId,
        departmentId = departmentId,
      )
        .onSuccess { created ->
          _isMutating.value = false
          _actionBanner.value =
            false to "Department Admin (${created.email}) created and assigned to ${created.departmentId}."
          loadSuperAdminDashboard(adminProfile)
        }
        .onFailure { error ->
          _isMutating.value = false
          _actionBanner.value =
            true to (error.localizedMessage ?: "Failed to create Department Admin account.")
        }
    }
  }

  fun setAdministratorStatus(
    targetAdminUid: String,
    isActive: Boolean,
    adminProfile: UserProfile,
  ) {
    viewModelScope.launch {
      if (WcuDemoDataProvider.isDemoUid(adminProfile.uid)) {
        demoAdmins =
          demoAdmins.map { if (it.uid == targetAdminUid) it.copy(isActive = isActive) else it }
        val verb = if (isActive) "activated" else "deactivated"
        _actionBanner.value = false to "Administrator $targetAdminUid has been $verb."
        loadSuperAdminDashboard(adminProfile)
        return@launch
      }

      _isMutating.value = true
      _actionBanner.value = null
      repository.setAdministratorActiveStatus(
        targetAdminUid = targetAdminUid,
        isActive = isActive,
      )
        .onSuccess { updated ->
          _isMutating.value = false
          val verb = if (updated.isActive) "activated" else "deactivated"
          _actionBanner.value =
            false to "Administrator account ${updated.email} has been $verb."
          loadSuperAdminDashboard(adminProfile)
        }
        .onFailure { error ->
          _isMutating.value = false
          _actionBanner.value =
            true to (error.localizedMessage ?: "Failed to update administrator status.")
        }
    }
  }

  fun reassignDepartmentAdmin(
    targetAdminUid: String,
    newFacultyId: String,
    newDepartmentId: String,
    adminProfile: UserProfile,
  ) {
    viewModelScope.launch {
      if (WcuDemoDataProvider.isDemoUid(adminProfile.uid)) {
        demoAdmins =
          demoAdmins.map {
            if (it.uid == targetAdminUid) {
              it.copy(facultyId = newFacultyId, departmentId = newDepartmentId)
            } else {
              it
            }
          }
        _actionBanner.value =
          false to "Reassigned $targetAdminUid to $newDepartmentId ($newFacultyId)."
        loadSuperAdminDashboard(adminProfile)
        return@launch
      }

      _isMutating.value = true
      _actionBanner.value = null
      repository.updateDepartmentAdminAssignment(
        targetAdminUid = targetAdminUid,
        newFacultyId = newFacultyId,
        newDepartmentId = newDepartmentId,
      )
        .onSuccess { updated ->
          _isMutating.value = false
          _actionBanner.value =
            false to "Reassigned ${updated.email} to ${updated.departmentId} (${updated.facultyId})."
          loadSuperAdminDashboard(adminProfile)
        }
        .onFailure { error ->
          _isMutating.value = false
          _actionBanner.value =
            true to (error.localizedMessage ?: "Failed to reassign Department Admin.")
        }
    }
  }

  fun saveFaculty(
    facultyId: String,
    name: String,
    description: String,
    isActive: Boolean,
    adminProfile: UserProfile,
  ) {
    viewModelScope.launch {
      if (WcuDemoDataProvider.isDemoUid(adminProfile.uid)) {
        val item = Faculty(id = facultyId.trim(), name = name.trim(), description = description.trim(), isActive = isActive)
        demoFaculties = demoFaculties.filterNot { it.id == item.id } + item
        _actionBanner.value = false to "Faculty '${item.name}' saved."
        loadSuperAdminDashboard(adminProfile)
        return@launch
      }

      _isMutating.value = true
      repository.upsertFaculty(facultyId, name, description, isActive)
        .onSuccess { faculty ->
          _isMutating.value = false
          _actionBanner.value = false to "Faculty '${faculty.name}' saved to Firestore."
          loadSuperAdminDashboard(adminProfile)
        }
        .onFailure { error ->
          _isMutating.value = false
          _actionBanner.value =
            true to (error.localizedMessage ?: "Failed to save faculty.")
        }
    }
  }

  fun saveDepartment(
    departmentId: String,
    name: String,
    facultyId: String,
    description: String,
    isActive: Boolean,
    adminProfile: UserProfile,
  ) {
    viewModelScope.launch {
      if (WcuDemoDataProvider.isDemoUid(adminProfile.uid)) {
        val item =
          Department(
            id = departmentId.trim(),
            name = name.trim(),
            facultyId = facultyId.trim(),
            description = description.trim(),
            isActive = isActive,
          )
        demoDepartments = demoDepartments.filterNot { it.id == item.id } + item
        _actionBanner.value = false to "Department '${item.name}' saved."
        loadSuperAdminDashboard(adminProfile)
        return@launch
      }

      _isMutating.value = true
      repository.upsertDepartment(departmentId, name, facultyId, description, isActive)
        .onSuccess { dept ->
          _isMutating.value = false
          _actionBanner.value = false to "Department '${dept.name}' saved to Firestore."
          loadSuperAdminDashboard(adminProfile)
        }
        .onFailure { error ->
          _isMutating.value = false
          _actionBanner.value =
            true to (error.localizedMessage ?: "Failed to save department.")
        }
    }
  }

  fun saveTeacherAsDepartmentAdmin(
    teacherId: String,
    fullName: String,
    email: String,
    specialization: String,
    isActive: Boolean,
    adminRecord: AdminAccountRecord,
  ) {
    val facId = adminRecord.facultyId.orEmpty()
    val deptId = adminRecord.departmentId.orEmpty()
    viewModelScope.launch {
      if (WcuDemoDataProvider.isDemoUid(adminRecord.uid)) {
        val item =
          Teacher(
            id = teacherId.trim(),
            fullName = fullName.trim(),
            email = email.trim(),
            facultyId = facId,
            departmentId = deptId,
            specialization = specialization.trim(),
            isActive = isActive,
          )
        demoTeachers = demoTeachers.filterNot { it.id == item.id } + item
        _actionBanner.value = false to "Department teacher '${item.fullName}' saved in $deptId."
        loadDepartmentAdminDashboard(adminRecord)
        return@launch
      }

      _isMutating.value = true
      repository.upsertTeacher(
        teacherId = teacherId,
        fullName = fullName,
        email = email,
        facultyId = facId,
        departmentId = deptId,
        specialization = specialization,
        isActive = isActive,
        actorRole = UserProfile.ROLE_DEPARTMENT_ADMIN,
        actorAssignedDepartmentId = deptId,
      )
        .onSuccess { teacher ->
          _isMutating.value = false
          _actionBanner.value =
            false to "Department teacher '${teacher.fullName}' saved in $deptId."
          loadDepartmentAdminDashboard(adminRecord)
        }
        .onFailure { error ->
          _isMutating.value = false
          _actionBanner.value =
            true to (error.localizedMessage ?: "Failed to save department teacher.")
        }
    }
  }

  fun saveCourseAsDepartmentAdmin(
    courseId: String,
    courseCode: String,
    courseName: String,
    teacherId: String,
    semesterId: String,
    academicYear: String,
    isActive: Boolean,
    adminRecord: AdminAccountRecord,
  ) {
    val deptId = adminRecord.departmentId.orEmpty()
    viewModelScope.launch {
      if (WcuDemoDataProvider.isDemoUid(adminRecord.uid)) {
        val item =
          Course(
            id = courseId.trim(),
            courseCode = courseCode.trim().uppercase(),
            courseName = courseName.trim(),
            teacherId = teacherId.trim(),
            departmentId = deptId,
            semesterId = semesterId.trim(),
            academicYear = academicYear.trim(),
            isActive = isActive,
          )
        demoCourses = demoCourses.filterNot { it.id == item.id } + item
        _actionBanner.value = false to "Department course '${item.courseCode}' saved in $deptId."
        loadDepartmentAdminDashboard(adminRecord)
        return@launch
      }

      _isMutating.value = true
      repository.upsertCourse(
        courseId = courseId,
        courseCode = courseCode,
        courseName = courseName,
        teacherId = teacherId,
        departmentId = deptId,
        semesterId = semesterId,
        academicYear = academicYear,
        isActive = isActive,
        actorRole = UserProfile.ROLE_DEPARTMENT_ADMIN,
        actorAssignedDepartmentId = deptId,
      )
        .onSuccess { course ->
          _isMutating.value = false
          _actionBanner.value =
            false to "Department course '${course.courseCode}' saved in $deptId."
          loadDepartmentAdminDashboard(adminRecord)
        }
        .onFailure { error ->
          _isMutating.value = false
          _actionBanner.value =
            true to (error.localizedMessage ?: "Failed to save department course.")
        }
    }
  }

  fun saveTeacherAsSuperAdmin(
    teacherId: String,
    fullName: String,
    email: String,
    facultyId: String,
    departmentId: String,
    specialization: String,
    isActive: Boolean,
    adminProfile: UserProfile,
  ) {
    viewModelScope.launch {
      if (WcuDemoDataProvider.isDemoUid(adminProfile.uid)) {
        val item =
          Teacher(
            id = teacherId.trim(),
            fullName = fullName.trim(),
            email = email.trim(),
            facultyId = facultyId.trim(),
            departmentId = departmentId.trim(),
            specialization = specialization.trim(),
            isActive = isActive,
          )
        demoTeachers = demoTeachers.filterNot { it.id == item.id } + item
        _actionBanner.value = false to "University teacher '${item.fullName}' saved in ${item.departmentId}."
        loadSuperAdminDashboard(adminProfile)
        return@launch
      }

      _isMutating.value = true
      repository.upsertTeacher(
        teacherId = teacherId,
        fullName = fullName,
        email = email,
        facultyId = facultyId,
        departmentId = departmentId,
        specialization = specialization,
        isActive = isActive,
        actorRole = UserProfile.ROLE_SUPER_ADMIN,
        actorAssignedDepartmentId = "",
      )
        .onSuccess { teacher ->
          _isMutating.value = false
          _actionBanner.value =
            false to "University teacher '${teacher.fullName}' saved in ${teacher.departmentId}."
          loadSuperAdminDashboard(adminProfile)
        }
        .onFailure { error ->
          _isMutating.value = false
          _actionBanner.value =
            true to (error.localizedMessage ?: "Failed to save teacher.")
        }
    }
  }

  fun saveCourseAsSuperAdmin(
    courseId: String,
    courseCode: String,
    courseName: String,
    teacherId: String,
    departmentId: String,
    semesterId: String,
    academicYear: String,
    isActive: Boolean,
    adminProfile: UserProfile,
  ) {
    viewModelScope.launch {
      if (WcuDemoDataProvider.isDemoUid(adminProfile.uid)) {
        val item =
          Course(
            id = courseId.trim(),
            courseCode = courseCode.trim().uppercase(),
            courseName = courseName.trim(),
            teacherId = teacherId.trim(),
            departmentId = departmentId.trim(),
            semesterId = semesterId.trim(),
            academicYear = academicYear.trim(),
            isActive = isActive,
          )
        demoCourses = demoCourses.filterNot { it.id == item.id } + item
        _actionBanner.value = false to "University course '${item.courseCode}' saved in ${item.departmentId}."
        loadSuperAdminDashboard(adminProfile)
        return@launch
      }

      _isMutating.value = true
      repository.upsertCourse(
        courseId = courseId,
        courseCode = courseCode,
        courseName = courseName,
        teacherId = teacherId,
        departmentId = departmentId,
        semesterId = semesterId,
        academicYear = academicYear,
        isActive = isActive,
        actorRole = UserProfile.ROLE_SUPER_ADMIN,
        actorAssignedDepartmentId = "",
      )
        .onSuccess { course ->
          _isMutating.value = false
          _actionBanner.value =
            false to "University course '${course.courseCode}' saved in ${course.departmentId}."
          loadSuperAdminDashboard(adminProfile)
        }
        .onFailure { error ->
          _isMutating.value = false
          _actionBanner.value =
            true to (error.localizedMessage ?: "Failed to save course.")
        }
    }
  }

  fun saveSemester(
    semesterId: String,
    name: String,
    academicYear: String,
    isActive: Boolean,
    isEvaluationOpen: Boolean,
    adminProfile: UserProfile,
  ) {
    viewModelScope.launch {
      if (WcuDemoDataProvider.isDemoUid(adminProfile.uid)) {
        _actionBanner.value =
          false to "Semester '${name.trim()}' (${academicYear.trim()}) updated."
        loadSuperAdminDashboard(adminProfile)
        return@launch
      }

      _isMutating.value = true
      repository.upsertSemester(
        semesterId = semesterId,
        name = name,
        academicYear = academicYear,
        isActive = isActive,
        isEvaluationOpen = isEvaluationOpen,
      )
        .onSuccess { semester ->
          _isMutating.value = false
          _actionBanner.value =
            false to "Semester '${semester.name}' (${semester.academicYear}) saved to Firestore."
          loadSuperAdminDashboard(adminProfile)
        }
        .onFailure { error ->
          _isMutating.value = false
          _actionBanner.value =
            true to (error.localizedMessage ?: "Failed to save semester.")
        }
    }
  }

  /**
   * Generates a Department-scoped PDF or Excel (.xlsx) evaluation report for a Department Admin.
   *
   * Strictly enforces:
   * - Active `department_admin` status
   * - Assigned `facultyId` and `departmentId` scope
   * - Audit logging in `/audit_logs/{logId}`
   */
  fun exportDepartmentAdminReport(
    outputDir: File,
    format: ExportFormat,
    filter: EvaluationReportExportFilter,
    adminRecord: AdminAccountRecord,
  ) {
    viewModelScope.launch {
      if (!adminRecord.isActive || !adminRecord.isDepartmentAdmin) {
        _actionBanner.value =
          true to "Export denied: Only an active Department Admin with a valid department assignment can export department reports."
        return@launch
      }

      val assignedDeptId = adminRecord.departmentId.orEmpty()
      val assignedFacId = adminRecord.facultyId.orEmpty()
      if (!filter.departmentId.isNullOrBlank() && filter.departmentId != assignedDeptId) {
        _actionBanner.value =
          true to "Export denied: Department Admin ($assignedDeptId) cannot export another department's data (${filter.departmentId})."
        return@launch
      }

      _isExporting.value = true
      _actionBanner.value = null

      val enforcedFilter =
        filter.copy(
          facultyId = assignedFacId,
          departmentId = assignedDeptId,
        )

      if (WcuDemoDataProvider.isDemoUid(adminRecord.uid)) {
        runCatching {
          val dataset =
            AdminManagementRepository.buildExportDatasetFromRecords(
              actorUid = adminRecord.uid,
              actorName = adminRecord.fullName,
              actorRole = UserProfile.ROLE_DEPARTMENT_ADMIN,
              assignedFacultyId = assignedFacId,
              assignedDepartmentId = assignedDeptId,
              filter = enforcedFilter,
              faculties = demoFaculties,
              departments = demoDepartments,
              semesters = WcuDemoDataProvider.demoSemesters,
              teachers = demoTeachers,
              courses = demoCourses,
              students = WcuDemoDataProvider.demoStudents,
              evaluations = WcuDemoDataProvider.demoEvaluations,
            )
          val generated =
            when (format) {
              ExportFormat.PDF ->
                EvaluationReportExporter.generatePdfReport(outputDir, dataset).getOrThrow()
              ExportFormat.XLSX ->
                EvaluationReportExporter.generateExcelReport(outputDir, dataset).getOrThrow()
            }
          val newLog =
            AdminAuditLog(
              id = "log_demo_export_${System.currentTimeMillis()}",
              actorUid = adminRecord.uid,
              actorRole = UserProfile.ROLE_DEPARTMENT_ADMIN,
              assignedDepartmentId = assignedDeptId,
              action = AdminManagementRepository.ACTION_EXPORT_EVALUATION_REPORT,
              targetCollection = AdminManagementRepository.COLLECTION_EVALUATIONS,
              targetDocumentId = dataset.reportScope,
              exportFormat = format.code,
              reportScope = dataset.reportScope,
              selectedFilters = enforcedFilter.toAuditSummaryString(),
              exportedRecordCount = dataset.totalEvaluationsCount,
              details =
                "Exported ${format.code} report (${generated.fileName}) • ${dataset.totalEvaluationsCount} records • Filters: ${enforcedFilter.toAuditSummaryString()}",
            )
          demoAuditLogs = listOf(newLog) + demoAuditLogs
          generated
        }
          .onSuccess { reportFile ->
            _isExporting.value = false
            _lastExportedReport.value = reportFile
            _actionBanner.value =
              false to "${format.displayName} generated (${reportFile.fileName} • ${reportFile.exportedRecordCount} records)."
            loadDepartmentAdminDashboard(adminRecord)
          }
          .onFailure { err ->
            _isExporting.value = false
            _actionBanner.value =
              true to (err.localizedMessage ?: "Failed to generate ${format.displayName}.")
          }
        return@launch
      }

      repository.prepareAuthorizedExportDataset(enforcedFilter)
        .mapCatching { dataset ->
          val generated =
            when (format) {
              ExportFormat.PDF ->
                EvaluationReportExporter.generatePdfReport(outputDir, dataset).getOrThrow()
              ExportFormat.XLSX ->
                EvaluationReportExporter.generateExcelReport(outputDir, dataset).getOrThrow()
            }
          repository.recordExportAuditLog(
            actorRole = UserProfile.ROLE_DEPARTMENT_ADMIN,
            assignedDepartmentId = assignedDeptId,
            exportFormat = format,
            reportScope = dataset.reportScope,
            filter = enforcedFilter,
            exportedRecordCount = dataset.totalEvaluationsCount,
          )
          generated
        }
        .onSuccess { reportFile ->
          _isExporting.value = false
          _lastExportedReport.value = reportFile
          _actionBanner.value =
            false to "${format.displayName} generated (${reportFile.fileName} • ${reportFile.exportedRecordCount} records) and logged to /audit_logs."
          loadDepartmentAdminDashboard(adminRecord)
        }
        .onFailure { error ->
          _isExporting.value = false
          _actionBanner.value =
            true to (error.localizedMessage ?: "Failed to export department evaluation report.")
        }
    }
  }

  /**
   * Generates a University-Wide or filtered PDF or Excel (.xlsx) evaluation report for a Super Admin.
   *
   * Strictly enforces:
   * - Active `super_admin` status
   * - Optional faculty, department, teacher, course, semester, and date filters
   * - Audit logging in `/audit_logs/{logId}`
   */
  fun exportSuperAdminReport(
    outputDir: File,
    format: ExportFormat,
    filter: EvaluationReportExportFilter,
    adminProfile: UserProfile,
  ) {
    viewModelScope.launch {
      val isSuperRole =
        adminProfile.role == UserProfile.ROLE_SUPER_ADMIN ||
          adminProfile.role == UserProfile.ROLE_ADMIN
      if (!adminProfile.isActive || !isSuperRole) {
        _actionBanner.value =
          true to "Export denied: Only an active Super Admin can export university-wide evaluation reports."
        return@launch
      }

      _isExporting.value = true
      _actionBanner.value = null

      if (WcuDemoDataProvider.isDemoUid(adminProfile.uid)) {
        runCatching {
          val dataset =
            AdminManagementRepository.buildExportDatasetFromRecords(
              actorUid = adminProfile.uid,
              actorName = adminProfile.fullName,
              actorRole = UserProfile.ROLE_SUPER_ADMIN,
              assignedFacultyId = null,
              assignedDepartmentId = null,
              filter = filter,
              faculties = demoFaculties,
              departments = demoDepartments,
              semesters = WcuDemoDataProvider.demoSemesters,
              teachers = demoTeachers,
              courses = demoCourses,
              students = WcuDemoDataProvider.demoStudents,
              evaluations = WcuDemoDataProvider.demoEvaluations,
            )
          val generated =
            when (format) {
              ExportFormat.PDF ->
                EvaluationReportExporter.generatePdfReport(outputDir, dataset).getOrThrow()
              ExportFormat.XLSX ->
                EvaluationReportExporter.generateExcelReport(outputDir, dataset).getOrThrow()
            }
          val newLog =
            AdminAuditLog(
              id = "log_demo_export_${System.currentTimeMillis()}",
              actorUid = adminProfile.uid,
              actorRole = UserProfile.ROLE_SUPER_ADMIN,
              assignedDepartmentId = filter.departmentId.orEmpty(),
              action = AdminManagementRepository.ACTION_EXPORT_EVALUATION_REPORT,
              targetCollection = AdminManagementRepository.COLLECTION_EVALUATIONS,
              targetDocumentId = dataset.reportScope,
              exportFormat = format.code,
              reportScope = dataset.reportScope,
              selectedFilters = filter.toAuditSummaryString(),
              exportedRecordCount = dataset.totalEvaluationsCount,
              details =
                "Exported ${format.code} report (${generated.fileName}) • ${dataset.totalEvaluationsCount} records • Filters: ${filter.toAuditSummaryString()}",
            )
          demoAuditLogs = listOf(newLog) + demoAuditLogs
          generated
        }
          .onSuccess { reportFile ->
            _isExporting.value = false
            _lastExportedReport.value = reportFile
            _actionBanner.value =
              false to "${format.displayName} generated (${reportFile.fileName} • ${reportFile.exportedRecordCount} records)."
            loadSuperAdminDashboard(adminProfile)
          }
          .onFailure { err ->
            _isExporting.value = false
            _actionBanner.value =
              true to (err.localizedMessage ?: "Failed to generate ${format.displayName}.")
          }
        return@launch
      }

      repository.prepareAuthorizedExportDataset(filter)
        .mapCatching { dataset ->
          val generated =
            when (format) {
              ExportFormat.PDF ->
                EvaluationReportExporter.generatePdfReport(outputDir, dataset).getOrThrow()
              ExportFormat.XLSX ->
                EvaluationReportExporter.generateExcelReport(outputDir, dataset).getOrThrow()
            }
          repository.recordExportAuditLog(
            actorRole = UserProfile.ROLE_SUPER_ADMIN,
            assignedDepartmentId = filter.departmentId.orEmpty(),
            exportFormat = format,
            reportScope = dataset.reportScope,
            filter = filter,
            exportedRecordCount = dataset.totalEvaluationsCount,
          )
          generated
        }
        .onSuccess { reportFile ->
          _isExporting.value = false
          _lastExportedReport.value = reportFile
          _actionBanner.value =
            false to "${format.displayName} generated (${reportFile.fileName} • ${reportFile.exportedRecordCount} records) and logged to /audit_logs."
          loadSuperAdminDashboard(adminProfile)
        }
        .onFailure { error ->
          _isExporting.value = false
          _actionBanner.value =
            true to (error.localizedMessage ?: "Failed to export university evaluation report.")
        }
    }
  }

  companion object {
    val Factory = viewModelFactory {
      initializer {
        val app =
          checkNotNull(this[APPLICATION_KEY]) {
            "APPLICATION_KEY missing from CreationExtras"
          }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        AdminManagementViewModel(AdminManagementRepository(db))
      }
    }
  }
}

