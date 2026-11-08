package com.example.base

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
abstract class FirestoreEmulatorTestBase {

  protected lateinit var firestore: FirebaseFirestore
  protected lateinit var auth: FirebaseAuth
  protected lateinit var databaseId: String

  @Before
  open fun setUpFirebase() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val resId =
      context.resources.getIdentifier(
        "firestore_database_id",
        "string",
        context.packageName,
      )
    databaseId = if (resId != 0) context.getString(resId) else "demo-database"

    val app =
      if (FirebaseApp.getApps(context).isEmpty()) {
        val newApp =
          FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
              .setApplicationId("com.aistudio.expomigration.rxnktp")
              .setProjectId(PROJECT_ID)
              .setApiKey("fake-api-key-for-emulator")
              .build(),
          )
        val newFirestore = FirebaseFirestore.getInstance(newApp, databaseId)
        newFirestore.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
        newFirestore.firestoreSettings =
          FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
            .build()

        val newAuth = FirebaseAuth.getInstance(newApp)
        newAuth.useEmulator(EMULATOR_HOST, AUTH_PORT)
        syncSecurityRulesToEmulator()
        newApp
      } else {
        FirebaseApp.getInstance()
      }

    firestore = FirebaseFirestore.getInstance(app, databaseId)
    auth = FirebaseAuth.getInstance(app)
  }

  private fun syncSecurityRulesToEmulator() {
    runCatching {
      val rulesFile =
        listOf(java.io.File("../firestore.rules"), java.io.File("firestore.rules"))
          .firstOrNull { it.exists() }
          ?: return
      val rulesContent = rulesFile.readText(Charsets.UTF_8)
      val fileObj =
        org.json.JSONObject()
          .put("name", "firestore.rules")
          .put("content", rulesContent)
      val filesArr = org.json.JSONArray().put(fileObj)
      val body =
        org.json.JSONObject()
          .put("rules", org.json.JSONObject().put("files", filesArr))
          .toString()

      val url =
        URL("http://$EMULATOR_HOST:$FIRESTORE_PORT/emulator/v1/projects/$PROJECT_ID:securityRules")
      val conn =
        (url.openConnection() as HttpURLConnection).apply {
          requestMethod = "PUT"
          doOutput = true
          setRequestProperty("Content-Type", "application/json")
        }
      conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
      conn.responseCode
      conn.disconnect()
    }
  }

  @After
  open fun tearDownFirebase() {
    auth.signOut()
  }

  // Local unit test helper ONLY (under app/src/test/).
  protected suspend fun signInTestUser(
    email: String,
    password: String = DEFAULT_PASSWORD,
  ): String =
    withContext(Dispatchers.IO) {
      withTimeout(AUTH_TIMEOUT_MS) {
        val result =
          try {
            auth.signInWithEmailAndPassword(email, password).await()
          } catch (unused: FirebaseAuthInvalidUserException) {
            auth.createUserWithEmailAndPassword(email, password).await()
          }
        checkNotNull(result.user?.uid) { "User auth failed" }
      }
    }

  protected suspend fun signInVerifiedAdminTestUser(
    email: String,
    password: String = DEFAULT_PASSWORD,
  ): String =
    withContext(Dispatchers.IO) {
      val uid = signInTestUser(email, password)
      // Mark emailVerified = true in the local Firebase Auth Emulator admin endpoint
      val url =
        URL(
          "http://$EMULATOR_HOST:$AUTH_PORT/identitytoolkit.googleapis.com/v1/projects/$PROJECT_ID/accounts:update"
        )
      val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Authorization", "Bearer owner")
      }
      val payload = """{"localId":"$uid","emailVerified":true}"""
      conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
      conn.responseCode
      conn.disconnect()

      withTimeout(AUTH_TIMEOUT_MS) {
        auth.currentUser?.getIdToken(true)?.await()
      }
      uid
    }

  protected fun Throwable.unwrapFirestoreException(): FirebaseFirestoreException? {
    var current: Throwable? = this
    while (current != null) {
      if (current is FirebaseFirestoreException) return current
      current = current.cause
    }
    return null
  }

  protected companion object {
    const val EMULATOR_HOST = "127.0.0.1"
    const val FIRESTORE_PORT = 8085
    const val AUTH_PORT = 9099
    const val PROJECT_ID = "demo-no-project"
    const val DEFAULT_PASSWORD = "Password123!"
    const val AUTH_TIMEOUT_MS = 5000L
  }
}
