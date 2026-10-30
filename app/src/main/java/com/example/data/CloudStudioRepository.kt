package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

@IgnoreExtraProperties
data class StudioCloudProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val categoryTitleAr: String = "",
    val processingModeAr: String = "",
    val aspectRatioLabel: String = "1:1 (مربع)",
    val resolutionLabel: String = "1K (1024px)",
    val outputWidth: Long = 1024L,
    val outputHeight: Long = 1024L,
    val exportFormat: String = "PNG",
    val savedFilePath: String = "",
    val originalFileName: String = "",
    val notesAr: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class CloudStudioRepository(private val db: FirebaseFirestore) {

    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProjects(userId: String? = null): Flow<List<StudioCloudProject>> = flow {
        val uid = userId ?: requireUserId()
        val path = "users/$uid/projects"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("projects")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudioCloudProject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createProject(project: StudioCloudProject): Result<String> {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("projects")
        val docRef = if (project.id.isNotBlank()) {
            collectionRef.document(project.id)
        } else {
            collectionRef.document()
        }

        val payload = mapOf(
            "userId" to uid,
            "title" to project.title.ifBlank { "مشروع مشتاق جرافكس" }.take(200),
            "categoryTitleAr" to project.categoryTitleAr.ifBlank { "موك أب" }.take(200),
            "processingModeAr" to project.processingModeAr.ifBlank { "المحافظة القصوى" }.take(200),
            "aspectRatioLabel" to project.aspectRatioLabel.ifBlank { "1:1" }.take(40),
            "resolutionLabel" to project.resolutionLabel.ifBlank { "1K" }.take(40),
            "outputWidth" to project.outputWidth.coerceIn(1L, 16384L),
            "outputHeight" to project.outputHeight.coerceIn(1L, 16384L),
            "exportFormat" to if (project.exportFormat.uppercase() == "JPEG") "JPEG" else "PNG",
            "savedFilePath" to project.savedFilePath.ifBlank { "/local/export.png" }.take(1024),
            "originalFileName" to project.originalFileName.ifBlank { "design.png" }.take(500),
            "notesAr" to project.notesAr.take(2000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserProjects(userId: String? = null): Result<List<StudioCloudProject>> {
        val uid = userId ?: requireUserId()
        val path = "users/$uid/projects"
        return try {
            val snapshot = db.collection("users")
                .document(uid)
                .collection("projects")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val list = snapshot.toObjects(
                StudioCloudProject::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getProjectById(ownerUserId: String, projectId: String): Result<StudioCloudProject?> {
        val docRef = db.collection("users")
            .document(ownerUserId)
            .collection("projects")
            .document(projectId)
        return try {
            val snap = docRef.get().await()
            val obj = snap.toObject(
                StudioCloudProject::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(obj)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProject(project: StudioCloudProject): Result<Unit> {
        val uid = requireUserId()
        require(project.id.isNotBlank()) { "Project ID must not be blank for update" }
        requireNotNull(project.createdAt) { "Project createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("projects").document(project.id)
        return try {
            docRef.set(project.copy(userId = uid, updatedAt = null)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
