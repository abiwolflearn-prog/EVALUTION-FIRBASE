package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class UserProfileRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun `1 firebase initialization configures named database and auth`() {
    assertNotNull(firestore)
    assertNotNull(auth)
    assertTrue(databaseId.isNotBlank())
  }

  @Test
  fun `2 student registration creates Firestore user profile with student role`() = runBlocking {
    val uniqueEmail = "student_${UUID.randomUUID().toString().take(8)}@wcu.edu.et"
    val uid = signInTestUser(uniqueEmail)
    val repository = UserProfileRepository(firestore, auth)

    val createResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createStudentProfile(
          fullName = "Abebe Kebede",
          email = uniqueEmail,
          studentId = "WCU/1420/16",
          facultyId = "fac_eng_tech",
          departmentId = "dep_se",
          yearOfStudy = 3,
        )
      }

    assertTrue("Expected registration profile creation to succeed", createResult.isSuccess)
    val profile = createResult.getOrThrow()
    assertEquals(uid, profile.uid)
    assertEquals("Abebe Kebede", profile.fullName)
    assertEquals("WCU/1420/16", profile.studentId)
    assertEquals(UserProfile.ROLE_STUDENT, profile.role)
    assertTrue(profile.isActive)
  }

  @Test
  fun `3 duplicate email registration is rejected by Firebase Auth`() = runBlocking {
    val duplicateEmail = "dup_${UUID.randomUUID().toString().take(8)}@wcu.edu.et"
    withContext(Dispatchers.IO) {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        auth.createUserWithEmailAndPassword(duplicateEmail, DEFAULT_PASSWORD).await()
      }
    }

    try {
      withContext(Dispatchers.IO) {
        withTimeout(DEFAULT_TIMEOUT_MS) {
          auth.createUserWithEmailAndPassword(duplicateEmail, DEFAULT_PASSWORD).await()
        }
      }
      fail("Expected FirebaseAuthUserCollisionException on duplicate email registration")
    } catch (e: FirebaseAuthUserCollisionException) {
      assertNotNull(e.message)
    }
  }

  @Test
  fun `4 valid login and 9 authentication persistence across repository reads`() = runBlocking {
    val email = "login_${UUID.randomUUID().toString().take(8)}@wcu.edu.et"
    val uid = signInTestUser(email)
    val repository = UserProfileRepository(firestore, auth)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createStudentProfile(
        fullName = "Hanna Tadesse",
        email = email,
        studentId = "WCU/2048/16",
        facultyId = "fac_med_health",
        departmentId = "dep_nurs",
        yearOfStudy = 2,
      ).getOrThrow()
    }

    // Sign out and sign back in with valid credentials
    auth.signOut()
    withContext(Dispatchers.IO) {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        auth.signInWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
      }
    }

    assertNotNull("Expected currentUser session to persist", auth.currentUser)
    assertEquals(uid, auth.currentUser?.uid)

    val fetched =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.getUserProfile(uid).getOrThrow()
      }
    assertNotNull(fetched)
    assertEquals("Hanna Tadesse", fetched?.fullName)
    assertEquals("WCU/2048/16", fetched?.studentId)
  }

  @Test
  fun `5 invalid password and 6 invalid email are rejected`() = runBlocking {
    val email = "valid_${UUID.randomUUID().toString().take(8)}@wcu.edu.et"
    signInTestUser(email, "CorrectPass123!")
    auth.signOut()

    // 5. Invalid password
    try {
      withContext(Dispatchers.IO) {
        withTimeout(DEFAULT_TIMEOUT_MS) {
          auth.signInWithEmailAndPassword(email, "WrongPassword999!").await()
        }
      }
      fail("Expected invalid password login to fail")
    } catch (e: FirebaseAuthInvalidCredentialsException) {
      assertNotNull(e.message)
    }

    // 6. Invalid email format
    try {
      withContext(Dispatchers.IO) {
        withTimeout(DEFAULT_TIMEOUT_MS) {
          auth.signInWithEmailAndPassword("not-an-email-address", DEFAULT_PASSWORD).await()
        }
      }
      fail("Expected malformed email login to fail")
    } catch (e: FirebaseAuthInvalidCredentialsException) {
      assertNotNull(e.message)
    }
  }

  @Test
  fun `7 password reset and 8 email verification execute against Firebase Auth`() = runBlocking {
    val email = "verify_${UUID.randomUUID().toString().take(8)}@wcu.edu.et"
    signInTestUser(email)

    val currentUser = checkNotNull(auth.currentUser)
    withContext(Dispatchers.IO) {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        currentUser.sendEmailVerification().await()
        auth.sendPasswordResetEmail(email).await()
      }
    }
    assertEquals(email, currentUser.email)
  }

  @Test
  fun `10 logout clears session and unauthenticated Firestore access is denied`() = runBlocking {
    val email = "logout_${UUID.randomUUID().toString().take(8)}@wcu.edu.et"
    val uid = signInTestUser(email)
    val repository = UserProfileRepository(firestore, auth)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createStudentProfile(
        fullName = "Dawit Bekele",
        email = email,
        studentId = "WCU/3010/16",
        facultyId = "fac_bus_econ",
        departmentId = "dep_acct",
        yearOfStudy = 4,
      ).getOrThrow()
    }

    auth.signOut()

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        firestore.collection("users").document(uid).get().await()
      }
      fail("Expected PERMISSION_DENIED when reading profile after logout")
    } catch (e: Exception) {
      val fsEx = checkNotNull(e.unwrapFirestoreException()) { "Expected FirebaseFirestoreException, got $e" }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
    }
  }

  @Test
  fun `11 unauthorized admin access is rejected and 12 student role escalation is prevented`() =
    runBlocking {
      val email = "student_no_admin_${UUID.randomUUID().toString().take(8)}@wcu.edu.et"
      val uid = signInTestUser(email)
      val repository = UserProfileRepository(firestore, auth)

      // Attempt to directly create a document with role = 'admin' in Firestore
      try {
        withTimeout(DEFAULT_TIMEOUT_MS) {
          firestore
            .collection("users")
            .document(uid)
            .set(
              mapOf(
                "uid" to uid,
                "fullName" to "Escalation Attempt",
                "email" to email,
                "studentId" to "WCU/9999/16",
                "role" to UserProfile.ROLE_ADMIN,
                "facultyId" to "fac_eng_tech",
                "departmentId" to "dep_cs",
                "yearOfStudy" to 3,
                "isActive" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            )
            .await()
        }
        fail("Expected PERMISSION_DENIED when student attempts to self-assign admin role on create")
      } catch (e: Exception) {
        val fsEx = checkNotNull(e.unwrapFirestoreException())
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
      }

      // Create valid student profile
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createStudentProfile(
          fullName = "Meron Hailu",
          email = email,
          studentId = "WCU/5120/16",
          facultyId = "fac_eng_tech",
          departmentId = "dep_cs",
          yearOfStudy = 3,
        ).getOrThrow()
      }

      // Verify student is NOT authorized as admin
      val isAdmin =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repository.verifyAdminAuthorization().getOrThrow()
        }
      assertFalse("Regular student must not pass admin verification", isAdmin)

      // Attempt to escalate role to 'admin' via update
      try {
        withTimeout(DEFAULT_TIMEOUT_MS) {
          firestore
            .collection("users")
            .document(uid)
            .update(
              mapOf(
                "role" to UserProfile.ROLE_ADMIN,
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            )
            .await()
        }
        fail("Expected PERMISSION_DENIED when student attempts to escalate role on update")
      } catch (e: Exception) {
        val fsEx = checkNotNull(e.unwrapFirestoreException())
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
      }
    }

  @Test
  fun `13 firestore cross-user access restrictions enforce PII isolation`() = runBlocking {
    val aliceEmail = "alice_${UUID.randomUUID().toString().take(8)}@wcu.edu.et"
    val bobEmail = "bob_${UUID.randomUUID().toString().take(8)}@wcu.edu.et"

    val aliceUid = signInTestUser(aliceEmail)
    val aliceRepo = UserProfileRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createStudentProfile(
        fullName = "Alice WCU",
        email = aliceEmail,
        studentId = "WCU/1111/16",
        facultyId = "fac_eng_tech",
        departmentId = "dep_se",
        yearOfStudy = 3,
      ).getOrThrow()
    }

    // Observe Alice's own profile succeeds
    val observedAlice =
      withTimeout(FLOW_TIMEOUT_MS) {
        aliceRepo.observeUserProfile(aliceUid).first { it != null }
      }
    assertEquals("Alice WCU", observedAlice?.fullName)

    // Switch to Bob and attempt to read Alice's profile
    signInTestUser(bobEmail)
    val bobRepo = UserProfileRepository(firestore, auth)

    val crossReadResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        bobRepo.getUserProfile(aliceUid)
      }
    assertTrue("Expected cross-user read to fail", crossReadResult.isFailure)
    val fsEx = checkNotNull(crossReadResult.exceptionOrNull()?.unwrapFirestoreException())
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
