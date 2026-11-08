package com.example.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.repository.AcademicDirectoryRepository
import com.example.data.repository.AcademicDirectorySnapshot
import com.example.data.repository.TeacherProfileDetails
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class DirectorySubSection(val label: String) {
  FACULTIES("Faculties"),
  DEPARTMENTS("Departments"),
  TEACHERS("Teachers"),
  COURSES("Courses"),
}

sealed interface AcademicDirectoryUiState {
  data object Loading : AcademicDirectoryUiState
  data class Success(
    val snapshot: AcademicDirectorySnapshot,
    val isRefreshing: Boolean = false,
  ) : AcademicDirectoryUiState
  data class Error(
    val message: String,
  ) : AcademicDirectoryUiState
}

sealed interface TeacherDetailUiState {
  data object Idle : TeacherDetailUiState
  data object Loading : TeacherDetailUiState
  data class Loaded(val details: TeacherProfileDetails) : TeacherDetailUiState
  data class MissingOrInactive(val teacherId: String) : TeacherDetailUiState
  data class Error(val message: String) : TeacherDetailUiState
}

class AcademicDirectoryViewModel(
  private val studentProfile: UserProfile,
  val repository: AcademicDirectoryRepository,
) : ViewModel() {

  private val _uiState =
    MutableStateFlow<AcademicDirectoryUiState>(AcademicDirectoryUiState.Loading)
  val uiState: StateFlow<AcademicDirectoryUiState> = _uiState.asStateFlow()

  private val _selectedSection =
    MutableStateFlow(DirectorySubSection.TEACHERS)
  val selectedSection: StateFlow<DirectorySubSection> = _selectedSection.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedFacultyId = MutableStateFlow<String?>(null)
  val selectedFacultyId: StateFlow<String?> = _selectedFacultyId.asStateFlow()

  private val _selectedDepartmentId = MutableStateFlow<String?>(null)
  val selectedDepartmentId: StateFlow<String?> = _selectedDepartmentId.asStateFlow()

  private val _teacherDetailState =
    MutableStateFlow<TeacherDetailUiState>(TeacherDetailUiState.Idle)
  val teacherDetailState: StateFlow<TeacherDetailUiState> = _teacherDetailState.asStateFlow()

  init {
    refreshDirectory()
  }

  fun selectSection(section: DirectorySubSection) {
    _selectedSection.value = section
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun selectFacultyFilter(facultyId: String?) {
    _selectedFacultyId.value = facultyId
    if (facultyId == null) {
      _selectedDepartmentId.value = null
    } else {
      val currentSnapshot = (_uiState.value as? AcademicDirectoryUiState.Success)?.snapshot
      val currentDept = _selectedDepartmentId.value
      if (currentDept != null && currentSnapshot != null) {
        val belongsToFaculty =
          currentSnapshot.departments.any { it.id == currentDept && it.facultyId == facultyId }
        if (!belongsToFaculty) {
          _selectedDepartmentId.value = null
        }
      }
    }
  }

  fun selectDepartmentFilter(departmentId: String?) {
    _selectedDepartmentId.value = departmentId
    if (departmentId != null) {
      val currentSnapshot = (_uiState.value as? AcademicDirectoryUiState.Success)?.snapshot
      val dept = currentSnapshot?.departments?.firstOrNull { it.id == departmentId }
      if (dept != null && dept.facultyId.isNotBlank()) {
        _selectedFacultyId.value = dept.facultyId
      }
    }
  }

  fun clearAllFilters() {
    _searchQuery.value = ""
    _selectedFacultyId.value = null
    _selectedDepartmentId.value = null
  }

  fun filterToStudentOwnDepartment() {
    _selectedFacultyId.value = studentProfile.facultyId.takeIf { it.isNotBlank() }
    _selectedDepartmentId.value = studentProfile.departmentId.takeIf { it.isNotBlank() }
  }

  fun refreshDirectory() {
    viewModelScope.launch {
      if (com.example.data.model.WcuDemoDataProvider.isDemoUid(studentProfile.uid)) {
        _uiState.value =
          AcademicDirectoryUiState.Success(
            com.example.data.model.WcuDemoDataProvider.buildAcademicDirectorySnapshot(),
            isRefreshing = false,
          )
        return@launch
      }

      val current = _uiState.value
      if (current is AcademicDirectoryUiState.Success) {
        _uiState.value = current.copy(isRefreshing = true)
      } else {
        _uiState.value = AcademicDirectoryUiState.Loading
      }

      repository
        .loadAcademicDirectorySnapshot(studentUid = studentProfile.uid)
        .onSuccess { snapshot ->
          _uiState.value = AcademicDirectoryUiState.Success(snapshot, isRefreshing = false)
        }
        .onFailure { error ->
          _uiState.value =
            AcademicDirectoryUiState.Error(
              error.localizedMessage ?: "Failed to load academic directory from Cloud Firestore."
            )
        }
    }
  }

  fun openTeacherDetails(teacherId: String) {
    viewModelScope.launch {
      if (com.example.data.model.WcuDemoDataProvider.isDemoUid(studentProfile.uid)) {
        val details =
          com.example.data.model.WcuDemoDataProvider.buildTeacherProfileDetails(
            teacherId = teacherId,
            studentProfile = studentProfile,
          )
        _teacherDetailState.value =
          if (details == null) TeacherDetailUiState.MissingOrInactive(teacherId)
          else TeacherDetailUiState.Loaded(details)
        return@launch
      }

      _teacherDetailState.value = TeacherDetailUiState.Loading
      repository
        .loadTeacherProfileDetails(teacherId = teacherId, studentProfile = studentProfile)
        .onSuccess { details ->
          if (details == null) {
            _teacherDetailState.value = TeacherDetailUiState.MissingOrInactive(teacherId)
          } else {
            _teacherDetailState.value = TeacherDetailUiState.Loaded(details)
          }
        }
        .onFailure { error ->
          _teacherDetailState.value =
            TeacherDetailUiState.Error(
              error.localizedMessage ?: "Failed to load teacher profile details."
            )
        }
    }
  }

  fun closeTeacherDetails() {
    _teacherDetailState.value = TeacherDetailUiState.Idle
  }

  companion object {
    fun provideFactory(studentProfile: UserProfile): ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val app =
          checkNotNull(this[APPLICATION_KEY]) {
            "APPLICATION_KEY missing from CreationExtras"
          }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        AcademicDirectoryViewModel(
          studentProfile = studentProfile,
          repository = AcademicDirectoryRepository(db),
        )
      }
    }
  }
}
