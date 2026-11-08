package com.example.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.StudentDashboardSummary
import com.example.data.model.UserProfile
import com.example.data.repository.StudentDashboardRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface StudentDashboardUiState {
  data object Loading : StudentDashboardUiState
  data class Success(
    val summary: StudentDashboardSummary,
    val isRefreshing: Boolean = false,
  ) : StudentDashboardUiState
  data class Error(
    val message: String,
    val fallbackProfile: UserProfile,
  ) : StudentDashboardUiState
}

class StudentDashboardViewModel(
  private val initialProfile: UserProfile,
  private val repository: StudentDashboardRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow<StudentDashboardUiState>(StudentDashboardUiState.Loading)
  val uiState: StateFlow<StudentDashboardUiState> = _uiState.asStateFlow()

  private var observeJob: Job? = null

  init {
    startObservingDashboard()
  }

  private fun startObservingDashboard() {
    observeJob?.cancel()
    observeJob =
      viewModelScope.launch {
        if (com.example.data.model.WcuDemoDataProvider.isDemoUid(initialProfile.uid)) {
          _uiState.value =
            StudentDashboardUiState.Success(
              com.example.data.model.WcuDemoDataProvider.buildStudentDashboardSummary(initialProfile)
            )
          return@launch
        }

        _uiState.value = StudentDashboardUiState.Loading
        // First perform a full one-shot fetch to resolve cross-collection references cleanly
        repository
          .loadStudentDashboard(initialProfile.uid)
          .onSuccess { initialSummary ->
            _uiState.value = StudentDashboardUiState.Success(initialSummary)
          }
          .onFailure { error ->
            _uiState.value =
              StudentDashboardUiState.Error(
                message =
                  error.localizedMessage
                    ?: "Unable to load Student Dashboard from Cloud Firestore.",
                fallbackProfile = initialProfile,
              )
          }

        // Then subscribe to real-time updates on user, teachers, courses, and evaluations
        repository
          .observeStudentDashboard(initialProfile)
          .catch { error ->
            val current = _uiState.value
            if (current !is StudentDashboardUiState.Success) {
              _uiState.value =
                StudentDashboardUiState.Error(
                  message =
                    error.localizedMessage
                      ?: "Real-time Firestore synchronization encountered an error.",
                  fallbackProfile = initialProfile,
                )
            }
          }
          .collect { liveSummary ->
            _uiState.value = StudentDashboardUiState.Success(liveSummary, isRefreshing = false)
          }
      }
  }

  fun refreshDashboard() {
    viewModelScope.launch {
      if (com.example.data.model.WcuDemoDataProvider.isDemoUid(initialProfile.uid)) {
        _uiState.value =
          StudentDashboardUiState.Success(
            com.example.data.model.WcuDemoDataProvider.buildStudentDashboardSummary(initialProfile),
            isRefreshing = false,
          )
        return@launch
      }

      val current = _uiState.value
      if (current is StudentDashboardUiState.Success) {
        _uiState.value = current.copy(isRefreshing = true)
      } else {
        _uiState.value = StudentDashboardUiState.Loading
      }

      repository
        .loadStudentDashboard(initialProfile.uid)
        .onSuccess { refreshedSummary ->
          _uiState.value = StudentDashboardUiState.Success(refreshedSummary, isRefreshing = false)
        }
        .onFailure { error ->
          _uiState.value =
            StudentDashboardUiState.Error(
              message =
                error.localizedMessage ?: "Failed to refresh dashboard data from Cloud Firestore.",
              fallbackProfile = initialProfile,
            )
        }
    }
  }

  private val _isSubmittingEvaluation = MutableStateFlow(false)
  val isSubmittingEvaluation: StateFlow<Boolean> = _isSubmittingEvaluation.asStateFlow()

  private val _submissionFeedback = MutableStateFlow<Pair<Boolean, String>?>(null)
  val submissionFeedback: StateFlow<Pair<Boolean, String>?> = _submissionFeedback.asStateFlow()

  fun clearSubmissionFeedback() {
    _submissionFeedback.value = null
  }

  fun submitCourseEvaluation(
    teacherId: String,
    courseId: String,
    questionRatings: Map<String, Int>,
    comment: String,
    onSubmittedSuccess: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isSubmittingEvaluation.value = true
      _submissionFeedback.value = null

      val result =
        if (com.example.data.model.WcuDemoDataProvider.isDemoUid(initialProfile.uid)) {
          com.example.data.model.WcuDemoDataProvider.submitDemoStudentEvaluation(
            studentProfile = initialProfile,
            teacherId = teacherId,
            courseId = courseId,
            questionRatings = questionRatings,
            comment = comment,
          )
        } else {
          repository.submitTeacherEvaluation(
            studentUid = initialProfile.uid,
            teacherId = teacherId,
            courseId = courseId,
            questionRatings = questionRatings,
            comment = comment,
          )
        }

      _isSubmittingEvaluation.value = false
      result
        .onSuccess { evaluation ->
          _submissionFeedback.value =
            true to
              "Evaluation submitted (${String.format(java.util.Locale.US, "%.2f", evaluation.overallScore)} / 5.00)."
          refreshDashboard()
          onSubmittedSuccess()
        }
        .onFailure { err ->
          _submissionFeedback.value =
            false to (err.localizedMessage ?: "Failed to submit teacher evaluation.")
        }
    }
  }

  companion object {
    fun provideFactory(profile: UserProfile): ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val app =
          checkNotNull(this[APPLICATION_KEY]) {
            "APPLICATION_KEY missing from CreationExtras"
          }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        StudentDashboardViewModel(
          initialProfile = profile,
          repository = StudentDashboardRepository(db),
        )
      }
    }
  }
}
