package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CloudStudioRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun buildSampleCloudProject(uid: String) = StudioCloudProject(
        id = "proj_${UUID.randomUUID().toString().replace("-", "")}",
        userId = uid,
        title = "موك أب أكياس بلاستيكية",
        categoryTitleAr = "موك أب أكياس بلاستيكية",
        processingModeAr = "المحافظة القصوى على الأصل",
        aspectRatioLabel = "1:1 (مربع)",
        resolutionLabel = "1K (1024px)",
        outputWidth = 1024L,
        outputHeight = 1024L,
        exportFormat = "PNG",
        savedFilePath = "/local/moshtaq_exports/test.png",
        originalFileName = "logo.png",
        notesAr = "اختبار قواعد الأمان"
    )

    @Test
    fun createProject_validPayload_createsDocumentAndReturnsId() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = CloudStudioRepository(firestore)
        val project = buildSampleCloudProject(aliceUid)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createProject(project) }
        assertTrue(createResult.isSuccess)
        val createdId = createResult.getOrThrow()
        assertEquals(project.id, createdId)
    }

    @Test
    fun getUserProjects_authenticatedOwner_returnsMatchingProjects() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = CloudStudioRepository(firestore)
        val project = buildSampleCloudProject(aliceUid)
        val projectId = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createProject(project).getOrThrow() }

        val projectsResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getUserProjects(aliceUid) }
        assertTrue(projectsResult.isSuccess)
        assertTrue(projectsResult.getOrThrow().any { it.id == projectId })
    }

    @Test
    fun observeUserProjects_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = CloudStudioRepository(firestore)
        val project = buildSampleCloudProject(aliceUid)
        val projectId = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createProject(project).getOrThrow() }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProjects(aliceUid).first { list -> list.any { it.id == projectId } }
        }
        assertTrue(emitted.any { it.id == projectId })
    }

    @Test
    fun getProjectById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CloudStudioRepository(firestore)
        val project = buildSampleCloudProject(aliceUid)
        val projectId = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.createProject(project).getOrThrow() }

        signInTestUser(BOB_EMAIL)
        val bobRepo = CloudStudioRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getProjectById(aliceUid, projectId) }
        assertTrue(result.isFailure)
        val firestoreEx = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeUserProjects_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = CloudStudioRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeUserProjects(aliceUid).first()
            }
            fail("Expected exception for unauthenticated user")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
