package com.example.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddModerator
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.remember
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
import com.example.data.model.SuperAdminDashboardSummary
import com.example.data.model.UserProfile
import com.example.data.model.WcuAcademicCatalogFallback
import com.example.ui.theme.WcuGold500
import com.example.ui.theme.WcuNavy900
import com.example.ui.theme.WcuWhite
import java.util.Locale

enum class SuperAdminSection(val label: String) {
  DASHBOARD("Dashboard"),
  FACULTIES("Faculties"),
  DEPARTMENTS("Departments"),
  STUDENTS("Students"),
  TEACHERS("Teachers"),
  COURSES("Courses"),
  ADMINISTRATORS("Administrators"),
  EVALUATIONS("Evaluations"),
  REPORTS_ANALYTICS("Reports & Analytics"),
  ACADEMIC_SETTINGS("Academic Settings"),
  PROFILE("Profile"),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SuperAdminDashboardScreen(
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
  val uiState by viewModel.superAdminUiState.collectAsStateWithLifecycle()
  val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()
  val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
  val lastExportedReport by viewModel.lastExportedReport.collectAsStateWithLifecycle()
  val actionBanner by viewModel.actionBanner.collectAsStateWithLifecycle()

  var currentSection by rememberSaveable { mutableStateOf(SuperAdminSection.DASHBOARD) }
  var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

  // Confirmation dialog state for activating/deactivating an admin
  var pendingStatusToggleAdmin by remember { mutableStateOf<AdminAccountRecord?>(null) }
  // Reassign department dialog state
  var pendingReassignAdmin by remember { mutableStateOf<AdminAccountRecord?>(null) }

  if (currentSection != SuperAdminSection.DASHBOARD) {
    BackHandler { currentSection = SuperAdminSection.DASHBOARD }
  }

  LaunchedEffect(profile.uid) {
    viewModel.loadSuperAdminDashboard(profile)
  }

  if (showLogoutDialog) {
    AlertDialog(
      onDismissRequest = { showLogoutDialog = false },
      title = { Text("Confirm Super Admin Sign Out") },
      text = { Text("Sign out of the Wachemo University Super Admin Console?") },
      confirmButton = {
        Button(
          onClick = {
            showLogoutDialog = false
            onSignOut()
          },
          modifier = Modifier.testTag("confirm_super_admin_logout_button"),
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

  pendingStatusToggleAdmin?.let { targetAdmin ->
    val nextState = !targetAdmin.isActive
    AlertDialog(
      onDismissRequest = { pendingStatusToggleAdmin = null },
      title = {
        Text(
          if (nextState) "Activate Administrator Account"
          else "Deactivate Administrator Account"
        )
      },
      text = {
        Text(
          if (nextState) {
            "Restore active Department Admin privileges for ${targetAdmin.fullName} (${targetAdmin.email})?"
          } else {
            "Immediately revoke Department Admin privileges for ${targetAdmin.fullName} (${targetAdmin.email})? They will be blocked from signing in and performing any Firestore administrative actions."
          }
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.setAdministratorStatus(targetAdmin.uid, nextState, profile)
            pendingStatusToggleAdmin = null
          },
          modifier = Modifier.testTag("confirm_toggle_admin_status_button"),
        ) {
          Text(if (nextState) "Activate" else "Deactivate")
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingStatusToggleAdmin = null }) {
          Text("Cancel")
        }
      },
    )
  }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .testTag("super_admin_dashboard_screen"),
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "WCU Super Admin Console",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = WcuWhite,
            )
            Text(
              text = "LEVEL 1 • UNIVERSITY-WIDE • ${profile.email}",
              style = MaterialTheme.typography.labelSmall,
              color = WcuGold500,
            )
          }
        },
        actions = {
          IconButton(
            onClick = { viewModel.loadSuperAdminDashboard(profile) },
            modifier = Modifier.testTag("super_admin_refresh_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Refresh Super Admin Dashboard",
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
            modifier = Modifier.testTag("super_admin_logout_button"),
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
      // Horizontal Navigation Strip for all 11 Super Admin Sections
      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp)
          .testTag("super_admin_section_nav_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(SuperAdminSection.entries) { section ->
          FilterChip(
            selected = currentSection == section,
            onClick = { currentSection = section },
            label = { Text(section.label) },
            modifier = Modifier.testTag("super_admin_nav_${section.name.lowercase()}"),
          )
        }
      }

      when (val state = uiState) {
        SuperAdminUiState.Loading -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .testTag("super_admin_loading_state"),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              CircularProgressIndicator()
              Text("Loading university-wide records from Cloud Firestore...")
            }
          }
        }

        is SuperAdminUiState.Error -> {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(24.dp)
              .testTag("super_admin_error_state"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            FeedbackBannerCard(
              isError = true,
              message = state.message,
              onDismiss = { viewModel.loadSuperAdminDashboard(profile) },
            )
            Button(
              onClick = { viewModel.loadSuperAdminDashboard(profile) },
              modifier = Modifier.testTag("super_admin_retry_button"),
            ) {
              Text("Retry Firestore Load")
            }
          }
        }

        is SuperAdminUiState.Success -> {
          val summary = state.summary

          pendingReassignAdmin?.let { targetAdmin ->
            ReassignDepartmentAdminDialog(
              targetAdmin = targetAdmin,
              summary = summary,
              onDismiss = { pendingReassignAdmin = null },
              onConfirm = { facId, deptId ->
                viewModel.reassignDepartmentAdmin(targetAdmin.uid, facId, deptId, profile)
                pendingReassignAdmin = null
              },
            )
          }

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
              SuperAdminSection.DASHBOARD -> {
                item {
                  WcuUniversityBanner(
                    title = "Super Admin: ${profile.fullName}",
                    subtitle = "University-Wide Governance • All Faculties, Departments & Admins",
                  )
                }

                // KPI Grid
                item {
                  SuperAdminKpiCard(summary = summary)
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
                        text = "University Governance Quick Actions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                      )
                      FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                      ) {
                        AssistChip(
                          onClick = { currentSection = SuperAdminSection.FACULTIES },
                          label = { Text("Manage Faculties") },
                          leadingIcon = { Icon(Icons.Default.Domain, contentDescription = null) },
                          modifier = Modifier.testTag("quick_action_manage_faculties"),
                        )
                        AssistChip(
                          onClick = { currentSection = SuperAdminSection.DEPARTMENTS },
                          label = { Text("Manage Departments") },
                          leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null) },
                          modifier = Modifier.testTag("quick_action_manage_departments"),
                        )
                        AssistChip(
                          onClick = { currentSection = SuperAdminSection.ADMINISTRATORS },
                          label = { Text("Manage Department Admins") },
                          leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                          modifier = Modifier.testTag("quick_action_manage_admins"),
                        )
                        AssistChip(
                          onClick = { currentSection = SuperAdminSection.TEACHERS },
                          label = { Text("Manage Teachers") },
                          leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                          modifier = Modifier.testTag("quick_action_manage_teachers"),
                        )
                        AssistChip(
                          onClick = { currentSection = SuperAdminSection.COURSES },
                          label = { Text("Manage Courses") },
                          leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                          modifier = Modifier.testTag("quick_action_manage_courses"),
                        )
                        AssistChip(
                          onClick = { currentSection = SuperAdminSection.EVALUATIONS },
                          label = { Text("View Evaluations") },
                          leadingIcon = { Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null) },
                          modifier = Modifier.testTag("quick_action_view_evaluations"),
                        )
                        AssistChip(
                          onClick = { currentSection = SuperAdminSection.REPORTS_ANALYTICS },
                          label = { Text("University Analytics") },
                          leadingIcon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                          modifier = Modifier.testTag("quick_action_university_analytics"),
                        )
                      }
                    }
                  }
                }

                // Department Performance Comparison
                item {
                  DepartmentPerformanceComparisonCard(summary = summary)
                }

                // Recent Administrator Activity Logs
                item {
                  AuditLogSectionCard(summary = summary)
                }
              }

              SuperAdminSection.ADMINISTRATORS -> {
                item {
                  SuperAdminManageAdminsSection(
                    summary = summary,
                    isMutating = isMutating,
                    onCreateDepartmentAdmin = { uid, name, email, facId, deptId ->
                      viewModel.createDepartmentAdmin(uid, name, email, facId, deptId, profile)
                    },
                    onToggleAdminStatus = { target -> pendingStatusToggleAdmin = target },
                    onReassignAdmin = { target -> pendingReassignAdmin = target },
                  )
                }
              }

              SuperAdminSection.FACULTIES -> {
                item {
                  SuperAdminFacultiesSection(
                    summary = summary,
                    isMutating = isMutating,
                    onSaveFaculty = { id, name, desc ->
                      viewModel.saveFaculty(id, name, desc, true, profile)
                    },
                  )
                }
              }

              SuperAdminSection.DEPARTMENTS -> {
                item {
                  SuperAdminDepartmentsSection(
                    summary = summary,
                    isMutating = isMutating,
                    onSaveDepartment = { id, name, facId, desc ->
                      viewModel.saveDepartment(id, name, facId, desc, true, profile)
                    },
                  )
                }
              }

              SuperAdminSection.STUDENTS -> {
                item {
                  Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                  ) {
                    Column(
                      modifier = Modifier.padding(16.dp),
                      verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Text(
                        text = "University Students (${summary.students.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                      if (summary.students.isEmpty()) {
                        Text(
                          text = "No student records registered in Firestore yet.",
                          style = MaterialTheme.typography.bodySmall,
                        )
                      } else {
                        summary.students.forEach { student ->
                          Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                          ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                              Text(student.fullName, fontWeight = FontWeight.Bold)
                              Text(
                                "${student.studentId} • ${student.email} • Dept: ${student.departmentId}",
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

              SuperAdminSection.TEACHERS -> {
                item {
                  var teacherIdInput by rememberSaveable { mutableStateOf("") }
                  var teacherNameInput by rememberSaveable { mutableStateOf("") }
                  var teacherEmailInput by rememberSaveable { mutableStateOf("") }
                  var teacherFacInput by rememberSaveable { mutableStateOf(summary.faculties.firstOrNull()?.id ?: "fac_eng_tech") }
                  var teacherDeptInput by rememberSaveable { mutableStateOf(summary.departments.firstOrNull()?.id ?: "dep_se") }
                  var teacherSpecInput by rememberSaveable { mutableStateOf("") }

                  Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                  ) {
                    Column(
                      modifier = Modifier.padding(16.dp),
                      verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Text(
                        text = "Add or Update University Teacher",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                      OutlinedTextField(
                        value = teacherIdInput,
                        onValueChange = { teacherIdInput = it },
                        label = { Text("Teacher ID (e.g., tch_se_03)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("super_admin_teacher_id_input"),
                      )
                      OutlinedTextField(
                        value = teacherNameInput,
                        onValueChange = { teacherNameInput = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("super_admin_teacher_name_input"),
                      )
                      OutlinedTextField(
                        value = teacherEmailInput,
                        onValueChange = { teacherEmailInput = it },
                        label = { Text("Institutional Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("super_admin_teacher_email_input"),
                      )
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                          value = teacherFacInput,
                          onValueChange = { teacherFacInput = it },
                          label = { Text("Faculty ID") },
                          singleLine = true,
                          modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                          value = teacherDeptInput,
                          onValueChange = { teacherDeptInput = it },
                          label = { Text("Department ID") },
                          singleLine = true,
                          modifier = Modifier.weight(1f),
                        )
                      }
                      OutlinedTextField(
                        value = teacherSpecInput,
                        onValueChange = { teacherSpecInput = it },
                        label = { Text("Academic Rank & Specialization") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("super_admin_teacher_spec_input"),
                      )
                      Button(
                        onClick = {
                          viewModel.saveTeacherAsSuperAdmin(
                            teacherId = teacherIdInput,
                            fullName = teacherNameInput,
                            email = teacherEmailInput,
                            facultyId = teacherFacInput,
                            departmentId = teacherDeptInput,
                            specialization = teacherSpecInput,
                            isActive = true,
                            adminProfile = profile,
                          )
                          teacherIdInput = ""
                          teacherNameInput = ""
                          teacherEmailInput = ""
                          teacherSpecInput = ""
                        },
                        enabled = !isMutating && teacherIdInput.isNotBlank() && teacherNameInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("super_admin_save_teacher_button"),
                      ) {
                        Text("Save Teacher Record")
                      }

                      HorizontalDivider()

                      Text(
                        text = "University Teachers (${summary.teachers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                      summary.teachers.forEach { teacher ->
                        Surface(
                          color = MaterialTheme.colorScheme.surfaceVariant,
                          shape = RoundedCornerShape(12.dp),
                          modifier = Modifier.fillMaxWidth(),
                        ) {
                          Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                          ) {
                            Column(modifier = Modifier.weight(1f)) {
                              Text(teacher.fullName, fontWeight = FontWeight.Bold)
                              Text(
                                "${teacher.specialization} • Dept: ${teacher.departmentId} • Active: ${teacher.isActive}",
                                style = MaterialTheme.typography.bodySmall,
                              )
                            }
                            OutlinedButton(
                              onClick = {
                                viewModel.saveTeacherAsSuperAdmin(
                                  teacherId = teacher.id,
                                  fullName = teacher.fullName,
                                  email = teacher.email,
                                  facultyId = teacher.facultyId,
                                  departmentId = teacher.departmentId,
                                  specialization = teacher.specialization,
                                  isActive = !teacher.isActive,
                                  adminProfile = profile,
                                )
                              },
                              enabled = !isMutating,
                            ) {
                              Text(if (teacher.isActive) "Deactivate" else "Activate")
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }

              SuperAdminSection.COURSES -> {
                item {
                  var courseIdInput by rememberSaveable { mutableStateOf("") }
                  var courseCodeInput by rememberSaveable { mutableStateOf("") }
                  var courseNameInput by rememberSaveable { mutableStateOf("") }
                  var courseTeacherInput by rememberSaveable { mutableStateOf(summary.teachers.firstOrNull()?.id ?: "tch_se_01") }
                  var courseDeptInput by rememberSaveable { mutableStateOf(summary.departments.firstOrNull()?.id ?: "dep_se") }
                  var courseSemInput by rememberSaveable { mutableStateOf(summary.semesters.firstOrNull()?.id ?: "sem_2026_1") }
                  var courseYearInput by rememberSaveable { mutableStateOf("2026/27") }

                  Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                  ) {
                    Column(
                      modifier = Modifier.padding(16.dp),
                      verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Text(
                        text = "Add or Update University Course",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                          value = courseIdInput,
                          onValueChange = { courseIdInput = it },
                          label = { Text("Course ID (e.g., crs_se_305)") },
                          singleLine = true,
                          modifier = Modifier.weight(1f).testTag("super_admin_course_id_input"),
                        )
                        OutlinedTextField(
                          value = courseCodeInput,
                          onValueChange = { courseCodeInput = it },
                          label = { Text("Code (e.g., SENG305)") },
                          singleLine = true,
                          modifier = Modifier.weight(1f).testTag("super_admin_course_code_input"),
                        )
                      }
                      OutlinedTextField(
                        value = courseNameInput,
                        onValueChange = { courseNameInput = it },
                        label = { Text("Course Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("super_admin_course_name_input"),
                      )
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                          value = courseDeptInput,
                          onValueChange = { courseDeptInput = it },
                          label = { Text("Department ID") },
                          singleLine = true,
                          modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                          value = courseTeacherInput,
                          onValueChange = { courseTeacherInput = it },
                          label = { Text("Assigned Teacher ID") },
                          singleLine = true,
                          modifier = Modifier.weight(1f),
                        )
                      }
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                          value = courseSemInput,
                          onValueChange = { courseSemInput = it },
                          label = { Text("Semester ID") },
                          singleLine = true,
                          modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                          value = courseYearInput,
                          onValueChange = { courseYearInput = it },
                          label = { Text("Academic Year") },
                          singleLine = true,
                          modifier = Modifier.weight(1f),
                        )
                      }
                      Button(
                        onClick = {
                          viewModel.saveCourseAsSuperAdmin(
                            courseId = courseIdInput,
                            courseCode = courseCodeInput,
                            courseName = courseNameInput,
                            teacherId = courseTeacherInput,
                            departmentId = courseDeptInput,
                            semesterId = courseSemInput,
                            academicYear = courseYearInput,
                            isActive = true,
                            adminProfile = profile,
                          )
                          courseIdInput = ""
                          courseCodeInput = ""
                          courseNameInput = ""
                        },
                        enabled = !isMutating && courseIdInput.isNotBlank() && courseCodeInput.isNotBlank() && courseNameInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("super_admin_save_course_button"),
                      ) {
                        Text("Save Course & Teacher Assignment")
                      }

                      HorizontalDivider()

                      Text(
                        text = "University Courses (${summary.courses.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                      summary.courses.forEach { course ->
                        Surface(
                          color = MaterialTheme.colorScheme.surfaceVariant,
                          shape = RoundedCornerShape(12.dp),
                          modifier = Modifier.fillMaxWidth(),
                        ) {
                          Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                              "${course.courseCode} — ${course.courseName}",
                              fontWeight = FontWeight.Bold,
                            )
                            Text(
                              "Dept: ${course.departmentId} • Teacher: ${course.teacherId} • ${course.academicYear}",
                              style = MaterialTheme.typography.bodySmall,
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }

              SuperAdminSection.EVALUATIONS -> {
                item {
                  DepartmentPerformanceComparisonCard(summary = summary)
                }
              }

              SuperAdminSection.REPORTS_ANALYTICS -> {
                item {
                  SuperAdminEvaluationReportSection(
                    summary = summary,
                    isExporting = isExporting,
                    lastExportedReport = lastExportedReport,
                    onExportReport = { format, filter ->
                      viewModel.exportSuperAdminReport(
                        outputDir = EvaluationReportExporter.getReportsDirectory(context),
                        format = format,
                        filter = filter,
                        adminProfile = profile,
                      )
                    },
                  )
                }
                item {
                  DepartmentPerformanceComparisonCard(summary = summary)
                }
                item {
                  AuditLogSectionCard(summary = summary)
                }
              }

              SuperAdminSection.ACADEMIC_SETTINGS -> {
                item {
                  var semIdInput by rememberSaveable { mutableStateOf("sem_2026_1") }
                  var semNameInput by rememberSaveable { mutableStateOf("Semester I") }
                  var semYearInput by rememberSaveable { mutableStateOf("2026/27") }
                  var semEvaluationOpen by rememberSaveable { mutableStateOf(true) }

                  Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                  ) {
                    Column(
                      modifier = Modifier.padding(16.dp),
                      verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Text(
                        text = "Configure Academic Semester & Evaluation Period",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                          value = semIdInput,
                          onValueChange = { semIdInput = it },
                          label = { Text("Semester ID") },
                          singleLine = true,
                          modifier = Modifier.weight(1f).testTag("super_admin_semester_id_input"),
                        )
                        OutlinedTextField(
                          value = semYearInput,
                          onValueChange = { semYearInput = it },
                          label = { Text("Academic Year") },
                          singleLine = true,
                          modifier = Modifier.weight(1f).testTag("super_admin_semester_year_input"),
                        )
                      }
                      OutlinedTextField(
                        value = semNameInput,
                        onValueChange = { semNameInput = it },
                        label = { Text("Semester Name (e.g., Semester I)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("super_admin_semester_name_input"),
                      )
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                          selected = semEvaluationOpen,
                          onClick = { semEvaluationOpen = !semEvaluationOpen },
                          label = {
                            Text(
                              if (semEvaluationOpen) "Evaluation Window: OPEN"
                              else "Evaluation Window: CLOSED"
                            )
                          },
                        )
                      }
                      Button(
                        onClick = {
                          viewModel.saveSemester(
                            semesterId = semIdInput,
                            name = semNameInput,
                            academicYear = semYearInput,
                            isActive = true,
                            isEvaluationOpen = semEvaluationOpen,
                            adminProfile = profile,
                          )
                        },
                        enabled = !isMutating && semIdInput.isNotBlank() && semNameInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("super_admin_save_semester_button"),
                      ) {
                        Text("Save Semester Settings")
                      }

                      HorizontalDivider()

                      Text(
                        text = "Academic Semesters & University Settings (${summary.semesters.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                      summary.semesters.forEach { sem ->
                        Surface(
                          color = MaterialTheme.colorScheme.surfaceVariant,
                          shape = RoundedCornerShape(12.dp),
                          modifier = Modifier.fillMaxWidth(),
                        ) {
                          Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                              "${sem.name} (${sem.academicYear})",
                              fontWeight = FontWeight.Bold,
                            )
                            Text(
                              "ID: ${sem.id} • Active Semester: ${sem.isActive} • Evaluation Open: ${sem.isEvaluationOpen}",
                              style = MaterialTheme.typography.bodySmall,
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }

              SuperAdminSection.PROFILE -> {
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
                        text = "Verified Super Admin Profile",
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
                      Text("Role: SUPER_ADMIN (Level 1 University-Wide Governance)")
                      Text("Active Status: ${adminRecord.isActive}")
                    }
                  }
                }
                item {
                  AuditLogSectionCard(summary = summary)
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
private fun SuperAdminKpiCard(summary: SuperAdminDashboardSummary) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("super_admin_kpi_card"),
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
          text = "University-Wide Key Metrics",
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
            text = String.format(Locale.US, "%.2f / 5.0", summary.overallUniversityRating),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        MetricBadge("Faculties", summary.totalFaculties.toString())
        MetricBadge("Departments", summary.totalDepartments.toString())
        MetricBadge("Students", summary.totalStudents.toString())
        MetricBadge("Teachers", summary.totalTeachers.toString())
        MetricBadge("Active Courses", summary.totalActiveCourses.toString())
        MetricBadge("Administrators", summary.totalAdministrators.toString())
        MetricBadge("Submitted Evals", summary.totalSubmittedEvaluations.toString())
        MetricBadge("Pending Evals", summary.totalPendingEvaluations.toString())
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "University Evaluation Completion",
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
private fun MetricBadge(label: String, value: String) {
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
private fun DepartmentPerformanceComparisonCard(summary: SuperAdminDashboardSummary) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("super_admin_dept_comparison_card"),
    shape = RoundedCornerShape(16.dp),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "Department Performance Comparison (${summary.departmentComparisons.size})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      if (summary.departmentComparisons.isEmpty()) {
        Text(
          text = "No department analytics available yet.",
          style = MaterialTheme.typography.bodySmall,
        )
      } else {
        summary.departmentComparisons.forEach { row ->
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
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = row.departmentName,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = String.format(Locale.US, "Avg %.2f ★ (%d%%)", row.averageScore, row.completionPercentage),
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                )
              }
              Text(
                text = row.facultyName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "Students: ${row.studentCount} • Teachers: ${row.activeTeacherCount} • Courses: ${row.activeCourseCount} • Submitted: ${row.submittedEvaluationCount}",
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
private fun AuditLogSectionCard(summary: SuperAdminDashboardSummary) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("super_admin_audit_logs_card"),
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
        Icon(Icons.Default.History, contentDescription = null)
        Text(
          text = "Administrator Activity Audit Logs (${summary.recentAuditLogs.size})",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
      }
      if (summary.recentAuditLogs.isEmpty()) {
        Text(
          text = "No administrative actions recorded yet.",
          style = MaterialTheme.typography.bodySmall,
        )
      } else {
        summary.recentAuditLogs.take(10).forEach { log ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "${log.action} • ${log.actorRole.uppercase()} (${log.actorUid})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
              )
              if (log.exportFormat.isNotBlank()) {
                Text(
                  text = "Format: ${log.exportFormat} • Scope: ${log.reportScope} • Records: ${log.exportedRecordCount} • Filters: ${log.selectedFilters}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.SemiBold,
                )
              }
              Text(
                text = log.details,
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SuperAdminManageAdminsSection(
  summary: SuperAdminDashboardSummary,
  isMutating: Boolean,
  onCreateDepartmentAdmin: (String, String, String, String, String) -> Unit,
  onToggleAdminStatus: (AdminAccountRecord) -> Unit,
  onReassignAdmin: (AdminAccountRecord) -> Unit,
) {
  val faculties =
    summary.faculties.ifEmpty {
      listOf(
        com.example.data.model.Faculty(
          id = "fac_eng_tech",
          name = "College of Engineering and Technology (CET)",
          isActive = true,
        ),
        com.example.data.model.Faculty(
          id = "fac_med_health",
          name = "College of Medicine and Health Sciences (CMHS)",
          isActive = true,
        ),
      )
    }
  val allDepartments =
    summary.departments.ifEmpty {
      listOf(
        com.example.data.model.Department(
          id = "dep_se",
          name = "Software Engineering (SEng)",
          facultyId = "fac_eng_tech",
          isActive = true,
        ),
        com.example.data.model.Department(
          id = "dep_cs",
          name = "Computer Science (CS)",
          facultyId = "fac_eng_tech",
          isActive = true,
        ),
        com.example.data.model.Department(
          id = "dep_nurs",
          name = "Comprehensive Nursing (NURS)",
          facultyId = "fac_med_health",
          isActive = true,
        ),
      )
    }

  var adminUidInput by rememberSaveable { mutableStateOf("") }
  var fullNameInput by rememberSaveable { mutableStateOf("") }
  var emailInput by rememberSaveable { mutableStateOf("") }
  var selectedFacultyId by rememberSaveable {
    mutableStateOf(faculties.firstOrNull()?.id ?: "fac_eng_tech")
  }
  val filteredDepartments =
    remember(selectedFacultyId, allDepartments) {
      allDepartments.filter { it.facultyId == selectedFacultyId }
    }
  var selectedDepartmentId by rememberSaveable(selectedFacultyId) {
    mutableStateOf(filteredDepartments.firstOrNull()?.id ?: "dep_se")
  }
  var facultyDropdownExpanded by remember { mutableStateOf(false) }
  var deptDropdownExpanded by remember { mutableStateOf(false) }

  var searchQuery by rememberSaveable { mutableStateOf("") }
  var statusFilter by rememberSaveable { mutableStateOf("ALL") }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("create_department_admin_card"),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.AddModerator,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Create & Assign Department Admin",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        Text(
          text = "Provisions a Department Admin record at /admins/{uid} scoped to a single Faculty and Department without replacing your Super Admin session.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
          value = fullNameInput,
          onValueChange = { fullNameInput = it },
          label = { Text("Administrator Full Name") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("create_admin_fullname_input"),
        )

        OutlinedTextField(
          value = emailInput,
          onValueChange = { emailInput = it },
          label = { Text("Institutional Email Address") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("create_admin_email_input"),
        )

        OutlinedTextField(
          value = adminUidInput,
          onValueChange = { adminUidInput = it },
          label = { Text("Firebase Auth UID (or leave blank to generate ID)") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("create_admin_uid_input"),
        )

        // Faculty Selection Dropdown
        ExposedDropdownMenuBox(
          expanded = facultyDropdownExpanded,
          onExpandedChange = { facultyDropdownExpanded = !facultyDropdownExpanded },
        ) {
          val selectedFacName =
            faculties.firstOrNull { it.id == selectedFacultyId }?.name ?: selectedFacultyId
          OutlinedTextField(
            value = selectedFacName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Assigned Faculty") },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = facultyDropdownExpanded)
            },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
              .testTag("create_admin_faculty_dropdown"),
          )
          ExposedDropdownMenu(
            expanded = facultyDropdownExpanded,
            onDismissRequest = { facultyDropdownExpanded = false },
          ) {
            faculties.forEach { fac ->
              DropdownMenuItem(
                text = { Text(fac.name) },
                onClick = {
                  selectedFacultyId = fac.id
                  val firstDept =
                    allDepartments.firstOrNull { it.facultyId == fac.id }?.id.orEmpty()
                  selectedDepartmentId = firstDept
                  facultyDropdownExpanded = false
                },
              )
            }
          }
        }

        // Department Selection Dropdown (filtered by selectedFacultyId)
        ExposedDropdownMenuBox(
          expanded = deptDropdownExpanded,
          onExpandedChange = { deptDropdownExpanded = !deptDropdownExpanded },
        ) {
          val selectedDeptName =
            filteredDepartments.firstOrNull { it.id == selectedDepartmentId }?.name
              ?: selectedDepartmentId
          OutlinedTextField(
            value = selectedDeptName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Assigned Department") },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptDropdownExpanded)
            },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
              .testTag("create_admin_department_dropdown"),
          )
          ExposedDropdownMenu(
            expanded = deptDropdownExpanded,
            onDismissRequest = { deptDropdownExpanded = false },
          ) {
            filteredDepartments.forEach { dept ->
              DropdownMenuItem(
                text = { Text(dept.name) },
                onClick = {
                  selectedDepartmentId = dept.id
                  deptDropdownExpanded = false
                },
              )
            }
          }
        }

        Button(
          onClick = {
            onCreateDepartmentAdmin(
              adminUidInput,
              fullNameInput,
              emailInput,
              selectedFacultyId,
              selectedDepartmentId,
            )
            adminUidInput = ""
            fullNameInput = ""
            emailInput = ""
          },
          enabled = !isMutating && fullNameInput.isNotBlank() && emailInput.contains("@"),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("submit_create_department_admin_button"),
        ) {
          Text(if (isMutating) "Provisioning..." else "Create Department Admin")
        }
      }
    }

    // Administrator Directory & Search/Filter
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("administrator_directory_card"),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "Registered Administrators (${summary.administrators.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          label = { Text("Search by name, email, or department ID") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_search_input"),
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("ALL", "ACTIVE", "INACTIVE", "DEPT_ADMIN").forEach { filter ->
            FilterChip(
              selected = statusFilter == filter,
              onClick = { statusFilter = filter },
              label = { Text(filter) },
            )
          }
        }

        val matchingAdmins =
          summary.administrators.filter { record ->
            val matchesQuery =
              searchQuery.isBlank() ||
                record.fullName.contains(searchQuery, ignoreCase = true) ||
                record.email.contains(searchQuery, ignoreCase = true) ||
                record.departmentId.orEmpty().contains(searchQuery, ignoreCase = true)
            val matchesStatus =
              when (statusFilter) {
                "ACTIVE" -> record.isActive
                "INACTIVE" -> !record.isActive
                "DEPT_ADMIN" -> record.role == UserProfile.ROLE_DEPARTMENT_ADMIN
                else -> true
              }
            matchesQuery && matchesStatus
          }

        if (matchingAdmins.isEmpty()) {
          Text(
            text = "No administrators match the current filter.",
            style = MaterialTheme.typography.bodySmall,
          )
        } else {
          matchingAdmins.forEach { adminItem ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_record_row_${adminItem.uid}"),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = adminItem.fullName.ifBlank { adminItem.email },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                  )
                  Surface(
                    color =
                      if (adminItem.isActive) {
                        MaterialTheme.colorScheme.primaryContainer
                      } else {
                        MaterialTheme.colorScheme.errorContainer
                      },
                    shape = RoundedCornerShape(999.dp),
                  ) {
                    Text(
                      text = if (adminItem.isActive) "ACTIVE" else "INACTIVE",
                      style = MaterialTheme.typography.labelSmall,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                  }
                }

                Text(
                  text = "${adminItem.email} • Role: ${adminItem.role.uppercase()}",
                  style = MaterialTheme.typography.bodySmall,
                )
                if (!adminItem.departmentId.isNullOrBlank()) {
                  Text(
                    text = "Assigned Faculty: ${adminItem.facultyId} • Department: ${adminItem.departmentId}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                  )
                }

                if (adminItem.role == UserProfile.ROLE_DEPARTMENT_ADMIN) {
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                      onClick = { onToggleAdminStatus(adminItem) },
                      modifier = Modifier.testTag("toggle_admin_status_${adminItem.uid}"),
                    ) {
                      Text(if (adminItem.isActive) "Deactivate" else "Activate")
                    }
                    OutlinedButton(
                      onClick = { onReassignAdmin(adminItem) },
                      modifier = Modifier.testTag("reassign_admin_dept_${adminItem.uid}"),
                    ) {
                      Text("Change Department")
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ReassignDepartmentAdminDialog(
  targetAdmin: AdminAccountRecord,
  summary: SuperAdminDashboardSummary,
  onDismiss: () -> Unit,
  onConfirm: (String, String) -> Unit,
) {
  val departments = summary.departments
  var selectedDeptId by rememberSaveable {
    mutableStateOf(targetAdmin.departmentId ?: departments.firstOrNull()?.id.orEmpty())
  }
  val selectedDept = departments.firstOrNull { it.id == selectedDeptId }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Reassign Department Admin") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Select the new department for ${targetAdmin.fullName} (${targetAdmin.email}):")
        departments.forEach { dept ->
          FilterChip(
            selected = selectedDeptId == dept.id,
            onClick = { selectedDeptId = dept.id },
            label = { Text("${dept.name} (${dept.id})") },
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (selectedDept != null) {
            onConfirm(selectedDept.facultyId, selectedDept.id)
          }
        },
        enabled = selectedDept != null,
      ) {
        Text("Save Assignment")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

@Composable
private fun SuperAdminFacultiesSection(
  summary: SuperAdminDashboardSummary,
  isMutating: Boolean,
  onSaveFaculty: (String, String, String) -> Unit,
) {
  var idInput by rememberSaveable { mutableStateOf("") }
  var nameInput by rememberSaveable { mutableStateOf("") }
  var descInput by rememberSaveable { mutableStateOf("") }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "Create or Update Faculty",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
          value = idInput,
          onValueChange = { idInput = it },
          label = { Text("Faculty ID (e.g. fac_eng_tech)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = nameInput,
          onValueChange = { nameInput = it },
          label = { Text("Faculty Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = descInput,
          onValueChange = { descInput = it },
          label = { Text("Description") },
          modifier = Modifier.fillMaxWidth(),
        )
        Button(
          onClick = {
            onSaveFaculty(idInput, nameInput, descInput)
            idInput = ""
            nameInput = ""
            descInput = ""
          },
          enabled = !isMutating && idInput.isNotBlank() && nameInput.isNotBlank(),
        ) {
          Text("Save Faculty")
        }
      }
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "Faculties (${summary.faculties.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        summary.faculties.forEach { fac ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(fac.name, fontWeight = FontWeight.Bold)
              Text("ID: ${fac.id} • Active: ${fac.isActive}", style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SuperAdminDepartmentsSection(
  summary: SuperAdminDashboardSummary,
  isMutating: Boolean,
  onSaveDepartment: (String, String, String, String) -> Unit,
) {
  var idInput by rememberSaveable { mutableStateOf("") }
  var nameInput by rememberSaveable { mutableStateOf("") }
  var facIdInput by rememberSaveable {
    mutableStateOf(summary.faculties.firstOrNull()?.id ?: "fac_eng_tech")
  }
  var descInput by rememberSaveable { mutableStateOf("") }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "Create or Update Department",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
          value = idInput,
          onValueChange = { idInput = it },
          label = { Text("Department ID (e.g. dep_se)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = nameInput,
          onValueChange = { nameInput = it },
          label = { Text("Department Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = facIdInput,
          onValueChange = { facIdInput = it },
          label = { Text("Parent Faculty ID") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = descInput,
          onValueChange = { descInput = it },
          label = { Text("Description") },
          modifier = Modifier.fillMaxWidth(),
        )
        Button(
          onClick = {
            onSaveDepartment(idInput, nameInput, facIdInput, descInput)
            idInput = ""
            nameInput = ""
            descInput = ""
          },
          enabled = !isMutating && idInput.isNotBlank() && nameInput.isNotBlank() && facIdInput.isNotBlank(),
        ) {
          Text("Save Department")
        }
      }
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "Departments (${summary.departments.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        summary.departments.forEach { dept ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(dept.name, fontWeight = FontWeight.Bold)
              Text(
                "ID: ${dept.id} • Faculty: ${dept.facultyId} • Active: ${dept.isActive}",
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }
      }
    }
  }
}
