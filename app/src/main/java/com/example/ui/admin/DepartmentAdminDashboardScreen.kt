package com.example.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.FeedbackBannerCard
import com.example.R
import com.example.WcuUniversityBanner
import com.example.data.export.EvaluationReportExporter
import com.example.data.model.AdminAccountRecord
import com.example.data.model.DepartmentAdminDashboardSummary
import com.example.data.model.UserProfile
import com.example.ui.theme.WcuGold500
import com.example.ui.theme.WcuNavy900
import com.example.ui.theme.WcuWhite
import java.util.Locale

enum class DepartmentAdminSection(val label: String) {
  DASHBOARD("Dashboard"),
  STUDENTS("Students"),
  TEACHERS("Teachers"),
  COURSES("Courses"),
  EVALUATIONS("Evaluations"),
  REPORTS("Reports"),
  PROFILE_SETTINGS("Profile / Settings"),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DepartmentAdminDashboardScreen(
  profile: UserProfile,
  adminRecord: AdminAccountRecord,
  emailVerified: Boolean,
  isDark: Boolean,
  onToggleDark: () -> Unit,
  feedbackBanner: Pair<Boolean, String>?,
  onClearBanner: () -> Unit,
  onSignOut: () -> Unit,
  viewModel: AdminManagementViewModel = viewModel(factory = AdminManagementViewModel.Factory),
) {
  val context = LocalContext.current
  val uiState by viewModel.departmentAdminUiState.collectAsStateWithLifecycle()
  val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()
  val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
  val lastExportedReport by viewModel.lastExportedReport.collectAsStateWithLifecycle()
  val actionBanner by viewModel.actionBanner.collectAsStateWithLifecycle()

  var currentSection by rememberSaveable { mutableStateOf(DepartmentAdminSection.DASHBOARD) }
  var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

  if (currentSection != DepartmentAdminSection.DASHBOARD) {
    BackHandler { currentSection = DepartmentAdminSection.DASHBOARD }
  }

  LaunchedEffect(adminRecord.uid, adminRecord.departmentId) {
    viewModel.loadDepartmentAdminDashboard(adminRecord)
  }

  if (showLogoutDialog) {
    AlertDialog(
      onDismissRequest = { showLogoutDialog = false },
      title = { Text("Confirm Department Admin Sign Out") },
      text = { Text("Sign out of your Wachemo University Department Admin portal?") },
      confirmButton = {
        Button(
          onClick = {
            showLogoutDialog = false
            onSignOut()
          },
          modifier = Modifier.testTag("confirm_dept_admin_logout_button"),
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
    modifier = Modifier
      .fillMaxSize()
      .testTag("department_admin_dashboard_screen"),
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "WCU Department Admin Console",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = WcuWhite,
            )
            Text(
              text = "LEVEL 2 • DEPT: ${adminRecord.departmentId} (${adminRecord.facultyId})",
              style = MaterialTheme.typography.labelSmall,
              color = WcuGold500,
            )
          }
        },
        actions = {
          IconButton(
            onClick = { viewModel.loadDepartmentAdminDashboard(adminRecord) },
            modifier = Modifier.testTag("dept_admin_refresh_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Refresh Department Dashboard",
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
            modifier = Modifier.testTag("dept_admin_logout_button"),
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
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      // Strictly 7 Department Admin Sections (no Faculties, no other Departments, no Admin creation)
      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp)
          .testTag("dept_admin_section_nav_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(DepartmentAdminSection.entries) { section ->
          FilterChip(
            selected = currentSection == section,
            onClick = { currentSection = section },
            label = { Text(section.label) },
            modifier = Modifier.testTag("dept_admin_nav_${section.name.lowercase()}"),
          )
        }
      }

      when (val state = uiState) {
        DepartmentAdminUiState.Loading -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .testTag("dept_admin_loading_state"),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              CircularProgressIndicator()
              Text("Loading department records for ${adminRecord.departmentId}...")
            }
          }
        }

        is DepartmentAdminUiState.Error -> {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(24.dp)
              .testTag("dept_admin_error_state"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            FeedbackBannerCard(
              isError = true,
              message = state.message,
              onDismiss = { viewModel.loadDepartmentAdminDashboard(adminRecord) },
            )
            Button(
              onClick = { viewModel.loadDepartmentAdminDashboard(adminRecord) },
              modifier = Modifier.testTag("dept_admin_retry_button"),
            ) {
              Text("Retry Department Load")
            }
          }
        }

        is DepartmentAdminUiState.Success -> {
          val summary = state.summary

          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            if (feedbackBanner != null) {
              item {
                FeedbackBannerCard(
                  isError = feedbackBanner.first,
                  message = feedbackBanner.second,
                  onDismiss = onClearBanner,
                )
              }
            }

            if (actionBanner != null) {
              item {
                FeedbackBannerCard(
                  isError = actionBanner!!.first,
                  message = actionBanner!!.second,
                  onDismiss = viewModel::clearActionBanner,
                )
              }
            }

            when (currentSection) {
              DepartmentAdminSection.DASHBOARD -> {
                item {
                  WcuUniversityBanner(
                    title = summary.departmentDisplayName,
                    subtitle = "${summary.facultyDisplayName} • Assigned Admin: ${profile.fullName}",
                  )
                }

                // Assigned Faculty & Department Scope Card
                item {
                  Card(
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("dept_admin_scope_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                  ) {
                    Row(
                      modifier = Modifier.padding(14.dp),
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                      )
                      Column {
                        Text(
                          text = "Department-Scoped Access Enforced",
                          style = MaterialTheme.typography.labelLarge,
                          fontWeight = FontWeight.Bold,
                        )
                        Text(
                          text = "Faculty: ${summary.facultyDisplayName} (${adminRecord.facultyId}) • Department: ${summary.departmentDisplayName} (${adminRecord.departmentId})",
                          style = MaterialTheme.typography.bodySmall,
                        )
                      }
                    }
                  }
                }

                // Department KPI Card
                item {
                  DepartmentAdminKpiCard(summary = summary)
                }

                // Quick Actions
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
                        text = "Department Quick Actions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                      )
                      FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                      ) {
                        AssistChip(
                          onClick = { currentSection = DepartmentAdminSection.TEACHERS },
                          label = { Text("Manage Department Teachers") },
                          leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                          modifier = Modifier.testTag("dept_quick_action_teachers"),
                        )
                        AssistChip(
                          onClick = { currentSection = DepartmentAdminSection.COURSES },
                          label = { Text("Manage Department Courses") },
                          leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                          modifier = Modifier.testTag("dept_quick_action_courses"),
                        )
                        AssistChip(
                          onClick = { currentSection = DepartmentAdminSection.STUDENTS },
                          label = { Text("View Department Students") },
                          leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                          modifier = Modifier.testTag("dept_quick_action_students"),
                        )
                        AssistChip(
                          onClick = { currentSection = DepartmentAdminSection.EVALUATIONS },
                          label = { Text("View Department Evaluations") },
                          leadingIcon = { Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null) },
                          modifier = Modifier.testTag("dept_quick_action_evaluations"),
                        )
                        AssistChip(
                          onClick = { currentSection = DepartmentAdminSection.REPORTS },
                          label = { Text("Department Reports") },
                          leadingIcon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                          modifier = Modifier.testTag("dept_quick_action_reports"),
                        )
                      }
                    }
                  }
                }

                // Recent Department Evaluations
                item {
                  DepartmentEvaluationsListCard(summary = summary)
                }
              }

              DepartmentAdminSection.STUDENTS -> {
                item {
                  Card(
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("dept_admin_students_card"),
                    shape = RoundedCornerShape(16.dp),
                  ) {
                    Column(
                      modifier = Modifier.padding(16.dp),
                      verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Text(
                        text = "${summary.departmentDisplayName} Students (${summary.departmentStudents.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                      if (summary.departmentStudents.isEmpty()) {
                        Text(
                          text = "No students registered in ${summary.departmentDisplayName} yet.",
                          style = MaterialTheme.typography.bodySmall,
                        )
                      } else {
                        summary.departmentStudents.forEach { student ->
                          Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                          ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                              Text(student.fullName, fontWeight = FontWeight.Bold)
                              Text(
                                "${student.studentId} • ${student.email} • Year ${student.yearOfStudy}",
                                style = MaterialTheme.typography.bodySmall,
                              )
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }

              DepartmentAdminSection.TEACHERS -> {
                item {
                  DepartmentAdminTeachersSection(
                    summary = summary,
                    isMutating = isMutating,
                    onSaveTeacher = { id, name, email, spec ->
                      viewModel.saveTeacherAsDepartmentAdmin(
                        teacherId = id,
                        fullName = name,
                        email = email,
                        specialization = spec,
                        isActive = true,
                        adminRecord = adminRecord,
                      )
                    },
                  )
                }
              }

              DepartmentAdminSection.COURSES -> {
                item {
                  DepartmentAdminCoursesSection(
                    summary = summary,
                    isMutating = isMutating,
                    onSaveCourse = { id, code, name, teacherId, semId, year ->
                      viewModel.saveCourseAsDepartmentAdmin(
                        courseId = id,
                        courseCode = code,
                        courseName = name,
                        teacherId = teacherId,
                        semesterId = semId,
                        academicYear = year,
                        isActive = true,
                        adminRecord = adminRecord,
                      )
                    },
                  )
                }
              }

              DepartmentAdminSection.EVALUATIONS -> {
                item {
                  DepartmentAdminKpiCard(summary = summary)
                }
                item {
                  DepartmentEvaluationsListCard(summary = summary)
                }
              }

              DepartmentAdminSection.REPORTS -> {
                item {
                  DepartmentAdminEvaluationReportSection(
                    summary = summary,
                    isExporting = isExporting,
                    lastExportedReport = lastExportedReport,
                    onExportReport = { format, filter ->
                      viewModel.exportDepartmentAdminReport(
                        outputDir = EvaluationReportExporter.getReportsDirectory(context),
                        format = format,
                        filter = filter,
                        adminRecord = adminRecord,
                      )
                    },
                  )
                }
                item {
                  DepartmentAdminKpiCard(summary = summary)
                }
                item {
                  DepartmentEvaluationsListCard(summary = summary)
                }
              }

              DepartmentAdminSection.PROFILE_SETTINGS -> {
                item {
                  Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                  ) {
                    Column(
                      modifier = Modifier.padding(16.dp),
                      verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Text(
                        text = "Department Administrator Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                      Text(
                        "UID: ${profile.uid}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                      )
                      Text("Full Name: ${profile.fullName}")
                      Text("Email: ${profile.email} (Verified: $emailVerified)")
                      Text("Role: DEPARTMENT_ADMIN (Level 2)")
                      Text("Assigned Faculty: ${summary.facultyDisplayName} (${adminRecord.facultyId})")
                      Text("Assigned Department: ${summary.departmentDisplayName} (${adminRecord.departmentId})")
                      Text(
                        "Note: Only a Super Admin can modify your assigned Faculty or Department.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
              }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DepartmentAdminKpiCard(summary: DepartmentAdminDashboardSummary) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("dept_admin_kpi_card"),
    shape = RoundedCornerShape(16.dp),
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
        Text(
          text = "Department Metrics",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = WcuGold500,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = String.format(Locale.US, "%.2f / 5.0", summary.departmentAverageRating),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        DeptMetricBadge("Dept Students", summary.totalStudents.toString())
        DeptMetricBadge("Active Teachers", summary.totalActiveTeachers.toString())
        DeptMetricBadge("Active Courses", summary.totalActiveCourses.toString())
        DeptMetricBadge("Submitted Evals", summary.totalSubmittedEvaluations.toString())
        DeptMetricBadge("Pending Evals", summary.pendingEvaluationsCount.toString())
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Department Evaluation Completion",
            style = MaterialTheme.typography.labelMedium,
          )
          Text(
            text = "${summary.completionPercentage}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        LinearProgressIndicator(
          progress = { (summary.completionPercentage / 100f).coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp)),
        )
      }
    }
  }
}

@Composable
private fun DeptMetricBadge(label: String, value: String) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(12.dp),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
      )
    }
  }
}

@Composable
private fun DepartmentEvaluationsListCard(summary: DepartmentAdminDashboardSummary) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("dept_admin_evaluations_card"),
    shape = RoundedCornerShape(16.dp),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "Recent Evaluations in ${summary.departmentDisplayName} (${summary.departmentEvaluations.size})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      if (summary.departmentEvaluations.isEmpty()) {
        Text(
          text = "No teacher evaluations submitted in this department yet.",
          style = MaterialTheme.typography.bodySmall,
        )
      } else {
        summary.departmentEvaluations.take(15).forEach { eval ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "Teacher: ${eval.teacherId} • Course: ${eval.courseId}",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = String.format(Locale.US, "%.2f ★", eval.overallScore),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              if (eval.comment.isNotBlank()) {
                Text(
                  text = "\"${eval.comment}\"",
                  style = MaterialTheme.typography.bodySmall,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DepartmentAdminTeachersSection(
  summary: DepartmentAdminDashboardSummary,
  isMutating: Boolean,
  onSaveTeacher: (String, String, String, String) -> Unit,
) {
  var idInput by rememberSaveable { mutableStateOf("") }
  var nameInput by rememberSaveable { mutableStateOf("") }
  var emailInput by rememberSaveable { mutableStateOf("") }
  var specInput by rememberSaveable { mutableStateOf("") }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("dept_admin_add_teacher_card"),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "Add or Update Teacher in ${summary.departmentDisplayName}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Automatically locked to Faculty ${summary.adminRecord.facultyId} and Department ${summary.adminRecord.departmentId}.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = idInput,
          onValueChange = { idInput = it },
          label = { Text("Teacher ID (e.g. tch_se_10)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = nameInput,
          onValueChange = { nameInput = it },
          label = { Text("Teacher Full Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = emailInput,
          onValueChange = { emailInput = it },
          label = { Text("Institutional Email") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = specInput,
          onValueChange = { specInput = it },
          label = { Text("Academic Rank & Specialization") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Button(
          onClick = {
            onSaveTeacher(idInput, nameInput, emailInput, specInput)
            idInput = ""
            nameInput = ""
            emailInput = ""
            specInput = ""
          },
          enabled = !isMutating && idInput.isNotBlank() && nameInput.isNotBlank() && emailInput.contains("@"),
          modifier = Modifier.testTag("dept_admin_save_teacher_button"),
        ) {
          Text("Save Department Teacher")
        }
      }
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "Department Teachers (${summary.departmentTeachers.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        summary.departmentTeachers.forEach { teacher ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(teacher.fullName, fontWeight = FontWeight.Bold)
              Text(
                "${teacher.specialization} • ${teacher.email} • Active: ${teacher.isActive}",
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DepartmentAdminCoursesSection(
  summary: DepartmentAdminDashboardSummary,
  isMutating: Boolean,
  onSaveCourse: (String, String, String, String, String, String) -> Unit,
) {
  var idInput by rememberSaveable { mutableStateOf("") }
  var codeInput by rememberSaveable { mutableStateOf("") }
  var nameInput by rememberSaveable { mutableStateOf("") }
  var teacherIdInput by rememberSaveable {
    mutableStateOf(summary.departmentTeachers.firstOrNull()?.id.orEmpty())
  }
  var semesterIdInput by rememberSaveable { mutableStateOf("sem_2025_1") }
  var academicYearInput by rememberSaveable { mutableStateOf("2024/2025") }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("dept_admin_add_course_card"),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "Add or Update Course in ${summary.departmentDisplayName}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
          value = idInput,
          onValueChange = { idInput = it },
          label = { Text("Course ID (e.g. crs_se_409)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = codeInput,
          onValueChange = { codeInput = it },
          label = { Text("Course Code (e.g. SENG409)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = nameInput,
          onValueChange = { nameInput = it },
          label = { Text("Course Title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = teacherIdInput,
          onValueChange = { teacherIdInput = it },
          label = { Text("Assigned Teacher ID") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Button(
          onClick = {
            onSaveCourse(idInput, codeInput, nameInput, teacherIdInput, semesterIdInput, academicYearInput)
            idInput = ""
            codeInput = ""
            nameInput = ""
          },
          enabled = !isMutating && idInput.isNotBlank() && codeInput.isNotBlank() && nameInput.isNotBlank() && teacherIdInput.isNotBlank(),
          modifier = Modifier.testTag("dept_admin_save_course_button"),
        ) {
          Text("Save Department Course")
        }
      }
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "Department Courses (${summary.departmentCourses.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        summary.departmentCourses.forEach { course ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text("${course.courseCode} — ${course.courseName}", fontWeight = FontWeight.Bold)
              Text(
                "Teacher: ${course.teacherId} • Semester: ${course.semesterId} (${course.academicYear})",
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }
      }
    }
  }
}
