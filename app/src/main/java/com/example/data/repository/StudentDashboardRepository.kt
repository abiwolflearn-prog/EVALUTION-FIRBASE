package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Course
import com.example.data.model.Department
import com.example.data.model.Faculty
import com.example.data.model.PendingCourseEvaluationItem
import com.example.data.model.RecentEvaluationActivityItem
import com.example.data.model.Semester
import com.example.data.model.StudentDashboardSummary
import com.example.data.model.Teacher
import com.example.data.model.TeacherEvaluation
import com.example.data.model.UserProfile
import com.example.data.model.WcuAcademicCatalogFallback
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudentDashboardRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in before loading Student Dashboard.")
  }

  /**
   * Loads the complete Student Dashboard summary from Cloud Firestore:
   * - Student profile (`/users/{uid}`)
   * - Faculty (`/faculties/{facultyId}`) and Department (`/departments/{departmentId}`)
   * - Active Semester (`/semesters`)
   * - Active Department Teachers (`/teachers`)
   * - Active Department Courses (`/courses`)
   * - Student's Submitted Evaluations (`/evaluations` filtered by `studentId == uid`)
   */
  suspend fun loadStudentDashboard(
    uid: String = requireUserId(),
  ): Result<StudentDashboardSummary> = runCatching {
    val profileDocRef = db.collection(COLLECTION_USERS).document(uid)
    val profile =
      try {
        val snap = profileDocRef.get().await()
        snap.toObject(
          UserProfile::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ) ?: throw IllegalStateException("Student profile document /users/$uid not found.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, profileDocRef.path)
        throw e
      }

    val faculty = fetchFacultyOrNull(profile.facultyId)
    val department = fetchDepartmentOrNull(profile.departmentId)
    val activeSemester = fetchActiveSemesterOrNull()
    val departmentTeachers = fetchActiveTeachersForDepartment(profile.departmentId)
    val departmentCourses = fetchActiveCoursesForDepartment(profile.departmentId, activeSemester?.id)
    val studentEvaluations = fetchStudentEvaluations(profile.uid)

    // Also resolve any teachers assigned to courses or evaluated by the student if they belong to cross-cutting faculties
    val teacherById = departmentTeachers.associateBy { it.id }.toMutableMap()
    val missingTeacherIds =
      (departmentCourses.map { it.teacherId } + studentEvaluations.map { it.teacherId })
        .filter { it.isNotBlank() && !teacherById.containsKey(it) }
        .distinct()

    for (teacherId in missingTeacherIds) {
      fetchTeacherOrNull(teacherId)?.let { teacher ->
        if (teacher.isActive) {
          teacherById[teacher.id] = teacher
        }
      }
    }

    // Also resolve any courses referenced in the student's evaluation history
    val courseById = departmentCourses.associateBy { it.id }.toMutableMap()
    val missingCourseIds =
      studentEvaluations
        .map { it.courseId }
        .filter { it.isNotBlank() && !courseById.containsKey(it) }
        .distinct()

    for (courseId in missingCourseIds) {
      fetchCourseOrNull(courseId)?.let { course ->
        courseById[course.id] = course
      }
    }

    buildDashboardSummary(
      profile = profile,
      faculty = faculty,
      department = department,
      activeSemester = activeSemester,
      departmentTeachers = departmentTeachers,
      teacherById = teacherById,
      departmentCourses = departmentCourses,
      courseById = courseById,
      studentEvaluations = studentEvaluations,
    )
  }

  /**
   * Real-time reactive stream of the Student Dashboard summary backed by Firestore snapshot listeners.
   */
  fun observeStudentDashboard(
    fallbackProfile: UserProfile,
  ): Flow<StudentDashboardSummary> = flow {
    val uid = fallbackProfile.uid.ifBlank { requireUserId() }
    val userDocRef = db.collection(COLLECTION_USERS).document(uid)
    val teachersQuery =
      db.collection(COLLECTION_TEACHERS)
        .whereEqualTo("departmentId", fallbackProfile.departmentId)
        .whereEqualTo("isActive", true)
    val coursesQuery =
      db.collection(COLLECTION_COURSES)
        .whereEqualTo("departmentId", fallbackProfile.departmentId)
        .whereEqualTo("isActive", true)
    val evaluationsQuery =
      db.collection(COLLECTION_EVALUATIONS)
        .whereEqualTo("studentId", uid)

    val userFlow =
      userDocRef.snapshots().map { snap ->
        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          ?: fallbackProfile
      }

    val teachersFlow =
      teachersQuery.snapshots().map { snap ->
        snap.toObjects(Teacher::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }

    val coursesFlow =
      coursesQuery.snapshots().map { snap ->
        snap.toObjects(Course::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }

    val evaluationsFlow =
      evaluationsQuery.snapshots().map { snap ->
        snap.toObjects(
          TeacherEvaluation::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      }

    val faculty = fetchFacultyOrNull(fallbackProfile.facultyId)
    val department = fetchDepartmentOrNull(fallbackProfile.departmentId)
    val activeSemester = fetchActiveSemesterOrNull()

    emitAll(
      combine(userFlow, teachersFlow, coursesFlow, evaluationsFlow) {
          liveProfile,
          liveTeachers,
          liveCourses,
          liveEvaluations ->
          val teacherById = liveTeachers.associateBy { it.id }
          val courseById = liveCourses.associateBy { it.id }
          buildDashboardSummary(
            profile = liveProfile,
            faculty = faculty,
            department = department,
            activeSemester = activeSemester,
            departmentTeachers = liveTeachers,
            teacherById = teacherById,
            departmentCourses = liveCourses,
            courseById = courseById,
            studentEvaluations = liveEvaluations,
          )
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, COLLECTION_EVALUATIONS)
          }
          throw error
        }
    )
  }

  private suspend fun fetchFacultyOrNull(facultyId: String): Faculty? {
    if (facultyId.isBlank()) return null
    val docRef = db.collection(COLLECTION_FACULTIES).document(facultyId)
    return try {
      val snap = docRef.get().await()
      if (!snap.exists()) return null
      val faculty =
        snap.toObject(Faculty::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      faculty?.takeIf { it.isActive }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      null
    }
  }

  private suspend fun fetchDepartmentOrNull(departmentId: String): Department? {
    if (departmentId.isBlank()) return null
    val docRef = db.collection(COLLECTION_DEPARTMENTS).document(departmentId)
    return try {
      val snap = docRef.get().await()
      if (!snap.exists()) return null
      val dept =
        snap.toObject(Department::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      dept?.takeIf { it.isActive }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      null
    }
  }

  private suspend fun fetchActiveSemesterOrNull(): Semester? {
    return try {
      val querySnapshot =
        db.collection(COLLECTION_SEMESTERS)
          .whereEqualTo("isActive", true)
          .get()
          .await()
      querySnapshot
        .toObjects(Semester::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .firstOrNull()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, COLLECTION_SEMESTERS)
      null
    }
  }

  private suspend fun fetchActiveTeachersForDepartment(departmentId: String): List<Teacher> {
    if (departmentId.isBlank()) return emptyList()
    return try {
      val querySnapshot =
        db.collection(COLLECTION_TEACHERS)
          .whereEqualTo("departmentId", departmentId)
          .whereEqualTo("isActive", true)
          .get()
          .await()
      querySnapshot
        .toObjects(Teacher::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedBy { it.fullName }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, COLLECTION_TEACHERS)
      throw e
    }
  }

  private suspend fun fetchTeacherOrNull(teacherId: String): Teacher? {
    if (teacherId.isBlank()) return null
    val docRef = db.collection(COLLECTION_TEACHERS).document(teacherId)
    return try {
      val snap = docRef.get().await()
      if (!snap.exists()) return null
      snap.toObject(Teacher::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      null
    }
  }

  private suspend fun fetchActiveCoursesForDepartment(
    departmentId: String,
    activeSemesterId: String?,
  ): List<Course> {
    if (departmentId.isBlank()) return emptyList()
    return try {
      val querySnapshot =
        db.collection(COLLECTION_COURSES)
          .whereEqualTo("departmentId", departmentId)
          .whereEqualTo("isActive", true)
          .get()
          .await()
      val courses =
        querySnapshot.toObjects(
          Course::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      if (!activeSemesterId.isNullOrBlank()) {
        val semesterMatched = courses.filter { it.semesterId == activeSemesterId }
        if (semesterMatched.isNotEmpty()) semesterMatched.sortedBy { it.courseCode }
        else courses.sortedBy { it.courseCode }
      } else {
        courses.sortedBy { it.courseCode }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, COLLECTION_COURSES)
      throw e
    }
  }

  private suspend fun fetchCourseOrNull(courseId: String): Course? {
    if (courseId.isBlank()) return null
    val docRef = db.collection(COLLECTION_COURSES).document(courseId)
    return try {
      val snap = docRef.get().await()
      if (!snap.exists()) return null
      snap.toObject(Course::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      null
    }
  }

  private suspend fun fetchStudentEvaluations(studentUid: String): List<TeacherEvaluation> {
    return try {
      val querySnapshot =
        db.collection(COLLECTION_EVALUATIONS)
          .whereEqualTo("studentId", studentUid)
          .get()
          .await()
      querySnapshot
        .toObjects(
          TeacherEvaluation::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
        .sortedByDescending { it.submittedAt?.toDate()?.time ?: 0L }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, COLLECTION_EVALUATIONS)
      throw e
    }
  }

  private fun buildDashboardSummary(
    profile: UserProfile,
    faculty: Faculty?,
    department: Department?,
    activeSemester: Semester?,
    departmentTeachers: List<Teacher>,
    teacherById: Map<String, Teacher>,
    departmentCourses: List<Course>,
    courseById: Map<String, Course>,
    studentEvaluations: List<TeacherEvaluation>,
  ): StudentDashboardSummary {
    val activeTeachersMap = linkedMapOf<String, Teacher>()
    departmentTeachers.filter { it.isActive }.forEach { activeTeachersMap[it.id] = it }
    departmentCourses.forEach { course ->
      val assignedTeacher = teacherById[course.teacherId]
      if (assignedTeacher != null && assignedTeacher.isActive) {
        activeTeachersMap[assignedTeacher.id] = assignedTeacher
      }
    }

    val availableTeachers = activeTeachersMap.values.sortedBy { it.fullName }

    // Eligible courses are active department courses whose assigned teacher is active
    val eligibleCourses =
      departmentCourses.filter { course ->
        course.isActive && activeTeachersMap.containsKey(course.teacherId)
      }

    // Determine which eligible courses have already been evaluated by this student
    val evaluatedCourseSemesterKeys =
      studentEvaluations
        .map { "${it.teacherId}_${it.courseId}_${it.semesterId}" }
        .toSet()

    val semesterDisplayName =
      activeSemester?.let { "${it.name} (${it.academicYear})" } ?: "Current Academic Semester"

    val pendingEvaluations =
      eligibleCourses
        .filter { course ->
          val key = "${course.teacherId}_${course.id}_${course.semesterId}"
          !evaluatedCourseSemesterKeys.contains(key)
        }
        .mapNotNull { course ->
          val teacher = activeTeachersMap[course.teacherId] ?: return@mapNotNull null
          PendingCourseEvaluationItem(
            course = course,
            teacher = teacher,
            semesterName = semesterDisplayName,
          )
        }

    val completedCount = studentEvaluations.size
    val pendingCount = pendingEvaluations.size
    val totalTargetEvaluations = completedCount + pendingCount

    val progressFraction =
      if (totalTargetEvaluations > 0) {
        (completedCount.toFloat() / totalTargetEvaluations.toFloat()).coerceIn(0f, 1f)
      } else {
        0f
      }
    val progressPercent = (progressFraction * 100f).roundToInt().coerceIn(0, 100)

    val recentActivity =
      studentEvaluations.map { eval ->
        val teacherName =
          teacherById[eval.teacherId]?.fullName ?: "Instructor (${eval.teacherId})"
        val course = courseById[eval.courseId]
        RecentEvaluationActivityItem(
          evaluationId = eval.evaluationId.ifBlank { eval.id },
          teacherId = eval.teacherId,
          teacherName = teacherName,
          courseId = eval.courseId,
          courseCode = course?.courseCode ?: eval.courseId.uppercase(),
          courseName = course?.courseName ?: "Academic Course (${eval.courseId})",
          semesterId = eval.semesterId,
          academicYear = eval.academicYear,
          overallScore = eval.overallScore,
          comment = eval.comment,
          submittedAtMillis = eval.submittedAt?.toDate()?.time,
          status = eval.status,
        )
      }

    return StudentDashboardSummary(
      profile = profile,
      faculty = faculty,
      department = department,
      facultyDisplayName = WcuAcademicCatalogFallback.resolveFacultyName(faculty, profile.facultyId),
      departmentDisplayName =
        WcuAcademicCatalogFallback.resolveDepartmentName(department, profile.departmentId),
      activeSemester = activeSemester,
      totalAvailableTeachers = availableTeachers.size,
      availableTeachers = availableTeachers,
      totalEligibleCourses = eligibleCourses.size,
      pendingEvaluationsCount = pendingCount,
      completedEvaluationsCount = completedCount,
      evaluationProgressPercent = progressPercent,
      evaluationProgressFraction = progressFraction,
      pendingEvaluations = pendingEvaluations,
      recentEvaluationActivity = recentActivity,
    )
  }

  /**
   * Submits an immutable 6-category, 18-question teacher evaluation to `/evaluations/{evaluationId}`
   * where `evaluationId = "${studentUid}_${teacherId}_${courseId}_${semesterId}"`.
   *
   * Enforces:
   * - Active student profile (`role == "student"` and `isActive == true`)
   * - Course active and matching student's `departmentId` and selected `teacherId`
   * - All 18 rubric questions (`q_1`..`q_18`) rated 1..5 stars
   * - 6 category averages (`cat_1`..`cat_6`) and `overallScore` accurately calculated
   * - Duplicate evaluation submission prevention
   */
  suspend fun submitTeacherEvaluation(
    studentUid: String = requireUserId(),
    teacherId: String,
    courseId: String,
    questionRatings: Map<String, Int>,
    comment: String,
  ): Result<TeacherEvaluation> = runCatching {
    require(studentUid.isNotBlank()) { "Authenticated student UID is required." }
    require(teacherId.isNotBlank()) { "Teacher ID is required." }
    require(courseId.isNotBlank()) { "Course ID is required." }

    val trimmedComment = comment.trim().take(1000)
    val (categoryRatings, overallScore) =
      com.example.data.model.WcuEvaluationRubricCategories.computeScoresFromQuestionRatings(
        questionRatings
      )

    val profileSnap = db.collection(COLLECTION_USERS).document(studentUid).get().await()
    val profile =
      profileSnap.toObject(
        UserProfile::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
      ) ?: throw IllegalStateException("Student profile /users/$studentUid not found.")

    if (!profile.isActive || profile.role != UserProfile.ROLE_STUDENT) {
      throw SecurityException("Only active students can submit teacher evaluations.")
    }

    val course =
      fetchCourseOrNull(courseId)
        ?: throw IllegalArgumentException("Course '$courseId' was not found.")
    if (!course.isActive) {
      throw IllegalStateException("Course '${course.courseCode}' is not currently active.")
    }
    if (course.departmentId != profile.departmentId) {
      throw SecurityException(
        "Department mismatch: you can only evaluate courses assigned to your department (${profile.departmentId})."
      )
    }
    if (course.teacherId != teacherId) {
      throw IllegalArgumentException(
        "Instructor mismatch: teacher '$teacherId' is not assigned to course '${course.courseCode}'."
      )
    }

    val activeSemester = fetchActiveSemesterOrNull()
    if (activeSemester != null && !activeSemester.isEvaluationOpen) {
      throw IllegalStateException("The evaluation window for ${activeSemester.name} is currently closed.")
    }

    val semesterId = course.semesterId.ifBlank { activeSemester?.id ?: "sem_current" }
    val academicYear = course.academicYear.ifBlank { activeSemester?.academicYear ?: "2026/27" }
    val evaluationId =
      TeacherEvaluation.buildCompositeId(
        studentUid = studentUid,
        teacherId = teacherId,
        courseId = courseId,
        semesterId = semesterId,
      )

    // Verify no duplicate evaluation exists for this student + teacher + course + semester
    val existingEvaluations = fetchStudentEvaluations(studentUid)
    if (existingEvaluations.any { it.evaluationId == evaluationId || it.id == evaluationId }) {
      throw IllegalStateException(
        "Duplicate evaluation prevented: you have already evaluated ${course.courseCode} for this semester."
      )
    }

    val docRef = db.collection(COLLECTION_EVALUATIONS).document(evaluationId)
    val payload =
      mapOf(
        "evaluationId" to evaluationId,
        "studentId" to studentUid,
        "teacherId" to teacherId,
        "courseId" to courseId,
        "departmentId" to profile.departmentId,
        "facultyId" to profile.facultyId,
        "semesterId" to semesterId,
        "academicYear" to academicYear,
        "categoryRatings" to categoryRatings,
        "questionRatings" to questionRatings,
        "overallScore" to overallScore,
        "comment" to trimmedComment,
        "submittedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        "status" to TeacherEvaluation.STATUS_SUBMITTED,
      )

    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }

    TeacherEvaluation(
      id = evaluationId,
      evaluationId = evaluationId,
      studentId = studentUid,
      teacherId = teacherId,
      courseId = courseId,
      departmentId = profile.departmentId,
      facultyId = profile.facultyId,
      semesterId = semesterId,
      academicYear = academicYear,
      categoryRatings = categoryRatings,
      questionRatings = questionRatings,
      overallScore = overallScore,
      comment = trimmedComment,
      submittedAt = com.google.firebase.Timestamp.now(),
      status = TeacherEvaluation.STATUS_SUBMITTED,
    )
  }

  companion object {
    const val COLLECTION_USERS = "users"
    const val COLLECTION_FACULTIES = "faculties"
    const val COLLECTION_DEPARTMENTS = "departments"
    const val COLLECTION_TEACHERS = "teachers"
    const val COLLECTION_COURSES = "courses"
    const val COLLECTION_SEMESTERS = "semesters"
    const val COLLECTION_EVALUATIONS = "evaluations"
  }
}
