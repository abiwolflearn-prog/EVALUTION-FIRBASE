package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val uid: String = "",
  val fullName: String = "",
  val email: String = "",
  val studentId: String = "",
  val role: String = ROLE_STUDENT,
  val facultyId: String = "",
  val departmentId: String = "",
  val yearOfStudy: Int = 1,
  @field:JvmField val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val isSuperAdminRole: Boolean
    get() = (role == ROLE_SUPER_ADMIN || role == ROLE_ADMIN) && isActive

  val isDepartmentAdminRole: Boolean
    get() =
      role == ROLE_DEPARTMENT_ADMIN &&
        isActive &&
        facultyId.isNotBlank() &&
        departmentId.isNotBlank()

  val isAdminRole: Boolean
    get() = isSuperAdminRole || isDepartmentAdminRole

  val isStudentRole: Boolean
    get() = role == ROLE_STUDENT && isActive

  companion object {
    const val ROLE_STUDENT = "student"
    const val ROLE_ADMIN = "admin"
    const val ROLE_SUPER_ADMIN = "super_admin"
    const val ROLE_DEPARTMENT_ADMIN = "department_admin"
  }
}
