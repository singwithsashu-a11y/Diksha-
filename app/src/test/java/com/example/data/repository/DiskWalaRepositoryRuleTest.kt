package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DiskWalaRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createCloudFile_and_observeFiles_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = DiskWalaRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCloudFile(
                name = "Cinema_Test_4K.mp4",
                category = "video",
                mimeType = "video/mp4",
                sizeBytes = 104857600L,
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                thumbnailKey = "scifi",
                durationSec = 15L,
                resolution = "4K 2160p"
            )
        }
        assertTrue("Expected file creation to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val fileId = createResult.getOrThrow()

        val emittedFiles = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeFiles(aliceUid).first { list -> list.any { it.id == fileId } }
        }
        assertTrue(emittedFiles.any { it.id == fileId && it.name == "Cinema_Test_4K.mp4" })
    }

    @Test
    fun getFileById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = DiskWalaRepository(firestore)
        val fileId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCloudFile(
                name = "Alice_Private_Video.mp4",
                category = "video",
                mimeType = "video/mp4",
                sizeBytes = 50000L
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = DiskWalaRepository(firestore)
        val bobReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getFileById(fileId)
        }
        assertTrue("Expected cross-user read to fail", bobReadResult.isFailure)
        val firestoreException = bobReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun observeFiles_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = DiskWalaRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeFiles(aliceUid).first()
            }
            fail("Expected observeFiles while signed out to throw Exception")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    @Test
    fun createAndOpenSharedLink_crossUserCanOpenActiveLinkAndSave() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = DiskWalaRepository(firestore)
        val fileId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCloudFile(
                name = "Shared_SciFi_Trailer.mp4",
                category = "video",
                mimeType = "video/mp4",
                sizeBytes = 25000000L,
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
            ).getOrThrow()
        }
        val aliceFile = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.getFileById(fileId).getOrThrow()!!
        }
        val linkCode = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createSharedLink(
                file = aliceFile,
                ownerHandle = "AliceDrive",
                allowDownload = true,
                accessPin = ""
            ).getOrThrow()
        }

        // Bob signs in, opens Alice's shared link, and saves it to his drive
        signInTestUser(BOB_EMAIL)
        val bobRepo = DiskWalaRepository(firestore)
        val openedLink = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.openSharedLinkByCode(linkCode).getOrThrow()
        }
        assertEquals("Shared_SciFi_Trailer.mp4", openedLink.fileName)

        val savedFileId = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.saveSharedLinkToMyDrive(openedLink).getOrThrow()
        }
        assertTrue(savedFileId.isNotBlank())
    }

    private companion object {
        const val ALICE_EMAIL = "alice@diskwala.test"
        const val BOB_EMAIL = "bob@diskwala.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
