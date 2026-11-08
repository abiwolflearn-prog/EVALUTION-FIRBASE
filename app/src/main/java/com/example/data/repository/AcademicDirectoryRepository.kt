package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Course
import com.example.data.model.Department
import com.example.data.model.Faculty
import com.example.data.model.Semester
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
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

data class TeacherCourseAssignmentItem(
  val course: Course,
  val semesterName: String,
  val isStudentDepartmentMatch: Boolean,
  val isAlreadyEvaluated: Boolean,
)

data class TeacherProfileDetails(
  val teacher: Teacher,
  val faculty: Faculty?,
  val department: Department?,
  val facultyDisplayName: String,
  val departmentDisplayName: String,
  val assignedCourses: List<TeacherCourseAssignmentItem>,
)

data class AcademicDirectorySnapshot(
  val faculties: List<Faculty>,
  val departments: List<Department>,
  val teachers: List<Teacher>,
  val courses: List<Course>,
  val activeSemester: Semester?,
  val evaluatedCompositeKeys: Set<String>,
)

class AcademicDirectoryRepository(
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
      ?: throw IllegalStateException("User must be signed in before accessing the academic directory.")
  }

  /**
   * Loads active faculties from `/faculties` (`isActive == true`).
   */
  suspend fun loadActiveFaculties(limitCount: Long = DEFAULT_PAGE_LIMIT): Result<List<Faculty>> =
    runCatching {
      requireUserId()
      try {
        val snapshot =
          db.collection(COLLECTION_FACULTIES)
            .whereEqualTo("isActive", true)
            .limit(limitCount)
            .get()
            .await()
        snapshot
          .toObjects(Faculty::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .filter { it.isActive && it.id.isNotBlank() }
          .sortedBy { it.name }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.LIST, COLLECTION_FACULTIES)
        throw e
      }
    }

  /**
   * Loads active departments from `/departments` (`isActive == true`), optionally filtered by `facultyId`.
   */
  suspend fun loadActiveDepartments(
    facultyId: String? = null,
    limitCount: Long = DEFAULT_PAGE_LIMIT,
  ): Result<List<Department>> = runCatching {
    requireUserId()
    try {
      var query: Query =
        db.collection(COLLECTION_DEPARTMENTS)
          .whereEqualTo("isActive", true)
      if (!facultyId.isNullOrBlank()) {
        query = query.whereEqualTo("facultyId", facultyId)
      }
      val snapshot = query.limit(limitCount).get().await()
      snapshot
        .toObjects(Department::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .filter { it.isActive && it.id.isNotBlank() }
        .sortedBy { it.name }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, COLLECTION_DEPARTMENTS)
      throw e
    }
  }

  /**
   * Loads active teachers from `/teachers` (`isActive == true`), optionally filtered by `facultyId` and/or `departmentId`.
   */
  suspend fun loadActiveTeachers(
    facultyId: String? = null,
    departmentId: String? = null,
    limitCount: Long = DEFAULT_PAGE_LIMIT,
  ): Result<List<Teacher>> = runCatching {
    requireUserId()
    try {
      var query: Query =
        db.collection(COLLECTION_TEACHERS)
          .whereEqualTo("isActive", true)
      if (!facultyId.isNullOrBlank()) {
        query = query.whereEqualTo("facultyId", facultyId)
      }
      if (!departmentId.isNullOrBlank()) {
        query = query.whereEqualTo("departmentId", departmentId)
      }
      val snapshot = query.limit(limitCount).get().await()
      snapshot
        .toObjects(Teacher::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .filter { it.isActive && it.id.isNotBlank() }
        .sortedBy { it.fullName }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, COLLECTION_TEACHERS)
      throw e
    }
  }

  /**
   * Loads active courses from `/courses` (`isActive == true`), optionally filtered by `departmentId`, `teacherId`, or `semesterId`.
   */
  suspend fun loadActiveCourses(
    departmentId: String? = null,
    teacherId: String? = null,
    semesterId: String? = null,
    limitCount: Long = DEFAULT_PAGE_LIMIT,
  ): Result<List<Course>> = runCatching {
    requireUserId()
    try {
      var query: Query =
        db.collection(COLLECTION_COURSES)
          .whereEqualTo("isActive", true)
      if (!departmentId.isNullOrBlank()) {
        query = query.whereEqualTo("departmentId", departmentId)
      }
      if (!teacherId.isNullOrBlank()) {
        query = query.whereEqualTo("teacherId", teacherId)
      }
      if (!semesterId.isNullOrBlank()) {
        query = query.whereEqualTo("semesterId", semesterId)
      }
      val snapshot = query.limit(limitCount).get().await()
      snapshot
        .toObjects(Course::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .filter { it.isActive && it.id.isNotBlank() }
        .sortedBy { it.courseCode }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, COLLECTION_COURSES)
      throw e
    }
  }

  /**
   * Safely loads a single Teacher profile and its assigned courses from Firestore.
   * Returns `null` if the teacher document does not exist or has `isActive == false` (never crashes on missing/deleted records).
   */
  suspend fun loadTeacherProfileDetails(
    teacherId: String,
    studentProfile: UserProfile,
  ): Result<TeacherProfileDetails?> = runCatching {
    requireUserId()
    val cleanTeacherId = teacherId.trim()
    if (cleanTeacherId.isEmpty()) return@runCatching null

    val teacherDocRef = db.collection(COLLECTION_TEACHERS).document(cleanTeacherId)
    val teacher =
      try {
        val snap = teacherDocRef.get().await()
        if (!snap.exists()) return@runCatching null
        snap
          .toObject(Teacher::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          ?.takeIf { it.isActive }
          ?: return@runCatching null
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, teacherDocRef.path)
        throw e
      }

    val faculty = fetchFacultySafely(teacher.facultyId)
    val department = fetchDepartmentSafely(teacher.departmentId)
    val activeSemester = fetchActiveSemesterSafely()
    val assignedCourses = loadActiveCourses(teacherId = teacher.id).getOrDefault(emptyList())
    val evaluatedKeys = fetchStudentEvaluatedKeys(studentProfile.uid)

    val semesterLabel =
      activeSemester?.let { "${it.name} (${it.academicYear})" } ?: "Current Academic Semester"

    val courseItems =
      assignedCourses.map { course ->
        val compositeKey =
          TeacherEvaluation.buildCompositeId(
            studentUid = studentProfile.uid,
            teacherId = teacher.id,
            courseId = course.id,
            semesterId = course.semesterId,
          )
        TeacherCourseAssignmentItem(
          course = course,
          semesterName = semesterLabel,
          isStudentDepartmentMatch = course.departmentId == studentProfile.departmentId,
          isAlreadyEvaluated = evaluatedKeys.contains(compositeKey),
        )
      }

    TeacherProfileDetails(
      teacher = teacher,
      faculty = faculty,
      department = department,
      facultyDisplayName = WcuAcademicCatalogFallback.resolveFacultyName(faculty, teacher.facultyId),
      departmentDisplayName =
        WcuAcademicCatalogFallback.resolveDepartmentName(department, teacher.departmentId),
      assignedCourses = courseItems,
    )
  }

  /**
   * Loads the full Academic Directory snapshot (faculties, departments, teachers, courses, active semester,
   * and student's evaluated composite keys) from Firestore.
   */
  suspend fun loadAcademicDirectorySnapshot(
    studentUid: String = requireUserId(),
    limitCount: Long = DEFAULT_PAGE_LIMIT,
  ): Result<AcademicDirectorySnapshot> = runCatching {
    val faculties = loadActiveFaculties(limitCount).getOrThrow()
    val departments = loadActiveDepartments(limitCount = limitCount).getOrThrow()
    val teachers = loadActiveTeachers(limitCount = limitCount).getOrThrow()
    val courses = loadActiveCourses(limitCount = limitCount).getOrThrow()
    val activeSemester = fetchActiveSemesterSafely()
    val evaluatedKeys = fetchStudentEvaluatedKeys(studentUid)

    AcademicDirectorySnapshot(
      faculties = faculties,
      departments = departments,
      teachers = teachers,
      courses = courses,
      activeSemester = activeSemester,
      evaluatedCompositeKeys = evaluatedKeys,
    )
  }

  /**
   * Pure search and filter helper for teachers (testable both with and without UI).
   */
  fun filterTeachers(
    teachers: List<Teacher>,
    searchQuery: String = "",
    facultyId: String? = null,
    departmentId: String? = null,
  ): List<Teacher> {
    val normalizedQuery = searchQuery.trim().lowercase()
    return teachers.filter { teacher ->
      if (!teacher.isActive) return@filter false
      val matchesFaculty = facultyId.isNullOrBlank() || teacher.facultyId == facultyId
      val matchesDepartment = departmentId.isNullOrBlank() || teacher.departmentId == departmentId
      val matchesQuery =
        normalizedQuery.isEmpty() ||
          teacher.fullName.lowercase().contains(normalizedQuery) ||
          teacher.specialization.lowercase().contains(normalizedQuery) ||
          teacher.email.lowercase().contains(normalizedQuery)
      matchesFaculty && matchesDepartment && matchesQuery
    }
  }

  /**
   * Pure search and filter helper for courses.
   */
  fun filterCourses(
    courses: List<Course>,
    teachersById: Map<String, Teacher>,
    departmentsById: Map<String, Department>,
    searchQuery: String = "",
    facultyId: String? = null,
    departmentId: String? = null,
  ): List<Course> {
    val normalizedQuery = searchQuery.trim().lowercase()
    return courses.filter { course ->
      if (!course.isActive) return@filter false
      val courseFacultyId =
        departmentsById[course.departmentId]?.facultyId
          ?: teachersById[course.teacherId]?.facultyId
      val matchesFaculty = facultyId.isNullOrBlank() || courseFacultyId == facultyId
      val matchesDepartment = departmentId.isNullOrBlank() || course.departmentId == departmentId
      val teacherName = teachersById[course.teacherId]?.fullName.orEmpty().lowercase()
      val matchesQuery =
        normalizedQuery.isEmpty() ||
          course.courseCode.lowercase().contains(normalizedQuery) ||
          course.courseName.lowercase().contains(normalizedQuery) ||
          teacherName.contains(normalizedQuery)
      matchesFaculty && matchesDepartment && matchesQuery
    }
  }

  private suspend fun fetchFacultySafely(facultyId: String): Faculty? {
    if (facultyId.isBlank()) return null
    val docRef = db.collection(COLLECTION_FACULTIES).document(facultyId)
    return try {
      val snap = docRef.get().await()
      if (!snap.exists()) return null
      snap
        .toObject(Faculty::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?.takeIf { it.isActive }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      null
    }
  }

  private suspend fun fetchDepartmentSafely(departmentId: String): Department? {
    if (departmentId.isBlank()) return null
    val docRef = db.collection(COLLECTION_DEPARTMENTS).document(departmentId)
    return try {
      val snap = docRef.get().await()
      if (!snap.exists()) return null
      snap
        .toObject(Department::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?.takeIf { it.isActive }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      null
    }
  }

  private suspend fun fetchActiveSemesterSafely(): Semester? {
    return try {
      val snapshot =
        db.collection(COLLECTION_SEMESTERS)
          .whereEqualTo("isActive", true)
          .limit(5)
          .get()
          .await()
      snapshot
        .toObjects(Semester::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .firstOrNull { it.isActive }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, COLLECTION_SEMESTERS)
      null
    }
  }

  private suspend fun fetchStudentEvaluatedKeys(studentUid: String): Set<String> {
    if (studentUid.isBlank()) return emptySet()
    return try {
      val snapshot =
        db.collection(COLLECTION_EVALUATIONS)
          .whereEqualTo("studentId", studentUid)
          .get()
          .await()
      snapshot
        .toObjects(
          TeacherEvaluation::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
        .map { eval ->
          eval.evaluationId.ifBlank {
            TeacherEvaluation.buildCompositeId(
              studentUid = eval.studentId,
              teacherId = eval.teacherId,
              courseId = eval.courseId,
              semesterId = eval.semesterId,
            )
          }
        }
        .toSet()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, COLLECTION_EVALUATIONS)
      emptySet()
    }
  }

  companion object {
    const val COLLECTION_FACULTIES = "faculties"
    const val COLLECTION_DEPARTMENTS = "departments"
    const val COLLECTION_TEACHERS = "teachers"
    const val COLLECTION_COURSES = "courses"
    const val COLLECTION_SEMESTERS = "semesters"
    const val COLLECTION_EVALUATIONS = "evaluations"
    const val DEFAULT_PAGE_LIMIT = 100L
  }
}
