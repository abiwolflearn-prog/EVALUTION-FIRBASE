package com.example.data.model

import com.example.data.repository.AcademicDirectorySnapshot
import com.example.data.repository.TeacherCourseAssignmentItem
import com.example.data.repository.TeacherProfileDetails
import kotlin.math.roundToInt

/**
 * Provides realistic Wachemo University academic records for interactive Demo Student,
 * Demo Department Admin, and Demo Super Admin sessions when exploring the app via the
 * Demo Login buttons without an active Google account on the emulator.
 */
object WcuDemoDataProvider {
  const val DEMO_STUDENT_UID = "demo_student_wcu"
  const val DEMO_DEPT_ADMIN_UID = "demo_dept_admin_se"
  const val DEMO_SUPER_ADMIN_UID = "demo_super_admin_wcu"

  fun isDemoUid(uid: String?): Boolean = uid != null && uid.startsWith("demo_")

  val demoStudentProfile =
    UserProfile(
      id = DEMO_STUDENT_UID,
      uid = DEMO_STUDENT_UID,
      fullName = "Abebe Kebede (Demo Student)",
      email = "abebe.kebede@wcu.edu.et",
      studentId = "WCU/1420/16",
      role = UserProfile.ROLE_STUDENT,
      facultyId = "fac_eng_tech",
      departmentId = "dep_se",
      yearOfStudy = 3,
      isActive = true,
    )

  val demoDepartmentAdminRecord =
    AdminAccountRecord(
      uid = DEMO_DEPT_ADMIN_UID,
      fullName = "Dr. Dawit Bekele (Demo Dept Admin)",
      email = "dawit.bekele@wcu.edu.et",
      role = UserProfile.ROLE_DEPARTMENT_ADMIN,
      facultyId = "fac_eng_tech",
      departmentId = "dep_se",
      isActive = true,
      createdBy = DEMO_SUPER_ADMIN_UID,
    )

  val demoDepartmentAdminProfile =
    UserProfile(
      id = DEMO_DEPT_ADMIN_UID,
      uid = DEMO_DEPT_ADMIN_UID,
      fullName = demoDepartmentAdminRecord.fullName,
      email = demoDepartmentAdminRecord.email,
      studentId = "",
      role = UserProfile.ROLE_DEPARTMENT_ADMIN,
      facultyId = "fac_eng_tech",
      departmentId = "dep_se",
      yearOfStudy = 1,
      isActive = true,
    )

  val demoSuperAdminRecord =
    AdminAccountRecord(
      uid = DEMO_SUPER_ADMIN_UID,
      fullName = "Prof. Tesfaye Worku (Demo Super Admin)",
      email = "abelbimrew868@gmail.com",
      role = UserProfile.ROLE_SUPER_ADMIN,
      facultyId = null,
      departmentId = null,
      isActive = true,
      createdBy = "WCU_BOOTSTRAP",
    )

  val demoSuperAdminProfile =
    UserProfile(
      id = DEMO_SUPER_ADMIN_UID,
      uid = DEMO_SUPER_ADMIN_UID,
      fullName = demoSuperAdminRecord.fullName,
      email = demoSuperAdminRecord.email,
      studentId = "",
      role = UserProfile.ROLE_SUPER_ADMIN,
      facultyId = "WCU-SUPER-ADMIN",
      departmentId = "UNIVERSITY-WIDE",
      yearOfStudy = 1,
      isActive = true,
    )

  val demoFaculties =
    listOf(
      Faculty(
        id = "fac_eng_tech",
        name = "College of Engineering and Technology (CET)",
        description = "Software, Computer Science, IT, Electrical & Civil Engineering",
        isActive = true,
      ),
      Faculty(
        id = "fac_nat_comp",
        name = "College of Natural and Computational Sciences (CNCS)",
        description = "Mathematics, Statistics, Physics, Biology & Chemistry",
        isActive = true,
      ),
      Faculty(
        id = "fac_med_health",
        name = "College of Medicine and Health Sciences (CMHS)",
        description = "Comprehensive Nursing, Public Health & Clinical Medicine",
        isActive = true,
      ),
      Faculty(
        id = "fac_bus_econ",
        name = "College of Business and Economics (CBE)",
        description = "Accounting, Finance, Management & Economics",
        isActive = true,
      ),
    )

  val demoDepartments =
    listOf(
      Department(
        id = "dep_se",
        name = "Software Engineering (SEng)",
        facultyId = "fac_eng_tech",
        description = "BSc in Software Engineering & Distributed Systems",
        isActive = true,
      ),
      Department(
        id = "dep_cs",
        name = "Computer Science (CS)",
        facultyId = "fac_eng_tech",
        description = "BSc in Computer Science & Artificial Intelligence",
        isActive = true,
      ),
      Department(
        id = "dep_it",
        name = "Information Technology (IT)",
        facultyId = "fac_eng_tech",
        description = "BSc in Information Technology & Network Administration",
        isActive = true,
      ),
      Department(
        id = "dep_nurs",
        name = "Comprehensive Nursing (NURS)",
        facultyId = "fac_med_health",
        description = "BSc in Comprehensive Clinical Nursing",
        isActive = true,
      ),
      Department(
        id = "dep_acct",
        name = "Accounting and Finance (ACCT)",
        facultyId = "fac_bus_econ",
        description = "BA in Accounting and Public Finance",
        isActive = true,
      ),
    )

  val demoTeachers =
    listOf(
      Teacher(
        id = "tch_se_01",
        fullName = "Dr. Kassahun Melesse",
        email = "kassahun.melesse@wcu.edu.et",
        facultyId = "fac_eng_tech",
        departmentId = "dep_se",
        specialization = "Associate Professor • Distributed Systems & Cloud Architecture",
        isActive = true,
      ),
      Teacher(
        id = "tch_se_02",
        fullName = "Inst. Bethlehem Tadesse",
        email = "bethlehem.tadesse@wcu.edu.et",
        facultyId = "fac_eng_tech",
        departmentId = "dep_se",
        specialization = "Senior Lecturer • Mobile Application Engineering & Kotlin",
        isActive = true,
      ),
      Teacher(
        id = "tch_cs_01",
        fullName = "Dr. Rahel Mekonnen",
        email = "rahel.mekonnen@wcu.edu.et",
        facultyId = "fac_eng_tech",
        departmentId = "dep_cs",
        specialization = "Assistant Professor • Machine Learning & Algorithms",
        isActive = true,
      ),
      Teacher(
        id = "tch_nurs_01",
        fullName = "Dr. Solomon Girma",
        email = "solomon.girma@wcu.edu.et",
        facultyId = "fac_med_health",
        departmentId = "dep_nurs",
        specialization = "Senior Clinical Educator • Patient Care & Nursing Ethics",
        isActive = true,
      ),
    )

  val demoCourses =
    listOf(
      Course(
        id = "crs_se_301",
        courseCode = "SENG3011",
        courseName = "Software Architecture & Design Patterns",
        teacherId = "tch_se_01",
        departmentId = "dep_se",
        semesterId = "sem_2026_1",
        academicYear = "2026/27",
        isActive = true,
      ),
      Course(
        id = "crs_se_302",
        courseCode = "SENG3042",
        courseName = "Enterprise Mobile Application Development",
        teacherId = "tch_se_02",
        departmentId = "dep_se",
        semesterId = "sem_2026_1",
        academicYear = "2026/27",
        isActive = true,
      ),
      Course(
        id = "crs_cs_301",
        courseCode = "COSC3021",
        courseName = "Design and Analysis of Algorithms",
        teacherId = "tch_cs_01",
        departmentId = "dep_cs",
        semesterId = "sem_2026_1",
        academicYear = "2026/27",
        isActive = true,
      ),
    )

  val demoSemesters =
    listOf(
      Semester(
        id = "sem_2026_1",
        name = "Semester I",
        academicYear = "2026/27",
        isActive = true,
        isEvaluationOpen = true,
      )
    )

  val demoStudents =
    listOf(
      demoStudentProfile,
      UserProfile(
        id = "demo_stu_se_2",
        uid = "demo_stu_se_2",
        fullName = "Yohannes Desta",
        email = "yohannes.desta@wcu.edu.et",
        studentId = "WCU/1488/16",
        role = UserProfile.ROLE_STUDENT,
        facultyId = "fac_eng_tech",
        departmentId = "dep_se",
        yearOfStudy = 3,
        isActive = true,
      ),
      UserProfile(
        id = "demo_stu_cs_1",
        uid = "demo_stu_cs_1",
        fullName = "Hanna Alemu",
        email = "hanna.alemu@wcu.edu.et",
        studentId = "WCU/2104/16",
        role = UserProfile.ROLE_STUDENT,
        facultyId = "fac_eng_tech",
        departmentId = "dep_cs",
        yearOfStudy = 4,
        isActive = true,
      ),
    )

  val demoAdministrators =
    listOf(
      demoSuperAdminRecord,
      demoDepartmentAdminRecord,
      AdminAccountRecord(
        uid = "demo_dept_admin_cs",
        fullName = "Dr. Samuel Girma",
        email = "samuel.girma@wcu.edu.et",
        role = UserProfile.ROLE_DEPARTMENT_ADMIN,
        facultyId = "fac_eng_tech",
        departmentId = "dep_cs",
        isActive = true,
        createdBy = DEMO_SUPER_ADMIN_UID,
      ),
    )

  private val mutableDemoEvaluations =
    mutableListOf(
      TeacherEvaluation(
        id = "${DEMO_STUDENT_UID}_tch_se_01_crs_se_301_sem_2026_1",
        evaluationId = "${DEMO_STUDENT_UID}_tch_se_01_crs_se_301_sem_2026_1",
        studentId = DEMO_STUDENT_UID,
        teacherId = "tch_se_01",
        courseId = "crs_se_301",
        departmentId = "dep_se",
        facultyId = "fac_eng_tech",
        semesterId = "sem_2026_1",
        academicYear = "2026/27",
        categoryRatings =
          mapOf(
            "cat_1" to 4.9,
            "cat_2" to 4.8,
            "cat_3" to 4.9,
            "cat_4" to 4.7,
            "cat_5" to 4.8,
            "cat_6" to 4.9,
          ),
        questionRatings = (1..18).associate { "q_$it" to 5 },
        overallScore = 4.83,
        comment = "Clear architectural diagrams and well-structured lab exercises.",
        status = TeacherEvaluation.STATUS_SUBMITTED,
      ),
      TeacherEvaluation(
        id = "demo_stu_se_2_tch_se_02_crs_se_302_sem_2026_1",
        evaluationId = "demo_stu_se_2_tch_se_02_crs_se_302_sem_2026_1",
        studentId = "demo_stu_se_2",
        teacherId = "tch_se_02",
        courseId = "crs_se_302",
        departmentId = "dep_se",
        facultyId = "fac_eng_tech",
        semesterId = "sem_2026_1",
        academicYear = "2026/27",
        categoryRatings =
          mapOf(
            "cat_1" to 4.7,
            "cat_2" to 4.6,
            "cat_3" to 4.8,
            "cat_4" to 4.6,
            "cat_5" to 4.7,
            "cat_6" to 4.8,
          ),
        questionRatings = (1..18).associate { "q_$it" to 5 },
        overallScore = 4.70,
        comment = "Engaging Jetpack Compose sessions and practical Android assignments.",
        status = TeacherEvaluation.STATUS_SUBMITTED,
      ),
      TeacherEvaluation(
        id = "demo_stu_cs_1_tch_cs_01_crs_cs_301_sem_2026_1",
        evaluationId = "demo_stu_cs_1_tch_cs_01_crs_cs_301_sem_2026_1",
        studentId = "demo_stu_cs_1",
        teacherId = "tch_cs_01",
        courseId = "crs_cs_301",
        departmentId = "dep_cs",
        facultyId = "fac_eng_tech",
        semesterId = "sem_2026_1",
        academicYear = "2026/27",
        categoryRatings =
          mapOf(
            "cat_1" to 4.6,
            "cat_2" to 4.5,
            "cat_3" to 4.7,
            "cat_4" to 4.5,
            "cat_5" to 4.6,
            "cat_6" to 4.7,
          ),
        questionRatings = (1..18).associate { "q_$it" to 5 },
        overallScore = 4.60,
        comment = "Thorough complexity proofs and algorithmic problem sets.",
        status = TeacherEvaluation.STATUS_SUBMITTED,
      ),
    )

  val demoEvaluations: List<TeacherEvaluation>
    get() = mutableDemoEvaluations.toList()

  fun submitDemoStudentEvaluation(
    studentProfile: UserProfile,
    teacherId: String,
    courseId: String,
    questionRatings: Map<String, Int>,
    comment: String,
  ): Result<TeacherEvaluation> = runCatching {
    val (categoryRatings, overallScore) =
      WcuEvaluationRubricCategories.computeScoresFromQuestionRatings(questionRatings)
    val course =
      demoCourses.firstOrNull { it.id == courseId && it.isActive }
        ?: throw IllegalArgumentException("Course '$courseId' was not found.")
    if (course.departmentId != studentProfile.departmentId) {
      throw SecurityException("Department mismatch: course does not belong to ${studentProfile.departmentId}.")
    }
    if (course.teacherId != teacherId) {
      throw IllegalArgumentException("Instructor '$teacherId' is not assigned to ${course.courseCode}.")
    }
    val activeSem = demoSemesters.first()
    val evaluationId =
      TeacherEvaluation.buildCompositeId(
        studentUid = studentProfile.uid,
        teacherId = teacherId,
        courseId = courseId,
        semesterId = activeSem.id,
      )
    if (mutableDemoEvaluations.any { it.evaluationId == evaluationId || it.id == evaluationId }) {
      throw IllegalStateException(
        "Duplicate evaluation prevented: you have already evaluated ${course.courseCode} for this semester."
      )
    }
    val created =
      TeacherEvaluation(
        id = evaluationId,
        evaluationId = evaluationId,
        studentId = studentProfile.uid,
        teacherId = teacherId,
        courseId = courseId,
        departmentId = studentProfile.departmentId,
        facultyId = studentProfile.facultyId,
        semesterId = activeSem.id,
        academicYear = activeSem.academicYear,
        categoryRatings = categoryRatings,
        questionRatings = questionRatings,
        overallScore = overallScore,
        comment = comment.trim().take(1000),
        submittedAt = com.google.firebase.Timestamp.now(),
        status = TeacherEvaluation.STATUS_SUBMITTED,
      )
    mutableDemoEvaluations.add(0, created)
    created
  }

  val demoAuditLogs =
    listOf(
      AdminAuditLog(
        id = "log_demo_01",
        actorUid = DEMO_SUPER_ADMIN_UID,
        actorRole = UserProfile.ROLE_SUPER_ADMIN,
        assignedDepartmentId = "",
        action = "CREATE_DEPARTMENT_ADMIN",
        targetCollection = "admins",
        targetDocumentId = DEMO_DEPT_ADMIN_UID,
        details = "Assigned Dr. Dawit Bekele to Software Engineering (dep_se)",
      ),
      AdminAuditLog(
        id = "log_demo_02",
        actorUid = DEMO_DEPT_ADMIN_UID,
        actorRole = UserProfile.ROLE_DEPARTMENT_ADMIN,
        assignedDepartmentId = "dep_se",
        action = "UPSERT_COURSE",
        targetCollection = "courses",
        targetDocumentId = "crs_se_302",
        details = "Updated SENG3042 Enterprise Mobile Application Development in dep_se",
      ),
    )

  fun buildStudentDashboardSummary(profile: UserProfile): StudentDashboardSummary {
    val activeSem = demoSemesters.first()
    val seTeachers = demoTeachers.filter { it.departmentId == profile.departmentId && it.isActive }
    val seCourses = demoCourses.filter { it.departmentId == profile.departmentId && it.isActive }
    val teacherById = demoTeachers.associateBy { it.id }
    val courseById = demoCourses.associateBy { it.id }
    val myEvaluations = mutableDemoEvaluations.filter { it.studentId == profile.uid }
    val evaluatedKeys =
      myEvaluations.map { "${it.teacherId}_${it.courseId}_${it.semesterId}" }.toSet()

    val pendingItems =
      seCourses
        .filter { !evaluatedKeys.contains("${it.teacherId}_${it.id}_${it.semesterId}") }
        .mapNotNull { course ->
          val teacher = teacherById[course.teacherId] ?: return@mapNotNull null
          PendingCourseEvaluationItem(
            course = course,
            teacher = teacher,
            semesterName = "${activeSem.name} (${activeSem.academicYear})",
          )
        }

    val recentItems =
      myEvaluations.mapIndexed { idx, eval ->
        val teacher = teacherById[eval.teacherId]
        val course = courseById[eval.courseId]
        RecentEvaluationActivityItem(
          evaluationId = eval.evaluationId,
          teacherId = eval.teacherId,
          teacherName = teacher?.fullName ?: eval.teacherId,
          courseId = eval.courseId,
          courseCode = course?.courseCode ?: eval.courseId,
          courseName = course?.courseName ?: eval.courseId,
          semesterId = eval.semesterId,
          academicYear = eval.academicYear,
          overallScore = eval.overallScore,
          comment = eval.comment,
          submittedAtMillis = eval.submittedAt?.toDate()?.time ?: (System.currentTimeMillis() - (idx + 1) * 3_600_000L),
          status = eval.status,
        )
      }

    val completedCount = myEvaluations.size
    val pendingCount = pendingItems.size
    val totalCount = (completedCount + pendingCount).coerceAtLeast(1)
    val fraction = (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
    val percent = (fraction * 100f).roundToInt().coerceIn(0, 100)

    return StudentDashboardSummary(
      profile = profile,
      faculty = demoFaculties.first(),
      department = demoDepartments.first(),
      facultyDisplayName = demoFaculties.first().name,
      departmentDisplayName = demoDepartments.first().name,
      activeSemester = activeSem,
      totalAvailableTeachers = seTeachers.size,
      availableTeachers = seTeachers,
      totalEligibleCourses = seCourses.size,
      pendingEvaluationsCount = pendingCount,
      completedEvaluationsCount = completedCount,
      evaluationProgressPercent = percent,
      evaluationProgressFraction = fraction,
      pendingEvaluations = pendingItems,
      recentEvaluationActivity = recentItems,
    )
  }

  fun buildAcademicDirectorySnapshot(): AcademicDirectorySnapshot {
    val demoStudentEvalKeys =
      mutableDemoEvaluations
        .filter { it.studentId == DEMO_STUDENT_UID }
        .map { it.evaluationId }
        .toSet()
    return AcademicDirectorySnapshot(
      faculties = demoFaculties,
      departments = demoDepartments,
      teachers = demoTeachers,
      courses = demoCourses,
      activeSemester = demoSemesters.first(),
      evaluatedCompositeKeys = demoStudentEvalKeys,
    )
  }

  fun buildTeacherProfileDetails(
    teacherId: String,
    studentProfile: UserProfile,
  ): TeacherProfileDetails? {
    val teacher = demoTeachers.firstOrNull { it.id == teacherId && it.isActive } ?: return null
    val fac = demoFaculties.firstOrNull { it.id == teacher.facultyId }
    val dept = demoDepartments.firstOrNull { it.id == teacher.departmentId }
    val evaluatedCourseIds =
      mutableDemoEvaluations
        .filter { it.studentId == studentProfile.uid && it.teacherId == teacher.id }
        .map { it.courseId }
        .toSet()
    val courses =
      demoCourses
        .filter { it.teacherId == teacher.id && it.isActive }
        .map { course ->
          TeacherCourseAssignmentItem(
            course = course,
            semesterName = "Semester I (2026/27)",
            isStudentDepartmentMatch = course.departmentId == studentProfile.departmentId,
            isAlreadyEvaluated = evaluatedCourseIds.contains(course.id),
          )
        }
    return TeacherProfileDetails(
      teacher = teacher,
      faculty = fac,
      department = dept,
      facultyDisplayName = fac?.name ?: teacher.facultyId,
      departmentDisplayName = dept?.name ?: teacher.departmentId,
      assignedCourses = courses,
    )
  }

  fun buildSuperAdminDashboardSummary(
    adminProfile: UserProfile,
    faculties: List<Faculty> = demoFaculties,
    departments: List<Department> = demoDepartments,
    teachers: List<Teacher> = demoTeachers,
    courses: List<Course> = demoCourses,
    students: List<UserProfile> = demoStudents,
    administrators: List<AdminAccountRecord> = demoAdministrators,
    evaluations: List<TeacherEvaluation> = demoEvaluations,
    auditLogs: List<AdminAuditLog> = demoAuditLogs,
  ): SuperAdminDashboardSummary {
    val facultiesById = faculties.associateBy { it.id }
    val comparisons =
      departments.map { dept ->
        val deptStudents = students.filter { it.departmentId == dept.id }
        val deptTeachers = teachers.filter { it.departmentId == dept.id && it.isActive }
        val deptCourses = courses.filter { it.departmentId == dept.id && it.isActive }
        val deptEvals = evaluations.filter { it.departmentId == dept.id }
        val expected = (deptStudents.size * deptCourses.size).coerceAtLeast(1)
        val pct =
          ((deptEvals.size.toDouble() / expected.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        val avg = if (deptEvals.isNotEmpty()) deptEvals.map { it.overallScore }.average() else 0.0
        DepartmentPerformanceRow(
          departmentId = dept.id,
          departmentName = dept.name,
          facultyId = dept.facultyId,
          facultyName = facultiesById[dept.facultyId]?.name ?: dept.facultyId,
          studentCount = deptStudents.size,
          activeTeacherCount = deptTeachers.size,
          activeCourseCount = deptCourses.size,
          submittedEvaluationCount = deptEvals.size,
          averageScore = avg,
          completionPercentage = pct,
        )
      }
    return SuperAdminDashboardSummary(
      adminProfile = adminProfile,
      totalFaculties = faculties.size,
      totalDepartments = departments.size,
      totalStudents = students.size,
      totalTeachers = teachers.count { it.isActive },
      totalActiveCourses = courses.count { it.isActive },
      totalAdministrators = administrators.size,
      totalSubmittedEvaluations = evaluations.size,
      totalPendingEvaluations = 3,
      completionPercentage = 50,
      overallUniversityRating = 4.83,
      faculties = faculties,
      departments = departments,
      teachers = teachers,
      courses = courses,
      students = students,
      administrators = administrators,
      semesters = demoSemesters,
      departmentComparisons = comparisons,
      recentAuditLogs = auditLogs,
    )
  }

  fun buildDepartmentAdminDashboardSummary(
    adminRecord: AdminAccountRecord,
    teachers: List<Teacher> = demoTeachers,
    courses: List<Course> = demoCourses,
    auditLogs: List<AdminAuditLog> = demoAuditLogs,
  ): DepartmentAdminDashboardSummary {
    val deptId = adminRecord.departmentId ?: "dep_se"
    val facId = adminRecord.facultyId ?: "fac_eng_tech"
    val fac = demoFaculties.firstOrNull { it.id == facId }
    val dept = demoDepartments.firstOrNull { it.id == deptId }
    val deptStudents = demoStudents.filter { it.departmentId == deptId }
    val deptTeachers = teachers.filter { it.departmentId == deptId }
    val deptCourses = courses.filter { it.departmentId == deptId }
    val deptEvals = demoEvaluations.filter { it.departmentId == deptId }
    return DepartmentAdminDashboardSummary(
      adminRecord = adminRecord,
      department = dept,
      faculty = fac,
      departmentDisplayName = dept?.name ?: deptId,
      facultyDisplayName = fac?.name ?: facId,
      totalStudents = deptStudents.size,
      totalActiveTeachers = deptTeachers.count { it.isActive },
      totalActiveCourses = deptCourses.count { it.isActive },
      totalSubmittedEvaluations = deptEvals.size,
      pendingEvaluationsCount = 2,
      completionPercentage = 50,
      departmentAverageRating = if (deptEvals.isNotEmpty()) 4.77 else 0.0,
      departmentStudents = deptStudents,
      departmentTeachers = deptTeachers,
      departmentCourses = deptCourses,
      departmentEvaluations = deptEvals,
      recentAuditLogs = auditLogs.filter { it.assignedDepartmentId == deptId },
    )
  }
}
