package com.example.ui.student

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.FeedbackBannerCard
import com.example.R
import com.example.data.model.PendingCourseEvaluationItem
import com.example.data.model.RecentEvaluationActivityItem
import com.example.data.model.StudentDashboardSummary
import com.example.data.model.Teacher
import com.example.data.model.UserProfile
import com.example.ui.theme.WcuBlue100
import com.example.ui.theme.WcuBlue600
import com.example.ui.theme.WcuGold500
import com.example.ui.theme.WcuNavy800
import com.example.ui.theme.WcuNavy900
import com.example.ui.theme.WcuWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class StudentPortalTab(val label: String) {
  HOME("Home"),
  TEACHERS("Teachers"),
  EVALUATIONS("Evaluations"),
  HISTORY("History"),
  PROFILE("Profile"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentPortalScreen(
  profile: UserProfile,
  emailVerified: Boolean,
  isDark: Boolean,
  onToggleDark: () -> Unit,
  feedbackBanner: Pair<Boolean, String>?,
  onClearBanner: () -> Unit,
  onSendVerification: () -> Unit,
  onSignOut: () -> Unit,
  onNavigateToTeacherEvaluation: (String, String) -> Unit = { _, _ -> },
  dashboardViewModel: StudentDashboardViewModel =
    viewModel(
      key = "student_dashboard_${profile.uid}",
      factory = StudentDashboardViewModel.provideFactory(profile),
    ),
) {
  val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
  var currentTab by rememberSaveable { mutableStateOf(StudentPortalTab.HOME) }
  var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

  if (currentTab != StudentPortalTab.HOME) {
    BackHandler { currentTab = StudentPortalTab.HOME }
  }

  if (showLogoutDialog) {
    AlertDialog(
      onDismissRequest = { showLogoutDialog = false },
      title = { Text("Confirm Sign Out") },
      text = {
        Text("Are you sure you want to sign out of your Wachemo University Student Portal session?")
      },
      confirmButton = {
        Button(
          onClick = {
            showLogoutDialog = false
            onSignOut()
          },
          modifier = Modifier.testTag("confirm_logout_button"),
        ) {
          Text("Sign Out")
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutDialog = false }) {
          Text("Cancel")
        }
      },
    )
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text =
                when (currentTab) {
                  StudentPortalTab.HOME -> "WCU Student Dashboard"
                  StudentPortalTab.TEACHERS -> "Department Teachers"
                  StudentPortalTab.EVALUATIONS -> "Pending Evaluations"
                  StudentPortalTab.HISTORY -> "Evaluation History"
                  StudentPortalTab.PROFILE -> "Student Profile"
                },
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = WcuWhite,
            )
            Text(
              text = "${profile.studentId} • ${profile.fullName}",
              style = MaterialTheme.typography.labelSmall,
              color = WcuBlue100,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        },
        actions = {
          IconButton(
            onClick = dashboardViewModel::refreshDashboard,
            modifier = Modifier.testTag("student_dashboard_refresh_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Refresh Firestore Data",
              tint = WcuWhite,
            )
          }
          IconButton(onClick = onToggleDark) {
            Icon(
              imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
              contentDescription = stringResource(R.string.switch_theme),
              tint = WcuWhite,
            )
          }
          IconButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.testTag("student_logout_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Sign Out",
              tint = WcuWhite,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = WcuNavy900),
      )
    },
    bottomBar = {
      NavigationBar {
        NavigationBarItem(
          selected = currentTab == StudentPortalTab.HOME,
          onClick = { currentTab = StudentPortalTab.HOME },
          icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
          label = { Text("Home") },
          modifier = Modifier.testTag("student_tab_home"),
        )
        NavigationBarItem(
          selected = currentTab == StudentPortalTab.TEACHERS,
          onClick = { currentTab = StudentPortalTab.TEACHERS },
          icon = { Icon(Icons.Default.Groups, contentDescription = "Teachers") },
          label = { Text("Teachers") },
          modifier = Modifier.testTag("student_tab_teachers"),
        )
        NavigationBarItem(
          selected = currentTab == StudentPortalTab.EVALUATIONS,
          onClick = { currentTab = StudentPortalTab.EVALUATIONS },
          icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Evaluations") },
          label = { Text("Evaluations") },
          modifier = Modifier.testTag("student_tab_evaluations"),
        )
        NavigationBarItem(
          selected = currentTab == StudentPortalTab.HISTORY,
          onClick = { currentTab = StudentPortalTab.HISTORY },
          icon = { Icon(Icons.Default.History, contentDescription = "History") },
          label = { Text("History") },
          modifier = Modifier.testTag("student_tab_history"),
        )
        NavigationBarItem(
          selected = currentTab == StudentPortalTab.PROFILE,
          onClick = { currentTab = StudentPortalTab.PROFILE },
          icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") },
          label = { Text("Profile") },
          modifier = Modifier.testTag("student_tab_profile"),
        )
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      when (val state = uiState) {
        StudentDashboardUiState.Loading -> {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(32.dp)
              .testTag("student_dashboard_loading"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "Loading student academic dashboard from Cloud Firestore...",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        is StudentDashboardUiState.Error -> {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .widthIn(max = 600.dp)
              .padding(24.dp)
              .testTag("student_dashboard_error"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = "Unable to Load Firestore Dashboard",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.error,
            )
            Text(
              text = state.message,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
              onClick = dashboardViewModel::refreshDashboard,
              modifier = Modifier.testTag("student_dashboard_retry_button"),
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Retry Loading from Firestore")
            }
          }
        }

        is StudentDashboardUiState.Success -> {
          val summary = state.summary
          when (currentTab) {
            StudentPortalTab.HOME -> {
              StudentDashboardHomeContent(
                summary = summary,
                emailVerified = emailVerified,
                feedbackBanner = feedbackBanner,
                onClearBanner = onClearBanner,
                onSendVerification = onSendVerification,
                onOpenTeacherDirectory = { currentTab = StudentPortalTab.TEACHERS },
                onOpenPendingEvaluations = { currentTab = StudentPortalTab.EVALUATIONS },
                onOpenEvaluationHistory = { currentTab = StudentPortalTab.HISTORY },
                onStartCourseEvaluation = onNavigateToTeacherEvaluation,
              )
            }

            StudentPortalTab.TEACHERS -> {
              AcademicDirectoryScreen(
                studentProfile = summary.profile,
                onBackHome = { currentTab = StudentPortalTab.HOME },
                onStartCourseEvaluation = onNavigateToTeacherEvaluation,
              )
            }

            StudentPortalTab.EVALUATIONS -> {
              StudentPendingEvaluationsTabContent(
                summary = summary,
                onBackHome = { currentTab = StudentPortalTab.HOME },
                onStartCourseEvaluation = onNavigateToTeacherEvaluation,
              )
            }

            StudentPortalTab.HISTORY -> {
              StudentEvaluationHistoryTabContent(
                summary = summary,
                onBackHome = { currentTab = StudentPortalTab.HOME },
              )
            }

            StudentPortalTab.PROFILE -> {
              StudentProfileTabContent(
                summary = summary,
                emailVerified = emailVerified,
                onSendVerification = onSendVerification,
                onRequestLogout = { showLogoutDialog = true },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun StudentDashboardHomeContent(
  summary: StudentDashboardSummary,
  emailVerified: Boolean,
  feedbackBanner: Pair<Boolean, String>?,
  onClearBanner: () -> Unit,
  onSendVerification: () -> Unit,
  onOpenTeacherDirectory: () -> Unit,
  onOpenPendingEvaluations: () -> Unit,
  onOpenEvaluationHistory: () -> Unit,
  onStartCourseEvaluation: (String, String) -> Unit = { _, _ -> },
) {
  val profile = summary.profile
  val activeSemesterLabel =
    summary.activeSemester?.let { "${it.name} • ${it.academicYear}" }
      ?: "2026/27 Academic Year • Semester I"

  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 640.dp)
      .padding(horizontal = 16.dp)
      .testTag("student_dashboard_home_list"),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item { Spacer(modifier = Modifier.height(4.dp)) }

    if (feedbackBanner != null) {
      item {
        FeedbackBannerCard(
          isError = feedbackBanner.first,
          message = feedbackBanner.second,
          onDismiss = onClearBanner,
        )
      }
    }

    // 1. Personalized Welcome Hero Banner & Student Identity
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("dashboard_welcome_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WcuNavy900),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(WcuNavy800)
                  .border(1.dp, WcuGold500.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.School,
                  contentDescription = null,
                  tint = WcuGold500,
                )
              }
              Column {
                Text(
                  text = "Welcome back, ${profile.fullName}",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = WcuWhite,
                )
                Text(
                  text = activeSemesterLabel,
                  style = MaterialTheme.typography.labelSmall,
                  color = WcuGold500,
                )
              }
            }
          }

          HorizontalDivider(color = WcuNavy800)

          // Student ID, Faculty, and Department from Firestore
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "Student ID",
                style = MaterialTheme.typography.labelSmall,
                color = WcuBlue100,
              )
              Text(
                text = profile.studentId,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = WcuGold500,
                modifier = Modifier.testTag("dashboard_student_id_text"),
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "Faculty / College",
                style = MaterialTheme.typography.labelSmall,
                color = WcuBlue100,
              )
              Text(
                text = summary.facultyDisplayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = WcuWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("dashboard_faculty_text"),
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "Department",
                style = MaterialTheme.typography.labelSmall,
                color = WcuBlue100,
              )
              Text(
                text = "${summary.departmentDisplayName} (Year ${profile.yearOfStudy})",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = WcuWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("dashboard_department_text"),
              )
            }
          }
        }
      }
    }

    // 2. Live Firestore KPI Summary Row: Total Teachers, Pending Evaluations, Completed Evaluations
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        DashboardKpiMetricCard(
          modifier = Modifier
            .weight(1f)
            .testTag("dashboard_total_teachers_card"),
          title = "Available Teachers",
          value = summary.totalAvailableTeachers.toString(),
          subtitle = "Active in Dept",
          accentColor = WcuBlue600,
        )
        DashboardKpiMetricCard(
          modifier = Modifier
            .weight(1f)
            .testTag("dashboard_pending_evaluations_card"),
          title = "Pending",
          value = summary.pendingEvaluationsCount.toString(),
          subtitle = "Awaiting Review",
          accentColor = WcuGold500,
        )
        DashboardKpiMetricCard(
          modifier = Modifier
            .weight(1f)
            .testTag("dashboard_completed_evaluations_card"),
          title = "Completed",
          value = summary.completedEvaluationsCount.toString(),
          subtitle = "Submitted",
          accentColor = MaterialTheme.colorScheme.primary,
        )
      }
    }

    // 3. Evaluation Progress Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("dashboard_evaluation_progress_card"),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = "Semester Evaluation Progress",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              val totalTarget =
                summary.completedEvaluationsCount + summary.pendingEvaluationsCount
              Text(
                text =
                  if (totalTarget > 0) {
                    "${summary.completedEvaluationsCount} of $totalTarget assigned course evaluations completed"
                  } else {
                    "No pending course evaluations assigned in Firestore yet"
                  },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text = "${summary.evaluationProgressPercent}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                  .padding(horizontal = 12.dp, vertical = 4.dp)
                  .testTag("dashboard_progress_percent_text"),
              )
            }
          }

          LinearProgressIndicator(
            progress = { summary.evaluationProgressFraction },
            modifier = Modifier
              .fillMaxWidth()
              .height(10.dp)
              .clip(RoundedCornerShape(999.dp)),
          )
        }
      }
    }

    // 4. Quick Access Actions: Teacher Directory & Pending Evaluations
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Quick Access",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = onOpenTeacherDirectory,
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("quick_access_teacher_directory"),
              shape = RoundedCornerShape(12.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Teacher Directory",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Button(
              onClick = onOpenPendingEvaluations,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = WcuNavy800,
                  contentColor = WcuWhite,
                ),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("quick_access_pending_evaluations"),
              shape = RoundedCornerShape(12.dp),
            ) {
              Icon(
                imageVector = Icons.Default.PendingActions,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Pending (${summary.pendingEvaluationsCount})",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }

    // 5. Pending Evaluations Preview (if any exist in Firestore)
    if (summary.pendingEvaluations.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Pending Course Evaluations",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          TextButton(onClick = onOpenPendingEvaluations) {
            Text("View All (${summary.pendingEvaluations.size})")
          }
        }
      }

      items(
        items = summary.pendingEvaluations.take(3),
        key = { "pending_${it.course.id}_${it.teacher.id}" },
      ) { pendingItem ->
        PendingEvaluationCard(
          item = pendingItem,
          onSelect = {
            onStartCourseEvaluation(pendingItem.course.id, pendingItem.teacher.id)
          },
        )
      }
    }

    // 6. Recent Evaluation Activity from Firestore
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("dashboard_recent_activity_section"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Recent Evaluation Activity",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
        if (summary.recentEvaluationActivity.isNotEmpty()) {
          TextButton(onClick = onOpenEvaluationHistory) {
            Text("Full History")
          }
        }
      }
    }

    if (summary.recentEvaluationActivity.isEmpty()) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_empty_recent_activity"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "No Completed Evaluations Yet",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = "Evaluations you submit for your department courses in Cloud Firestore will appear here with timestamps and overall ratings.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(
        items = summary.recentEvaluationActivity.take(5),
        key = { "recent_${it.evaluationId}" },
      ) { activity ->
        RecentEvaluationActivityCard(item = activity)
      }
    }

    if (!emailVerified) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "Email Verification Recommended",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Verify your university email (${profile.email}) to receive semester evaluation reminders.",
              style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
              onClick = onSendVerification,
              modifier = Modifier.testTag("send_email_verification_button"),
            ) {
              Icon(Icons.Default.MarkEmailRead, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Send Verification Email")
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun DashboardKpiMetricCard(
  modifier: Modifier = Modifier,
  title: String,
  value: String,
  subtitle: String,
  accentColor: androidx.compose.ui.graphics.Color,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = accentColor,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun PendingEvaluationCard(
  item: PendingCourseEvaluationItem,
  onSelect: () -> Unit,
) {
  Card(
    onClick = onSelect,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("pending_course_card_${item.course.id}"),
    shape = RoundedCornerShape(14.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "${item.course.courseCode} — ${item.course.courseName}",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Instructor: ${item.teacher.fullName} (${item.teacher.specialization})",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = item.semesterName,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      Surface(
        color = WcuGold500.copy(alpha = 0.18f),
        shape = RoundedCornerShape(999.dp),
      ) {
        Text(
          text = "Pending",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = WcuGold500,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }
  }
}

@Composable
private fun RecentEvaluationActivityCard(
  item: RecentEvaluationActivityItem,
) {
  val dateLabel =
    item.submittedAtMillis?.let { millis ->
      SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(millis))
    } ?: item.academicYear

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("recent_activity_card_${item.evaluationId}"),
    shape = RoundedCornerShape(14.dp),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${item.courseCode} • ${item.courseName}",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = WcuGold500,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = String.format(Locale.US, "%.2f / 5.0", item.overallScore),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      Text(
        text = "Instructor: ${item.teacherName}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (item.comment.isNotBlank()) {
        Text(
          text = "\"${item.comment}\"",
          style = MaterialTheme.typography.bodySmall,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Submitted: $dateLabel",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = item.status.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

@Composable
private fun StudentDepartmentTeachersTabContent(
  summary: StudentDashboardSummary,
  onBackHome: () -> Unit,
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 640.dp)
      .padding(horizontal = 16.dp)
      .testTag("student_teachers_tab_list"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item { Spacer(modifier = Modifier.height(4.dp)) }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WcuNavy900),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "Active Department Teachers (${summary.availableTeachers.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = WcuWhite,
          )
          Text(
            text = "${summary.departmentDisplayName} • ${summary.facultyDisplayName}",
            style = MaterialTheme.typography.bodySmall,
            color = WcuBlue100,
          )
        }
      }
    }

    if (summary.availableTeachers.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "No Active Teachers Found in Firestore",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "No active teachers are currently registered under ${summary.departmentDisplayName} in the /teachers Firestore collection.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onBackHome) {
              Text("Return to Dashboard")
            }
          }
        }
      }
    } else {
      items(summary.availableTeachers, key = { it.id }) { teacher ->
        TeacherDirectorySummaryCard(teacher = teacher)
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun TeacherDirectorySummaryCard(teacher: Teacher) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("teacher_card_${teacher.id}"),
    shape = RoundedCornerShape(14.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(WcuNavy900),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = teacher.fullName.take(2).uppercase(),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = WcuGold500,
        )
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = teacher.fullName,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = teacher.specialization,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = teacher.email,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun StudentPendingEvaluationsTabContent(
  summary: StudentDashboardSummary,
  onBackHome: () -> Unit,
  onStartCourseEvaluation: (String, String) -> Unit = { _, _ -> },
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 640.dp)
      .padding(horizontal = 16.dp)
      .testTag("student_pending_tab_list"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item { Spacer(modifier = Modifier.height(4.dp)) }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WcuNavy900),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "Pending Course Evaluations (${summary.pendingEvaluationsCount})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = WcuWhite,
          )
          Text(
            text = "Courses in ${summary.departmentDisplayName} awaiting your 6-category evaluation",
            style = MaterialTheme.typography.bodySmall,
            color = WcuBlue100,
          )
        }
      }
    }

    if (summary.pendingEvaluations.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "All Assigned Evaluations Completed",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "You have no pending teacher evaluations in Cloud Firestore for your department at this time.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onBackHome) {
              Text("Back to Student Dashboard")
            }
          }
        }
      }
    } else {
      items(
        items = summary.pendingEvaluations,
        key = { "pending_tab_${it.course.id}_${it.teacher.id}" },
      ) { item ->
        PendingEvaluationCard(
          item = item,
          onSelect = {
            onStartCourseEvaluation(item.course.id, item.teacher.id)
          },
        )
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun StudentEvaluationHistoryTabContent(
  summary: StudentDashboardSummary,
  onBackHome: () -> Unit,
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 640.dp)
      .padding(horizontal = 16.dp)
      .testTag("student_history_tab_list"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item { Spacer(modifier = Modifier.height(4.dp)) }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WcuNavy900),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "My Submitted Evaluations (${summary.completedEvaluationsCount})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = WcuWhite,
          )
          Text(
            text = "Immutable evaluation records stored in Cloud Firestore /evaluations",
            style = MaterialTheme.typography.bodySmall,
            color = WcuBlue100,
          )
        }
      }
    }

    if (summary.recentEvaluationActivity.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "No Evaluation History",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "You have not submitted any teacher evaluations yet.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onBackHome) {
              Text("Back to Student Dashboard")
            }
          }
        }
      }
    } else {
      items(
        items = summary.recentEvaluationActivity,
        key = { "history_${it.evaluationId}" },
      ) { item ->
        RecentEvaluationActivityCard(item = item)
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun StudentProfileTabContent(
  summary: StudentDashboardSummary,
  emailVerified: Boolean,
  onSendVerification: () -> Unit,
  onRequestLogout: () -> Unit,
) {
  val profile = summary.profile
  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 640.dp)
      .padding(horizontal = 16.dp)
      .testTag("student_profile_tab_list"),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item { Spacer(modifier = Modifier.height(4.dp)) }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Default.VerifiedUser,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Firestore Student Profile Record",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
          HorizontalDivider()
          ProfileDetailRow(label = "Full Name", value = profile.fullName)
          ProfileDetailRow(label = "Student ID", value = profile.studentId)
          ProfileDetailRow(label = "Email", value = profile.email)
          ProfileDetailRow(label = "Faculty", value = summary.facultyDisplayName)
          ProfileDetailRow(label = "Department", value = summary.departmentDisplayName)
          ProfileDetailRow(label = "Year of Study", value = "Year ${profile.yearOfStudy}")
          ProfileDetailRow(label = "Role", value = profile.role.uppercase())
          ProfileDetailRow(label = "Account Status", value = if (profile.isActive) "ACTIVE" else "INACTIVE")
          ProfileDetailRow(label = "UID", value = profile.uid)
        }
      }
    }

    if (!emailVerified) {
      item {
        OutlinedButton(
          onClick = onSendVerification,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Default.MarkEmailRead, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Send Email Verification Link")
        }
      }
    }

    item {
      OutlinedButton(
        onClick = onRequestLogout,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("student_sign_out_Action_button"),
        shape = RoundedCornerShape(12.dp),
      ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Sign Out of Student Portal")
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun ProfileDetailRow(
  label: String,
  value: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.SemiBold,
    )
  }
}
