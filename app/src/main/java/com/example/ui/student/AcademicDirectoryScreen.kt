package com.example.ui.student

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Course
import com.example.data.model.Department
import com.example.data.model.Faculty
import com.example.data.model.Teacher
import com.example.data.model.TeacherEvaluation
import com.example.data.model.UserProfile
import com.example.data.model.WcuAcademicCatalogFallback
import com.example.data.repository.AcademicDirectorySnapshot
import com.example.data.repository.TeacherCourseAssignmentItem
import com.example.data.repository.TeacherProfileDetails
import com.example.ui.theme.WcuBlue100
import com.example.ui.theme.WcuGold500
import com.example.ui.theme.WcuNavy800
import com.example.ui.theme.WcuNavy900
import com.example.ui.theme.WcuWhite

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AcademicDirectoryScreen(
  studentProfile: UserProfile,
  onBackHome: () -> Unit,
  onStartCourseEvaluation: (courseId: String, teacherId: String) -> Unit,
  directoryViewModel: AcademicDirectoryViewModel =
    viewModel(
      key = "academic_directory_${studentProfile.uid}",
      factory = AcademicDirectoryViewModel.provideFactory(studentProfile),
    ),
) {
  val uiState by directoryViewModel.uiState.collectAsStateWithLifecycle()
  val selectedSection by directoryViewModel.selectedSection.collectAsStateWithLifecycle()
  val searchQuery by directoryViewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedFacultyId by directoryViewModel.selectedFacultyId.collectAsStateWithLifecycle()
  val selectedDepartmentId by directoryViewModel.selectedDepartmentId.collectAsStateWithLifecycle()
  val teacherDetailState by directoryViewModel.teacherDetailState.collectAsStateWithLifecycle()

  // If Teacher Profile Detail screen is open, intercept back button to close detail view first
  if (teacherDetailState !is TeacherDetailUiState.Idle) {
    BackHandler { directoryViewModel.closeTeacherDetails() }
    TeacherProfileDetailScreen(
      detailState = teacherDetailState,
      onBackToDirectory = directoryViewModel::closeTeacherDetails,
      onStartCourseEvaluation = { courseId, teacherId ->
        directoryViewModel.closeTeacherDetails()
        onStartCourseEvaluation(courseId, teacherId)
      },
    )
    return
  }

  when (val state = uiState) {
    AcademicDirectoryUiState.Loading -> {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp)
          .testTag("academic_directory_loading"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Loading faculties, departments, teachers, and courses from Cloud Firestore...",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    is AcademicDirectoryUiState.Error -> {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 600.dp)
          .padding(24.dp)
          .testTag("academic_directory_error"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "Unable to Load Academic Directory",
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
          onClick = directoryViewModel::refreshDirectory,
          modifier = Modifier.testTag("academic_directory_retry_button"),
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Retry")
        }
      }
    }

    is AcademicDirectoryUiState.Success -> {
      val snapshot = state.snapshot
      val facultiesById = snapshot.faculties.associateBy { it.id }
      val departmentsById = snapshot.departments.associateBy { it.id }
      val teachersById = snapshot.teachers.associateBy { it.id }

      val normalizedQuery = searchQuery.trim().lowercase()

      val filteredFaculties =
        snapshot.faculties.filter { fac ->
          fac.isActive &&
            (normalizedQuery.isEmpty() ||
              fac.name.lowercase().contains(normalizedQuery) ||
              fac.description.lowercase().contains(normalizedQuery))
        }

      val filteredDepartments =
        snapshot.departments.filter { dept ->
          dept.isActive &&
            (selectedFacultyId == null || dept.facultyId == selectedFacultyId) &&
            (normalizedQuery.isEmpty() ||
              dept.name.lowercase().contains(normalizedQuery) ||
              dept.description.lowercase().contains(normalizedQuery))
        }

      val filteredTeachers =
        directoryViewModel.repository.filterTeachers(
          teachers = snapshot.teachers,
          searchQuery = searchQuery,
          facultyId = selectedFacultyId,
          departmentId = selectedDepartmentId,
        )

      val filteredCourses =
        directoryViewModel.repository.filterCourses(
          courses = snapshot.courses,
          teachersById = teachersById,
          departmentsById = departmentsById,
          searchQuery = searchQuery,
          facultyId = selectedFacultyId,
          departmentId = selectedDepartmentId,
        )

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 640.dp)
          .padding(horizontal = 16.dp)
          .testTag("academic_directory_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Directory Header Banner
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
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "WCU Academic Directory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WcuWhite,
                  )
                  Text(
                    text =
                      "${snapshot.faculties.size} Faculties • ${snapshot.departments.size} Departments • ${snapshot.teachers.size} Teachers • ${snapshot.courses.size} Courses",
                    style = MaterialTheme.typography.labelSmall,
                    color = WcuBlue100,
                  )
                }
                IconButton(
                  onClick = directoryViewModel::refreshDirectory,
                  modifier = Modifier.testTag("directory_refresh_button"),
                ) {
                  Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Directory",
                    tint = WcuGold500,
                  )
                }
              }
            }
          }
        }

        // Sub-section Switcher: Faculties, Departments, Teachers, Courses
        item {
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            DirectorySubSection.entries.forEach { section ->
              FilterChip(
                selected = selectedSection == section,
                onClick = { directoryViewModel.selectSection(section) },
                label = { Text(section.label) },
                leadingIcon = {
                  Icon(
                    imageVector =
                      when (section) {
                        DirectorySubSection.FACULTIES -> Icons.Default.AccountBalance
                        DirectorySubSection.DEPARTMENTS -> Icons.Default.Domain
                        DirectorySubSection.TEACHERS -> Icons.Default.Groups
                        DirectorySubSection.COURSES -> Icons.Default.Book
                      },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                },
                modifier = Modifier.testTag("directory_section_chip_${section.name.lowercase()}"),
              )
            }
          }
        }

        // Search and Filter Controls
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedTextField(
                value = searchQuery,
                onValueChange = directoryViewModel::updateSearchQuery,
                label = {
                  Text(
                    when (selectedSection) {
                      DirectorySubSection.FACULTIES -> "Search faculties..."
                      DirectorySubSection.DEPARTMENTS -> "Search departments..."
                      DirectorySubSection.TEACHERS -> "Search teachers by name, specialization, or email..."
                      DirectorySubSection.COURSES -> "Search courses by code, title, or teacher..."
                    }
                  )
                },
                leadingIcon = {
                  Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                  if (searchQuery.isNotEmpty()) {
                    IconButton(
                      onClick = { directoryViewModel.updateSearchQuery("") },
                      modifier = Modifier.testTag("directory_clear_search_button"),
                    ) {
                      Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                  }
                },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("directory_search_input"),
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  FilterChip(
                    selected = selectedDepartmentId == studentProfile.departmentId,
                    onClick = {
                      if (selectedDepartmentId == studentProfile.departmentId) {
                        directoryViewModel.clearAllFilters()
                      } else {
                        directoryViewModel.filterToStudentOwnDepartment()
                      }
                    },
                    label = { Text("My Department Only") },
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                      )
                    },
                    modifier = Modifier.testTag("directory_filter_my_department_chip"),
                  )
                }

                if (selectedFacultyId != null || selectedDepartmentId != null || searchQuery.isNotBlank()) {
                  TextButton(
                    onClick = directoryViewModel::clearAllFilters,
                    modifier = Modifier.testTag("directory_reset_filters_button"),
                  ) {
                    Text("Reset Filters")
                  }
                }
              }

              // Active Faculty & Department filter badges
              if (selectedFacultyId != null || selectedDepartmentId != null) {
                val facLabel =
                  selectedFacultyId?.let { id ->
                    WcuAcademicCatalogFallback.resolveFacultyName(facultiesById[id], id)
                  }
                val deptLabel =
                  selectedDepartmentId?.let { id ->
                    WcuAcademicCatalogFallback.resolveDepartmentName(departmentsById[id], id)
                  }
                Text(
                  text =
                    listOfNotNull(
                      facLabel?.let { "Faculty: $it" },
                      deptLabel?.let { "Department: $it" },
                    ).joinToString(" • "),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
          }
        }

        // Section-Specific Directory Lists
        when (selectedSection) {
          DirectorySubSection.FACULTIES -> {
            if (filteredFaculties.isEmpty()) {
              item {
                DirectoryEmptyStateCard(
                  title = "No Active Faculties Found",
                  message =
                    if (searchQuery.isNotBlank()) {
                      "No faculties match \"$searchQuery\"."
                    } else {
                      "No active faculty records are currently published in the /faculties Firestore collection."
                    },
                  onBackHome = onBackHome,
                )
              }
            } else {
              items(filteredFaculties, key = { "fac_${it.id}" }) { faculty ->
                val deptCount = snapshot.departments.count { it.facultyId == faculty.id }
                val teacherCount = snapshot.teachers.count { it.facultyId == faculty.id }
                FacultyDirectoryCard(
                  faculty = faculty,
                  departmentCount = deptCount,
                  teacherCount = teacherCount,
                  isSelected = selectedFacultyId == faculty.id,
                  onSelectFaculty = {
                    directoryViewModel.selectFacultyFilter(faculty.id)
                    directoryViewModel.selectSection(DirectorySubSection.DEPARTMENTS)
                  },
                )
              }
            }
          }

          DirectorySubSection.DEPARTMENTS -> {
            if (filteredDepartments.isEmpty()) {
              item {
                DirectoryEmptyStateCard(
                  title = "No Active Departments Found",
                  message =
                    if (selectedFacultyId != null || searchQuery.isNotBlank()) {
                      "No departments match the current faculty or search filter."
                    } else {
                      "No active department records are currently published in the /departments Firestore collection."
                    },
                  onBackHome = onBackHome,
                )
              }
            } else {
              items(filteredDepartments, key = { "dept_${it.id}" }) { department ->
                val facultyName =
                  WcuAcademicCatalogFallback.resolveFacultyName(
                    facultiesById[department.facultyId],
                    department.facultyId,
                  )
                val teacherCount = snapshot.teachers.count { it.departmentId == department.id }
                val courseCount = snapshot.courses.count { it.departmentId == department.id }
                DepartmentDirectoryCard(
                  department = department,
                  facultyName = facultyName,
                  teacherCount = teacherCount,
                  courseCount = courseCount,
                  isSelected = selectedDepartmentId == department.id,
                  onSelectDepartment = {
                    directoryViewModel.selectDepartmentFilter(department.id)
                    directoryViewModel.selectSection(DirectorySubSection.TEACHERS)
                  },
                )
              }
            }
          }

          DirectorySubSection.TEACHERS -> {
            if (filteredTeachers.isEmpty()) {
              item {
                DirectoryEmptyStateCard(
                  title = "No Active Teachers Found",
                  message =
                    if (selectedFacultyId != null || selectedDepartmentId != null || searchQuery.isNotBlank()) {
                      "No active teachers match your current search or faculty/department filters."
                    } else {
                      "No active teachers are currently registered in the /teachers Firestore collection."
                    },
                  onBackHome = onBackHome,
                )
              }
            } else {
              items(filteredTeachers, key = { "tch_${it.id}" }) { teacher ->
                val facultyName =
                  WcuAcademicCatalogFallback.resolveFacultyName(
                    facultiesById[teacher.facultyId],
                    teacher.facultyId,
                  )
                val departmentName =
                  WcuAcademicCatalogFallback.resolveDepartmentName(
                    departmentsById[teacher.departmentId],
                    teacher.departmentId,
                  )
                val courseCount = snapshot.courses.count { it.teacherId == teacher.id }
                TeacherDirectoryCard(
                  teacher = teacher,
                  facultyName = facultyName,
                  departmentName = departmentName,
                  assignedCourseCount = courseCount,
                  onClick = { directoryViewModel.openTeacherDetails(teacher.id) },
                )
              }
            }
          }

          DirectorySubSection.COURSES -> {
            if (filteredCourses.isEmpty()) {
              item {
                DirectoryEmptyStateCard(
                  title = "No Active Courses Found",
                  message =
                    if (selectedFacultyId != null || selectedDepartmentId != null || searchQuery.isNotBlank()) {
                      "No active courses match your current search or department filters."
                    } else {
                      "No active courses are currently published in the /courses Firestore collection."
                    },
                  onBackHome = onBackHome,
                )
              }
            } else {
              items(filteredCourses, key = { "crs_${it.id}" }) { course ->
                val teacher = teachersById[course.teacherId]
                val departmentName =
                  WcuAcademicCatalogFallback.resolveDepartmentName(
                    departmentsById[course.departmentId],
                    course.departmentId,
                  )
                val compositeKey =
                  TeacherEvaluation.buildCompositeId(
                    studentUid = studentProfile.uid,
                    teacherId = course.teacherId,
                    courseId = course.id,
                    semesterId = course.semesterId,
                  )
                val isAlreadyEvaluated = snapshot.evaluatedCompositeKeys.contains(compositeKey)
                val isEligibleDepartment =
                  course.departmentId == studentProfile.departmentId && teacher != null && teacher.isActive

                CourseDirectoryCard(
                  course = course,
                  teacherName = teacher?.fullName ?: "Instructor (${course.teacherId})",
                  departmentName = departmentName,
                  isAlreadyEvaluated = isAlreadyEvaluated,
                  isEligibleToEvaluate = isEligibleDepartment && !isAlreadyEvaluated,
                  onViewTeacher = {
                    if (course.teacherId.isNotBlank()) {
                      directoryViewModel.openTeacherDetails(course.teacherId)
                    }
                  },
                  onEvaluateCourse = {
                    if (isEligibleDepartment && !isAlreadyEvaluated) {
                      onStartCourseEvaluation(course.id, course.teacherId)
                    }
                  },
                )
              }
            }
          }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
      }
    }
  }
}

@Composable
private fun FacultyDirectoryCard(
  faculty: Faculty,
  departmentCount: Int,
  teacherCount: Int,
  isSelected: Boolean,
  onSelectFaculty: () -> Unit,
) {
  Card(
    onClick = onSelectFaculty,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("faculty_card_${faculty.id}"),
    shape = RoundedCornerShape(14.dp),
    colors =
      if (isSelected) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
      } else {
        CardDefaults.cardColors()
      },
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = faculty.name,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(999.dp),
        ) {
          Text(
            text = "$departmentCount Depts • $teacherCount Teachers",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }
      if (faculty.description.isNotBlank()) {
        Text(
          text = faculty.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun DepartmentDirectoryCard(
  department: Department,
  facultyName: String,
  teacherCount: Int,
  courseCount: Int,
  isSelected: Boolean,
  onSelectDepartment: () -> Unit,
) {
  Card(
    onClick = onSelectDepartment,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("department_card_${department.id}"),
    shape = RoundedCornerShape(14.dp),
    colors =
      if (isSelected) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
      } else {
        CardDefaults.cardColors()
      },
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = department.name,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(999.dp),
        ) {
          Text(
            text = "$teacherCount Teachers • $courseCount Courses",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }
      Text(
        text = facultyName,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
      )
      if (department.description.isNotBlank()) {
        Text(
          text = department.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun TeacherDirectoryCard(
  teacher: Teacher,
  facultyName: String,
  departmentName: String,
  assignedCourseCount: Int,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
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
          .size(48.dp)
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
          fontWeight = FontWeight.Medium,
        )
        Text(
          text = "$departmentName • $facultyName",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${teacher.email} • $assignedCourseCount Active Course(s)",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun CourseDirectoryCard(
  course: Course,
  teacherName: String,
  departmentName: String,
  isAlreadyEvaluated: Boolean,
  isEligibleToEvaluate: Boolean,
  onViewTeacher: () -> Unit,
  onEvaluateCourse: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("course_card_${course.id}"),
    shape = RoundedCornerShape(14.dp),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${course.courseCode} — ${course.courseName}",
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        if (isAlreadyEvaluated) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(999.dp),
          ) {
            Text(
              text = "Completed",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
      }

      Text(
        text = "Instructor: $teacherName",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = "Department: $departmentName • Academic Year: ${course.academicYear}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onViewTeacher,
          modifier = Modifier.weight(1f),
        ) {
          Text("Instructor Profile")
        }
        if (isEligibleToEvaluate) {
          Button(
            onClick = onEvaluateCourse,
            modifier = Modifier
              .weight(1f)
              .testTag("course_evaluate_button_${course.id}"),
          ) {
            Text("Evaluate")
          }
        }
      }
    }
  }
}

@Composable
private fun DirectoryEmptyStateCard(
  title: String,
  message: String,
  onBackHome: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("directory_empty_state_card"),
    shape = RoundedCornerShape(14.dp),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      OutlinedButton(onClick = onBackHome) {
        Text("Return to Student Dashboard")
      }
    }
  }
}

@Composable
private fun TeacherProfileDetailScreen(
  detailState: TeacherDetailUiState,
  onBackToDirectory: () -> Unit,
  onStartCourseEvaluation: (courseId: String, teacherId: String) -> Unit,
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 640.dp)
      .padding(horizontal = 16.dp)
      .testTag("teacher_profile_detail_screen"),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item { Spacer(modifier = Modifier.height(4.dp)) }

    item {
      OutlinedButton(
        onClick = onBackToDirectory,
        modifier = Modifier.testTag("teacher_detail_back_button"),
      ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Back to Academic Directory")
      }
    }

    when (detailState) {
      TeacherDetailUiState.Idle,
      TeacherDetailUiState.Loading -> {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              CircularProgressIndicator()
              Text(
                text = "Loading teacher profile and assigned courses from Firestore...",
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }
      }

      is TeacherDetailUiState.MissingOrInactive -> {
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("teacher_detail_missing_card"),
            shape = RoundedCornerShape(16.dp),
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = "Teacher Record Unavailable",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "The requested teacher profile (${detailState.teacherId}) is no longer active or was removed from Cloud Firestore.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      is TeacherDetailUiState.Error -> {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = "Error Loading Teacher Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
              )
              Text(
                text = detailState.message,
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }
      }

      is TeacherDetailUiState.Loaded -> {
        val details = detailState.details
        val teacher = details.teacher

        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("teacher_detail_header_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = WcuNavy900),
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box(
                  modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(WcuNavy800),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = teacher.fullName.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WcuGold500,
                  )
                }
                Column {
                  Text(
                    text = teacher.fullName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WcuWhite,
                  )
                  Text(
                    text = teacher.specialization,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WcuGold500,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }

              HorizontalDivider(color = WcuNavy800)

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.AccountBalance,
                  contentDescription = null,
                  tint = WcuBlue100,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = "Faculty: ${details.facultyDisplayName}",
                  style = MaterialTheme.typography.bodySmall,
                  color = WcuWhite,
                )
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.School,
                  contentDescription = null,
                  tint = WcuBlue100,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = "Department: ${details.departmentDisplayName}",
                  style = MaterialTheme.typography.bodySmall,
                  color = WcuWhite,
                )
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Email,
                  contentDescription = null,
                  tint = WcuBlue100,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = teacher.email,
                  style = MaterialTheme.typography.bodySmall,
                  color = WcuBlue100,
                )
              }
            }
          }
        }

        item {
          Text(
            text = "Assigned Active Courses (${details.assignedCourses.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
        }

        if (details.assignedCourses.isEmpty()) {
          item {
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp),
            ) {
              Text(
                text = "No active courses are currently assigned to this teacher in Cloud Firestore.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        } else {
          items(
            items = details.assignedCourses,
            key = { "assigned_course_${it.course.id}" },
          ) { item ->
            TeacherAssignedCourseCard(
              item = item,
              onEvaluate = {
                onStartCourseEvaluation(item.course.id, teacher.id)
              },
            )
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun TeacherAssignedCourseCard(
  item: TeacherCourseAssignmentItem,
  onEvaluate: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("teacher_assigned_course_card_${item.course.id}"),
    shape = RoundedCornerShape(14.dp),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = "${item.course.courseCode} — ${item.course.courseName}",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "${item.semesterName} • Academic Year ${item.course.academicYear}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      when {
        item.isAlreadyEvaluated -> {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "Already Evaluated — Duplicate Submission Prevented",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }

        !item.isStudentDepartmentMatch -> {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "Course belongs to department ${item.course.departmentId} (outside your registered department)",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        else -> {
          Button(
            onClick = onEvaluate,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("evaluate_course_button_${item.course.id}"),
            shape = RoundedCornerShape(10.dp),
          ) {
            Text("Evaluate Instructor for ${item.course.courseCode}")
          }
        }
      }
    }
  }
}
