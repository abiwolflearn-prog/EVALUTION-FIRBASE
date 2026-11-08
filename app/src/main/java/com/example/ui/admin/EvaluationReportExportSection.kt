package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.export.EvaluationReportExporter
import com.example.data.model.DepartmentAdminDashboardSummary
import com.example.data.model.EvaluationReportExportFilter
import com.example.data.model.ExportFormat
import com.example.data.model.GeneratedReportFile
import com.example.data.model.SuperAdminDashboardSummary
import com.example.ui.theme.WcuGold500
import com.example.ui.theme.WcuNavy900
import com.example.ui.theme.WcuWhite
import java.util.Locale

private enum class ExportDateWindow(val label: String, val durationMillis: Long?) {
  ALL_TIME("All Dates", null),
  LAST_7_DAYS("Last 7 Days", 7L * 24L * 3600L * 1000L),
  LAST_30_DAYS("Last 30 Days", 30L * 24L * 3600L * 1000L),
  LAST_180_DAYS("Current Semester (180d)", 180L * 24L * 3600L * 1000L),
}

/**
 * Department Admin Evaluation Reports & Export Section (Phase 5).
 *
 * Scoped strictly to `summary.adminRecord.departmentId` and `summary.adminRecord.facultyId`.
 * Provides:
 * - PDF export
 * - Excel (.xlsx) export
 * - Teacher filter
 * - Course filter
 * - Semester filter
 * - Date range filter
 * - Department summary
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DepartmentAdminEvaluationReportSection(
  summary: DepartmentAdminDashboardSummary,
  isExporting: Boolean,
  lastExportedReport: GeneratedReportFile?,
  onExportReport: (ExportFormat, EvaluationReportExportFilter) -> Unit,
) {
  val context = LocalContext.current
  var selectedTeacherId by rememberSaveable { mutableStateOf("") }
  var selectedCourseId by rememberSaveable { mutableStateOf("") }
  var selectedSemesterId by rememberSaveable { mutableStateOf("") }
  var selectedDateWindow by rememberSaveable { mutableStateOf(ExportDateWindow.ALL_TIME) }

  var teacherDropdownExpanded by remember { mutableStateOf(false) }
  var courseDropdownExpanded by remember { mutableStateOf(false) }
  var semesterDropdownExpanded by remember { mutableStateOf(false) }

  val availableSemesters =
    remember(summary.departmentCourses, summary.departmentEvaluations) {
      (summary.departmentCourses.map { it.semesterId } +
          summary.departmentEvaluations.map { it.semesterId })
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
    }

  fun buildCurrentFilter(): EvaluationReportExportFilter {
    val now = System.currentTimeMillis()
    val startMillis = selectedDateWindow.durationMillis?.let { now - it }
    return EvaluationReportExportFilter(
      facultyId = summary.adminRecord.facultyId,
      departmentId = summary.adminRecord.departmentId,
      teacherId = selectedTeacherId.takeIf { it.isNotBlank() },
      courseId = selectedCourseId.takeIf { it.isNotBlank() },
      semesterId = selectedSemesterId.takeIf { it.isNotBlank() },
      startDateMillis = startMillis,
      endDateMillis = if (startMillis != null) now else null,
    )
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("dept_admin_reports_export_section"),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // 1. Department Report Header & Scope Verification Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = WcuNavy900),
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
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = WcuGold500,
          )
          Text(
            text = "Department Evaluation Reports & Export",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = WcuWhite,
          )
        }
        Text(
          text = "Generate printable Wachemo University PDF reports and structured Excel (.xlsx) spreadsheets for ${summary.departmentDisplayName}.",
          style = MaterialTheme.typography.bodySmall,
          color = WcuWhite.copy(alpha = 0.85f),
        )
        Surface(
          color = WcuGold500.copy(alpha = 0.18f),
          shape = RoundedCornerShape(10.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = WcuGold500,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "Trusted Scope: ${summary.departmentDisplayName} (${summary.adminRecord.departmentId}) • ${summary.facultyDisplayName}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = WcuGold500,
            )
          }
        }
      }
    }

    // 2. Department Filter Controls Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("dept_admin_export_filters_card"),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Report Filter Criteria (Department-Scoped)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
        }

        // Teacher Filter Dropdown
        val selectedTeacherLabel =
          summary.departmentTeachers
            .firstOrNull { it.id == selectedTeacherId }
            ?.let { "${it.fullName} (${it.id})" }
            ?: "All Department Teachers (${summary.departmentTeachers.size})"

        ExposedDropdownMenuBox(
          expanded = teacherDropdownExpanded,
          onExpandedChange = { teacherDropdownExpanded = !teacherDropdownExpanded },
        ) {
          OutlinedTextField(
            value = selectedTeacherLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Teacher Filter") },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = teacherDropdownExpanded)
            },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
              .testTag("dept_admin_filter_teacher_dropdown"),
          )
          ExposedDropdownMenu(
            expanded = teacherDropdownExpanded,
            onDismissRequest = { teacherDropdownExpanded = false },
          ) {
            DropdownMenuItem(
              text = { Text("All Department Teachers (${summary.departmentTeachers.size})") },
              onClick = {
                selectedTeacherId = ""
                teacherDropdownExpanded = false
              },
            )
            summary.departmentTeachers.forEach { teacher ->
              DropdownMenuItem(
                text = { Text("${teacher.fullName} (${teacher.id})") },
                onClick = {
                  selectedTeacherId = teacher.id
                  teacherDropdownExpanded = false
                },
              )
            }
          }
        }

        // Course Filter Dropdown
        val selectedCourseLabel =
          summary.departmentCourses
            .firstOrNull { it.id == selectedCourseId }
            ?.let { "${it.courseCode} — ${it.courseName}" }
            ?: "All Department Courses (${summary.departmentCourses.size})"

        ExposedDropdownMenuBox(
          expanded = courseDropdownExpanded,
          onExpandedChange = { courseDropdownExpanded = !courseDropdownExpanded },
        ) {
          OutlinedTextField(
            value = selectedCourseLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Course Filter") },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded)
            },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
              .testTag("dept_admin_filter_course_dropdown"),
          )
          ExposedDropdownMenu(
            expanded = courseDropdownExpanded,
            onDismissRequest = { courseDropdownExpanded = false },
          ) {
            DropdownMenuItem(
              text = { Text("All Department Courses (${summary.departmentCourses.size})") },
              onClick = {
                selectedCourseId = ""
                courseDropdownExpanded = false
              },
            )
            summary.departmentCourses.forEach { course ->
              DropdownMenuItem(
                text = { Text("${course.courseCode} — ${course.courseName}") },
                onClick = {
                  selectedCourseId = course.id
                  courseDropdownExpanded = false
                },
              )
            }
          }
        }

        // Semester Filter Dropdown
        val selectedSemesterLabel =
          selectedSemesterId.ifBlank { "All Semesters (${availableSemesters.size.coerceAtLeast(1)})" }

        ExposedDropdownMenuBox(
          expanded = semesterDropdownExpanded,
          onExpandedChange = { semesterDropdownExpanded = !semesterDropdownExpanded },
        ) {
          OutlinedTextField(
            value = selectedSemesterLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Semester Filter") },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = semesterDropdownExpanded)
            },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
              .testTag("dept_admin_filter_semester_dropdown"),
          )
          ExposedDropdownMenu(
            expanded = semesterDropdownExpanded,
            onDismissRequest = { semesterDropdownExpanded = false },
          ) {
            DropdownMenuItem(
              text = { Text("All Semesters") },
              onClick = {
                selectedSemesterId = ""
                semesterDropdownExpanded = false
              },
            )
            availableSemesters.forEach { semId ->
              DropdownMenuItem(
                text = { Text(semId) },
                onClick = {
                  selectedSemesterId = semId
                  semesterDropdownExpanded = false
                },
              )
            }
          }
        }

        // Date Range Filter Chips
        Text(
          text = "Date Range Filter",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ExportDateWindow.entries.forEach { window ->
            FilterChip(
              selected = selectedDateWindow == window,
              onClick = { selectedDateWindow = window },
              label = { Text(window.label) },
              modifier = Modifier.testTag("dept_admin_date_chip_${window.name.lowercase()}"),
            )
          }
        }

        // Export Action Buttons (PDF & Excel)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = { onExportReport(ExportFormat.PDF, buildCurrentFilter()) },
            enabled = !isExporting,
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .testTag("dept_admin_export_pdf_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            if (isExporting) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
              )
            } else {
              Icon(Icons.Default.PictureAsPdf, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Export PDF")
            }
          }

          OutlinedButton(
            onClick = { onExportReport(ExportFormat.XLSX, buildCurrentFilter()) },
            enabled = !isExporting,
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .testTag("dept_admin_export_excel_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Default.TableChart, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Export Excel (.xlsx)")
          }
        }
      }
    }

    // 3. Generated Report File Card (if available)
    if (lastExportedReport != null) {
      ExportedReportFileCard(
        reportFile = lastExportedReport,
        cardTestTag = "dept_admin_last_exported_report_card",
        shareButtonTestTag = "dept_admin_share_report_button",
        onShare = {
          EvaluationReportExporter.shareGeneratedReport(context, lastExportedReport)
        },
      )
    }
  }
}

/**
 * Super Admin University-Wide Evaluation Reports & Export Section (Phase 5).
 *
 * Supports university-wide or filtered exports across:
 * - Faculty
 * - Department
 * - Teacher
 * - Course
 * - Semester
 * - Date range
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SuperAdminEvaluationReportSection(
  summary: SuperAdminDashboardSummary,
  isExporting: Boolean,
  lastExportedReport: GeneratedReportFile?,
  onExportReport: (ExportFormat, EvaluationReportExportFilter) -> Unit,
) {
  val context = LocalContext.current
  var selectedFacultyId by rememberSaveable { mutableStateOf("") }
  var selectedDepartmentId by rememberSaveable { mutableStateOf("") }
  var selectedTeacherId by rememberSaveable { mutableStateOf("") }
  var selectedCourseId by rememberSaveable { mutableStateOf("") }
  var selectedSemesterId by rememberSaveable { mutableStateOf("") }
  var selectedDateWindow by rememberSaveable { mutableStateOf(ExportDateWindow.ALL_TIME) }

  var facultyDropdownExpanded by remember { mutableStateOf(false) }
  var deptDropdownExpanded by remember { mutableStateOf(false) }
  var teacherDropdownExpanded by remember { mutableStateOf(false) }
  var courseDropdownExpanded by remember { mutableStateOf(false) }
  var semesterDropdownExpanded by remember { mutableStateOf(false) }

  val filteredDepartments =
    remember(summary.departments, selectedFacultyId) {
      if (selectedFacultyId.isBlank()) summary.departments
      else summary.departments.filter { it.facultyId == selectedFacultyId }
    }

  val filteredTeachers =
    remember(summary.teachers, selectedFacultyId, selectedDepartmentId) {
      summary.teachers.filter { teacher ->
        (selectedFacultyId.isBlank() || teacher.facultyId == selectedFacultyId) &&
          (selectedDepartmentId.isBlank() || teacher.departmentId == selectedDepartmentId)
      }
    }

  val filteredCourses =
    remember(summary.courses, selectedDepartmentId, selectedTeacherId) {
      summary.courses.filter { course ->
        (selectedDepartmentId.isBlank() || course.departmentId == selectedDepartmentId) &&
          (selectedTeacherId.isBlank() || course.teacherId == selectedTeacherId)
      }
    }

  fun buildCurrentFilter(): EvaluationReportExportFilter {
    val now = System.currentTimeMillis()
    val startMillis = selectedDateWindow.durationMillis?.let { now - it }
    return EvaluationReportExportFilter(
      facultyId = selectedFacultyId.takeIf { it.isNotBlank() },
      departmentId = selectedDepartmentId.takeIf { it.isNotBlank() },
      teacherId = selectedTeacherId.takeIf { it.isNotBlank() },
      courseId = selectedCourseId.takeIf { it.isNotBlank() },
      semesterId = selectedSemesterId.takeIf { it.isNotBlank() },
      startDateMillis = startMillis,
      endDateMillis = if (startMillis != null) now else null,
    )
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("super_admin_reports_export_section"),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // 1. University-Wide Report Header & Executive Summary Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = WcuNavy900),
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
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = WcuGold500,
          )
          Text(
            text = "University-Wide Evaluation Reports & Export",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = WcuWhite,
          )
        }
        Text(
          text = "Export official Wachemo University PDF reports and multi-sheet Excel (.xlsx) workbooks across all ${summary.totalFaculties} faculties and ${summary.totalDepartments} departments.",
          style = MaterialTheme.typography.bodySmall,
          color = WcuWhite.copy(alpha = 0.85f),
        )
        Text(
          text = "Overall University Rating: ${String.format(Locale.US, "%.2f / 5.00", summary.overallUniversityRating)} • Total Evaluations: ${summary.totalSubmittedEvaluations} • Completion: ${summary.completionPercentage}%",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = WcuGold500,
        )
      }
    }

    // 2. Multi-Dimension Filter Card (Faculty, Department, Teacher, Course, Semester, Date Range)
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("super_admin_export_filters_card"),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "University & Departmental Report Filters",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
        }

        // Faculty Filter
        val selectedFacultyLabel =
          summary.faculties
            .firstOrNull { it.id == selectedFacultyId }
            ?.let { "${it.name} (${it.id})" }
            ?: "All University Faculties (${summary.faculties.size})"

        ExposedDropdownMenuBox(
          expanded = facultyDropdownExpanded,
          onExpandedChange = { facultyDropdownExpanded = !facultyDropdownExpanded },
        ) {
          OutlinedTextField(
            value = selectedFacultyLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Faculty Filter") },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = facultyDropdownExpanded)
            },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
              .testTag("super_admin_filter_faculty_dropdown"),
          )
          ExposedDropdownMenu(
            expanded = facultyDropdownExpanded,
            onDismissRequest = { facultyDropdownExpanded = false },
          ) {
            DropdownMenuItem(
              text = { Text("All University Faculties (${summary.faculties.size})") },
              onClick = {
                selectedFacultyId = ""
                selectedDepartmentId = ""
                selectedTeacherId = ""
                selectedCourseId = ""
                facultyDropdownExpanded = false
              },
            )
            summary.faculties.forEach { fac ->
              DropdownMenuItem(
                text = { Text("${fac.name} (${fac.id})") },
                onClick = {
                  selectedFacultyId = fac.id
                  selectedDepartmentId = ""
                  selectedTeacherId = ""
                  selectedCourseId = ""
                  facultyDropdownExpanded = false
                },
              )
            }
          }
        }

        // Department Filter
        val selectedDeptLabel =
          filteredDepartments
            .firstOrNull { it.id == selectedDepartmentId }
            ?.let { "${it.name} (${it.id})" }
            ?: "All Departments (${filteredDepartments.size})"

        ExposedDropdownMenuBox(
          expanded = deptDropdownExpanded,
          onExpandedChange = { deptDropdownExpanded = !deptDropdownExpanded },
        ) {
          OutlinedTextField(
            value = selectedDeptLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Department Filter") },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptDropdownExpanded)
            },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
              .testTag("super_admin_filter_department_dropdown"),
          )
          ExposedDropdownMenu(
            expanded = deptDropdownExpanded,
            onDismissRequest = { deptDropdownExpanded = false },
          ) {
            DropdownMenuItem(
              text = { Text("All Departments (${filteredDepartments.size})") },
              onClick = {
                selectedDepartmentId = ""
                selectedTeacherId = ""
                selectedCourseId = ""
                deptDropdownExpanded = false
              },
            )
            filteredDepartments.forEach { dept ->
              DropdownMenuItem(
                text = { Text("${dept.name} (${dept.id})") },
                onClick = {
                  selectedDepartmentId = dept.id
                  selectedTeacherId = ""
                  selectedCourseId = ""
                  deptDropdownExpanded = false
                },
              )
            }
          }
        }

        // Teacher Filter
        val selectedTeacherLabel =
          filteredTeachers
            .firstOrNull { it.id == selectedTeacherId }
            ?.let { "${it.fullName} (${it.id})" }
            ?: "All Teachers (${filteredTeachers.size})"

        ExposedDropdownMenuBox(
          expanded = teacherDropdownExpanded,
          onExpandedChange = { teacherDropdownExpanded = !teacherDropdownExpanded },
        ) {
          OutlinedTextField(
            value = selectedTeacherLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Teacher Filter") },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = teacherDropdownExpanded)
            },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
              .testTag("super_admin_filter_teacher_dropdown"),
          )
          ExposedDropdownMenu(
            expanded = teacherDropdownExpanded,
            onDismissRequest = { teacherDropdownExpanded = false },
          ) {
            DropdownMenuItem(
              text = { Text("All Teachers (${filteredTeachers.size})") },
              onClick = {
                selectedTeacherId = ""
                selectedCourseId = ""
                teacherDropdownExpanded = false
              },
            )
            filteredTeachers.forEach { teacher ->
              DropdownMenuItem(
                text = { Text("${teacher.fullName} (${teacher.id})") },
                onClick = {
                  selectedTeacherId = teacher.id
                  selectedCourseId = ""
                  teacherDropdownExpanded = false
                },
              )
            }
          }
        }

        // Course Filter
        val selectedCourseLabel =
          filteredCourses
            .firstOrNull { it.id == selectedCourseId }
            ?.let { "${it.courseCode} — ${it.courseName}" }
            ?: "All Courses (${filteredCourses.size})"

        ExposedDropdownMenuBox(
          expanded = courseDropdownExpanded,
          onExpandedChange = { courseDropdownExpanded = !courseDropdownExpanded },
        ) {
          OutlinedTextField(
            value = selectedCourseLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Course Filter") },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded)
            },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
              .testTag("super_admin_filter_course_dropdown"),
          )
          ExposedDropdownMenu(
            expanded = courseDropdownExpanded,
            onDismissRequest = { courseDropdownExpanded = false },
          ) {
            DropdownMenuItem(
              text = { Text("All Courses (${filteredCourses.size})") },
              onClick = {
                selectedCourseId = ""
                courseDropdownExpanded = false
              },
            )
            filteredCourses.forEach { course ->
              DropdownMenuItem(
                text = { Text("${course.courseCode} — ${course.courseName}") },
                onClick = {
                  selectedCourseId = course.id
                  courseDropdownExpanded = false
                },
              )
            }
          }
        }

        // Semester Filter
        val selectedSemesterLabel =
          summary.semesters
            .firstOrNull { it.id == selectedSemesterId }
            ?.let { "${it.name} (${it.academicYear})" }
            ?: "All Semesters (${summary.semesters.size.coerceAtLeast(1)})"

        ExposedDropdownMenuBox(
          expanded = semesterDropdownExpanded,
          onExpandedChange = { semesterDropdownExpanded = !semesterDropdownExpanded },
        ) {
          OutlinedTextField(
            value = selectedSemesterLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Semester Filter") },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = semesterDropdownExpanded)
            },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
              .testTag("super_admin_filter_semester_dropdown"),
          )
          ExposedDropdownMenu(
            expanded = semesterDropdownExpanded,
            onDismissRequest = { semesterDropdownExpanded = false },
          ) {
            DropdownMenuItem(
              text = { Text("All Semesters") },
              onClick = {
                selectedSemesterId = ""
                semesterDropdownExpanded = false
              },
            )
            summary.semesters.forEach { sem ->
              DropdownMenuItem(
                text = { Text("${sem.name} (${sem.academicYear})") },
                onClick = {
                  selectedSemesterId = sem.id
                  semesterDropdownExpanded = false
                },
              )
            }
          }
        }

        // Date Range Filter Chips
        Text(
          text = "Date Range Filter",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ExportDateWindow.entries.forEach { window ->
            FilterChip(
              selected = selectedDateWindow == window,
              onClick = { selectedDateWindow = window },
              label = { Text(window.label) },
              modifier = Modifier.testTag("super_admin_date_chip_${window.name.lowercase()}"),
            )
          }
        }

        // Export Action Buttons (PDF & Excel)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = { onExportReport(ExportFormat.PDF, buildCurrentFilter()) },
            enabled = !isExporting,
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .testTag("super_admin_export_pdf_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            if (isExporting) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
              )
            } else {
              Icon(Icons.Default.PictureAsPdf, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Export PDF")
            }
          }

          OutlinedButton(
            onClick = { onExportReport(ExportFormat.XLSX, buildCurrentFilter()) },
            enabled = !isExporting,
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .testTag("super_admin_export_excel_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Default.TableChart, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Export Excel (.xlsx)")
          }
        }
      }
    }

    // 3. Generated Report File Card (if available)
    if (lastExportedReport != null) {
      ExportedReportFileCard(
        reportFile = lastExportedReport,
        cardTestTag = "super_admin_last_exported_report_card",
        shareButtonTestTag = "super_admin_share_report_button",
        onShare = {
          EvaluationReportExporter.shareGeneratedReport(context, lastExportedReport)
        },
      )
    }
  }
}

@Composable
private fun ExportedReportFileCard(
  reportFile: GeneratedReportFile,
  cardTestTag: String,
  shareButtonTestTag: String,
  onShare: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag(cardTestTag),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
    ),
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
          imageVector = Icons.Default.CheckCircle,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "Report Ready: ${reportFile.format.displayName}",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
      Text(
        text = "File: ${reportFile.fileName} (${reportFile.byteSize} bytes)",
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      Text(
        text = "Scope: ${reportFile.reportScope} • Exported Records: ${reportFile.exportedRecordCount} • Generated: ${reportFile.generatedAtIso}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      Button(
        onClick = onShare,
        modifier = Modifier
          .fillMaxWidth()
          .testTag(shareButtonTestTag),
        shape = RoundedCornerShape(10.dp),
      ) {
        Icon(Icons.Default.Share, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Share / Save ${reportFile.format.code} Report")
      }
    }
  }
}
