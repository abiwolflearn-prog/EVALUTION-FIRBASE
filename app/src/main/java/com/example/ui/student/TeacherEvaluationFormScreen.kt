package com.example.ui.student

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserProfile
import com.example.data.model.WcuEvaluationRubricCategories
import com.example.ui.theme.WcuBlue100
import com.example.ui.theme.WcuGold500
import com.example.ui.theme.WcuNavy700
import com.example.ui.theme.WcuNavy900
import com.example.ui.theme.WcuSuccessGreen
import com.example.ui.theme.WcuWhite
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TeacherEvaluationFormScreen(
  studentProfile: UserProfile,
  courseId: String,
  teacherId: String,
  dashboardViewModel: StudentDashboardViewModel,
  onBackToDashboard: () -> Unit,
) {
  BackHandler(onBack = onBackToDashboard)

  val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
  val isSubmitting by dashboardViewModel.isSubmittingEvaluation.collectAsStateWithLifecycle()
  val submissionFeedback by dashboardViewModel.submissionFeedback.collectAsStateWithLifecycle()

  val questionRatings = remember { mutableStateMapOf<String, Int>() }
  var comment by rememberSaveable { mutableStateOf("") }
  var localValidationMessage by remember { mutableStateOf<String?>(null) }
  var submittedSuccessfully by rememberSaveable { mutableStateOf(false) }

  val summary = (dashboardState as? StudentDashboardUiState.Success)?.summary
  val matchedPending =
    summary?.pendingEvaluations?.firstOrNull {
      it.course.id == courseId && it.teacher.id == teacherId
    }
  val alreadyEvaluatedItem =
    summary?.recentEvaluationActivity?.firstOrNull {
      it.courseId == courseId && it.teacherId == teacherId
    }

  val courseTitle =
    matchedPending?.let { "${it.course.courseCode} • ${it.course.courseName}" }
      ?: alreadyEvaluatedItem?.let { "${it.courseCode} • ${it.courseName}" }
      ?: "Course ID: $courseId"
  val instructorName =
    matchedPending?.teacher?.fullName
      ?: summary?.availableTeachers?.firstOrNull { it.id == teacherId }?.fullName
      ?: alreadyEvaluatedItem?.teacherName
      ?: "Instructor ($teacherId)"

  val ratedCount = questionRatings.size
  val totalQuestions = WcuEvaluationRubricCategories.ALL_QUESTIONS.size
  val liveAverage =
    if (questionRatings.isNotEmpty()) questionRatings.values.average() else 0.0

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .testTag("teacher_evaluation_section"),
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(
            onClick = {
              dashboardViewModel.clearSubmissionFeedback()
              onBackToDashboard()
            },
            modifier = Modifier.testTag("evaluation_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Student Dashboard",
              tint = WcuWhite,
            )
          }
        },
        title = {
          Column {
            Text(
              text = "WCU Instructor Evaluation Form",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = WcuWhite,
            )
            Text(
              text = "$instructorName • $courseId",
              style = MaterialTheme.typography.labelSmall,
              color = WcuBlue100,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = WcuNavy900),
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 640.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Course & Instructor Context Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = WcuNavy900),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = courseTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = WcuWhite,
            )
            Text(
              text = "Instructor: $instructorName",
              style = MaterialTheme.typography.bodyMedium,
              color = WcuBlue100,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = "Student: ${studentProfile.fullName} (${studentProfile.studentId}) • Dept: ${studentProfile.departmentId}",
              style = MaterialTheme.typography.labelSmall,
              color = WcuWhite.copy(alpha = 0.85f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
              progress = { (ratedCount.toFloat() / totalQuestions.toFloat()).coerceIn(0f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .testTag("evaluation_form_progress_bar"),
              color = WcuGold500,
              trackColor = WcuWhite.copy(alpha = 0.22f),
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Rated $ratedCount / $totalQuestions criteria across 6 categories",
                style = MaterialTheme.typography.labelSmall,
                color = WcuBlue100,
              )
              if (ratedCount > 0) {
                Text(
                  text = String.format(Locale.US, "Live Avg: %.2f / 5.00 ★", liveAverage),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = WcuGold500,
                )
              }
            }
          }
        }

        // Duplicate / Already Submitted Guard
        if (alreadyEvaluatedItem != null || submittedSuccessfully) {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("evaluation_already_submitted_card"),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, WcuSuccessGreen.copy(alpha = 0.5f)),
            colors =
              CardDefaults.cardColors(
                containerColor = WcuSuccessGreen.copy(alpha = 0.10f),
              ),
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.CheckCircle,
                  contentDescription = "Evaluation Submitted",
                  tint = WcuSuccessGreen,
                )
                Text(
                  text = "Evaluation Completed & Locked",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = WcuSuccessGreen,
                )
              }
              Text(
                text =
                  alreadyEvaluatedItem?.let {
                    "You have already submitted an official evaluation for $courseTitle (${String.format(Locale.US, "%.2f / 5.00 ★", it.overallScore)}). Duplicate submissions are prevented."
                  }
                    ?: "Thank you! Your confidential 6-category evaluation has been recorded in Cloud Firestore.",
                style = MaterialTheme.typography.bodySmall,
              )
              OutlinedButton(
                onClick = {
                  dashboardViewModel.clearSubmissionFeedback()
                  onBackToDashboard()
                },
                modifier = Modifier.testTag("return_to_dashboard_button"),
              ) {
                Text("Return to Student Dashboard")
              }
            }
          }
          return@Column
        }

        // Quick-Fill Helper Bar for Accessibility & Fast Evaluation
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            ),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "5-Star Rating Scale (1 = Poor, 2 = Fair, 3 = Good, 4 = Very Good, 5 = Excellent)",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              (5 downTo 3).forEach { presetStars ->
                FilterChip(
                  selected = false,
                  onClick = {
                    WcuEvaluationRubricCategories.ALL_QUESTIONS.forEach { q ->
                      questionRatings[q.questionId] = presetStars
                    }
                    localValidationMessage = null
                  },
                  label = { Text("Set All to $presetStars ★") },
                  modifier = Modifier.testTag("quick_fill_${presetStars}_stars"),
                )
              }
            }
          }
        }

        // Render all 6 Categories and 18 Questions
        WcuEvaluationRubricCategories.CATEGORIES_WITH_QUESTIONS.forEachIndexed { catIdx, category ->
          val catAnswered =
            category.questions.mapNotNull { questionRatings[it.questionId] }
          val catAvg = if (catAnswered.isNotEmpty()) catAnswered.average() else null

          Card(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("eval_category_card_${category.categoryId}"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Category ${catIdx + 1}: ${category.title}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = WcuNavy700,
                  )
                  Text(
                    text = "${catAnswered.size} of ${category.questions.size} criteria rated",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                if (catAvg != null) {
                  Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = WcuGold500.copy(alpha = 0.16f),
                  ) {
                    Text(
                      text = String.format(Locale.US, "%.2f ★", catAvg),
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = WcuNavy900,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                  }
                }
              }

              HorizontalDivider()

              category.questions.forEach { question ->
                val currentRating = questionRatings[question.questionId] ?: 0
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("eval_question_row_${question.questionId}"),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = "${question.questionNumber}. ${question.prompt}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    (1..5).forEach { starValue ->
                      val isSelected = starValue <= currentRating
                      IconButton(
                        onClick = {
                          questionRatings[question.questionId] = starValue
                          localValidationMessage = null
                        },
                        modifier = Modifier
                          .size(48.dp)
                          .testTag("rate_${question.questionId}_star_$starValue"),
                      ) {
                        Icon(
                          imageVector =
                            if (isSelected) Icons.Filled.Star else Icons.Outlined.StarOutline,
                          contentDescription =
                            "Rate question ${question.questionNumber} $starValue out of 5 stars",
                          tint =
                            if (isSelected) WcuGold500
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        )
                      }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                      text = if (currentRating > 0) "$currentRating / 5" else "Unrated",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color =
                        if (currentRating > 0) WcuNavy700
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }

        // Written Comment Field
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "Constructive Feedback & Comments (Optional)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
              value = comment,
              onValueChange = { if (it.length <= 1000) comment = it },
              label = { Text("Share observations on teaching clarity, lab organization, or support") },
              minLines = 3,
              maxLines = 5,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("evaluation_comment_input"),
            )
          }
        }

        // Validation / Submission Feedback Banner
        val feedbackToDisplay =
          localValidationMessage?.let { false to it } ?: submissionFeedback
        if (feedbackToDisplay != null) {
          val (isSuccess, msg) = feedbackToDisplay
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("evaluation_feedback_banner"),
            shape = RoundedCornerShape(12.dp),
            color =
              if (isSuccess) WcuSuccessGreen.copy(alpha = 0.14f)
              else MaterialTheme.colorScheme.errorContainer,
          ) {
            Text(
              text = msg,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              color =
                if (isSuccess) WcuSuccessGreen
                else MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(12.dp),
            )
          }
        }

        // Submit Evaluation Button
        Button(
          onClick = {
            if (questionRatings.size < totalQuestions) {
              val missingCount = totalQuestions - questionRatings.size
              localValidationMessage =
                "Please rate all 18 evaluation questions before submitting ($missingCount question(s) remaining)."
              return@Button
            }
            localValidationMessage = null
            dashboardViewModel.submitCourseEvaluation(
              teacherId = teacherId,
              courseId = courseId,
              questionRatings = questionRatings.toMap(),
              comment = comment,
              onSubmittedSuccess = {
                submittedSuccessfully = true
              },
            )
          },
          enabled = !isSubmitting,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("submit_teacher_evaluation_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = WcuNavy700, contentColor = WcuWhite),
        ) {
          if (isSubmitting) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
              color = WcuWhite,
            )
            Spacer(modifier = Modifier.size(10.dp))
            Text("Submitting Confidential Evaluation…")
          } else {
            Text(
              text = "Submit Official Evaluation ($ratedCount/$totalQuestions)",
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}
