package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CloudFile
import com.example.data.model.SharedLink
import com.example.data.model.TransferTask
import com.example.data.model.UserStorageConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DiskWalaRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeFiles(userId: String = requireUserId()): Flow<List<CloudFile>> = flow {
        val path = "files"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudFile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeSharedLinks(userId: String = requireUserId()): Flow<List<SharedLink>> = flow {
        val path = "shared_links"
        emitAll(
            db.collection(path)
                .whereEqualTo("ownerId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SharedLink::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeTransfers(userId: String = requireUserId()): Flow<List<TransferTask>> = flow {
        val path = "transfers"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TransferTask::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeStorageConfig(userId: String = requireUserId()): Flow<UserStorageConfig?> = flow {
        val path = "user_configs/$userId"
        emitAll(
            db.collection("user_configs").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserStorageConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getFileById(fileId: String): Result<CloudFile?> = runCatching {
        val docRef = db.collection("files").document(fileId)
        try {
            val snap = docRef.get().await()
            snap.toObject(CloudFile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createCloudFile(
        name: String,
        category: String,
        mimeType: String,
        sizeBytes: Long,
        parentFolderId: String = "root",
        folderPath: String = "/",
        streamUrl: String = "",
        thumbnailKey: String = "doc",
        durationSec: Long = 0L,
        resolution: String = "",
        tags: List<String> = emptyList(),
        isVaultLocked: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val fileId = "file_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("files").document(fileId)
        val payload = mapOf(
            "userId" to uid,
            "name" to name.take(200),
            "category" to category,
            "mimeType" to mimeType.take(100),
            "sizeBytes" to sizeBytes.coerceAtLeast(0L),
            "parentFolderId" to parentFolderId.take(128),
            "folderPath" to folderPath.take(300),
            "streamUrl" to streamUrl.take(1000),
            "thumbnailKey" to thumbnailKey.take(200),
            "durationSec" to durationSec.coerceAtLeast(0L),
            "resolution" to resolution.take(40),
            "playbackPositionSec" to 0L,
            "isStarred" to false,
            "isTrashed" to false,
            "isVaultLocked" to isVaultLocked,
            "isOfflineAvailable" to false,
            "tags" to tags.take(10).map { it.take(40) },
            "shareCode" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            fileId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateFileState(
        fileId: String,
        name: String? = null,
        parentFolderId: String? = null,
        folderPath: String? = null,
        playbackPositionSec: Long? = null,
        isStarred: Boolean? = null,
        isTrashed: Boolean? = null,
        isVaultLocked: Boolean? = null,
        isOfflineAvailable: Boolean? = null,
        tags: List<String>? = null,
        shareCode: String? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("files").document(fileId)
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (name != null) updates["name"] = name.take(200)
        if (parentFolderId != null) updates["parentFolderId"] = parentFolderId.take(128)
        if (folderPath != null) updates["folderPath"] = folderPath.take(300)
        if (playbackPositionSec != null) updates["playbackPositionSec"] = playbackPositionSec.coerceAtLeast(0L)
        if (isStarred != null) updates["isStarred"] = isStarred
        if (isTrashed != null) updates["isTrashed"] = isTrashed
        if (isVaultLocked != null) updates["isVaultLocked"] = isVaultLocked
        if (isOfflineAvailable != null) updates["isOfflineAvailable"] = isOfflineAvailable
        if (tags != null) updates["tags"] = tags.take(10).map { it.take(40) }
        if (shareCode != null) updates["shareCode"] = shareCode.take(64)

        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteFilePermanently(fileId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("files").document(fileId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun createSharedLink(
        file: CloudFile,
        ownerHandle: String,
        allowDownload: Boolean,
        accessPin: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val linkId = if (file.shareCode.isNotBlank()) {
            file.shareCode
        } else {
            "dw_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        }
        val docRef = db.collection("shared_links").document(linkId)
        val payload = mapOf(
            "ownerId" to uid,
            "ownerHandle" to ownerHandle.ifBlank { "DiskWala User" }.take(80),
            "fileId" to file.id.ifBlank { "file_ref" }.take(128),
            "fileName" to file.name.take(200),
            "category" to file.category,
            "mimeType" to file.mimeType.take(100),
            "sizeBytes" to file.sizeBytes.coerceAtLeast(0L),
            "streamUrl" to file.streamUrl.take(1000),
            "thumbnailKey" to file.thumbnailKey.take(200),
            "durationSec" to file.durationSec.coerceAtLeast(0L),
            "resolution" to file.resolution.take(40),
            "allowDownload" to allowDownload,
            "accessPin" to accessPin.take(16),
            "isActive" to true,
            "viewsCount" to 1L,
            "savesCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            if (file.id.isNotBlank()) {
                updateFileState(fileId = file.id, shareCode = linkId)
            }
            linkId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateSharedLinkPermissions(
        linkId: String,
        allowDownload: Boolean? = null,
        accessPin: String? = null,
        isActive: Boolean? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("shared_links").document(linkId)
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (allowDownload != null) updates["allowDownload"] = allowDownload
        if (accessPin != null) updates["accessPin"] = accessPin.take(16)
        if (isActive != null) updates["isActive"] = isActive
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun openSharedLinkByCode(rawCodeOrUrl: String): Result<SharedLink> = runCatching {
        requireUserId()
        val cleanedId = rawCodeOrUrl.trim()
            .substringAfterLast("/")
            .substringBefore("?")
            .trim()
        require(cleanedId.isNotBlank()) { "Please enter a valid DiskWala share code or URL" }
        val docRef = db.collection("shared_links").document(cleanedId)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                throw IllegalArgumentException("Shared link '$cleanedId' was not found or has expired.")
            }
            val link = snap.toObject(
                SharedLink::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw IllegalStateException("Unable to parse shared link metadata.")
            if (!link.isActive) {
                throw IllegalStateException("This shared link has been revoked by the owner.")
            }
            // Increment viewsCount
            docRef.update(
                mapOf(
                    "viewsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            link.copy(viewsCount = link.viewsCount + 1L)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException) {
                handleFirestoreError(e, OperationType.GET, docRef.path)
            }
            throw e
        }
    }

    suspend fun saveSharedLinkToMyDrive(link: SharedLink): Result<String> = runCatching {
        requireUserId()
        require(link.allowDownload) { "Owner disabled saving/downloading for this shared link." }
        val newFileId = createCloudFile(
            name = link.fileName,
            category = link.category,
            mimeType = link.mimeType,
            sizeBytes = link.sizeBytes,
            parentFolderId = "root",
            folderPath = "/Saved Links",
            streamUrl = link.streamUrl,
            thumbnailKey = link.thumbnailKey,
            durationSec = link.durationSec,
            resolution = link.resolution,
            tags = listOf("SharedLink", link.ownerHandle.take(20))
        ).getOrThrow()

        if (link.id.isNotBlank()) {
            val linkRef = db.collection("shared_links").document(link.id)
            try {
                linkRef.update(
                    mapOf(
                        "savesCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, linkRef.path)
            }
        }

        createTransferTask(
            fileId = newFileId,
            fileName = link.fileName,
            category = if (link.category == "folder") "archive" else link.category,
            direction = "cloud_save",
            totalBytes = link.sizeBytes,
            transferredBytes = link.sizeBytes,
            speedKbps = 48500L,
            isTurboBoosted = true,
            status = "completed"
        )
        newFileId
    }

    suspend fun createTransferTask(
        fileId: String,
        fileName: String,
        category: String,
        direction: String,
        totalBytes: Long,
        transferredBytes: Long,
        speedKbps: Long,
        isTurboBoosted: Boolean,
        status: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val transferId = "tr_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val docRef = db.collection("transfers").document(transferId)
        val safeCategory = if (category in listOf("video", "photo", "document", "audio", "archive")) {
            category
        } else {
            "document"
        }
        val payload = mapOf(
            "userId" to uid,
            "fileId" to fileId.ifBlank { "file_pending" }.take(128),
            "fileName" to fileName.take(200),
            "category" to safeCategory,
            "direction" to direction,
            "totalBytes" to totalBytes.coerceAtLeast(0L),
            "transferredBytes" to transferredBytes.coerceAtLeast(0L),
            "speedKbps" to speedKbps.coerceAtLeast(0L),
            "isTurboBoosted" to isTurboBoosted,
            "status" to status,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            transferId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateTransferTask(
        transferId: String,
        transferredBytes: Long? = null,
        speedKbps: Long? = null,
        isTurboBoosted: Boolean? = null,
        status: String? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("transfers").document(transferId)
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (transferredBytes != null) updates["transferredBytes"] = transferredBytes.coerceAtLeast(0L)
        if (speedKbps != null) updates["speedKbps"] = speedKbps.coerceAtLeast(0L)
        if (isTurboBoosted != null) updates["isTurboBoosted"] = isTurboBoosted
        if (status != null) updates["status"] = status
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTransferTask(transferId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("transfers").document(transferId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun ensureStorageConfig(userId: String = requireUserId()): Result<UserStorageConfig> = runCatching {
        val docRef = db.collection("user_configs").document(userId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(
                    UserStorageConfig::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserStorageConfig(id = userId, userId = userId)
            } else {
                val payload = mapOf(
                    "userId" to userId,
                    "planTier" to "free",
                    "quotaBytes" to 1099511627776L, // 1 TB
                    "vaultPinHash" to "2580",
                    "defaultStreamQuality" to "1080p",
                    "autoBackupVideos" to true,
                    "telegramLinkSync" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                UserStorageConfig(id = userId, userId = userId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateStorageConfig(
        planTier: String? = null,
        quotaBytes: Long? = null,
        vaultPinHash: String? = null,
        defaultStreamQuality: String? = null,
        autoBackupVideos: Boolean? = null,
        telegramLinkSync: Boolean? = null
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("user_configs").document(uid)
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (planTier != null) updates["planTier"] = planTier
        if (quotaBytes != null) updates["quotaBytes"] = quotaBytes
        if (vaultPinHash != null) updates["vaultPinHash"] = vaultPinHash.take(64)
        if (defaultStreamQuality != null) updates["defaultStreamQuality"] = defaultStreamQuality
        if (autoBackupVideos != null) updates["autoBackupVideos"] = autoBackupVideos
        if (telegramLinkSync != null) updates["telegramLinkSync"] = telegramLinkSync
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun seedStarterCloudDriveIfEmpty(userHandle: String = "DiskWala Creator") {
        val uid = requireUserId()
        ensureStorageConfig(uid)
        val existing = try {
            db.collection("files")
                .whereEqualTo("userId", uid)
                .limit(1)
                .get()
                .await()
        } catch (e: Exception) {
            return
        }
        if (!existing.isEmpty) return

        // Create starter folders
        val moviesFolderId = createCloudFile(
            name = "Movies & 4K Streams",
            category = "folder",
            mimeType = "inode/directory",
            sizeBytes = 0L,
            parentFolderId = "root",
            folderPath = "/",
            thumbnailKey = "folder",
            tags = listOf("4K", "Cinema")
        ).getOrNull() ?: "root"

        createCloudFile(
            name = "Work & Design Assets",
            category = "folder",
            mimeType = "inode/directory",
            sizeBytes = 0L,
            parentFolderId = "root",
            folderPath = "/",
            thumbnailKey = "folder",
            tags = listOf("Team", "Specs")
        )

        // Create streamable cloud videos (using real HTTPS MP4 sample streams)
        val scifiId = createCloudFile(
            name = "Orbital_Horizon_2160p_HDR.mp4",
            category = "video",
            mimeType = "video/mp4",
            sizeBytes = 2_485_120_000L, // 2.31 GB
            parentFolderId = "root",
            folderPath = "/",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            thumbnailKey = "scifi",
            durationSec = 15L,
            resolution = "4K 2160p HDR",
            tags = listOf("4K HDR", "Sci-Fi", "Dolby")
        ).getOrNull()

        val natureId = createCloudFile(
            name = "Iceland_Bioluminescence_Cinema_Cut.mp4",
            category = "video",
            mimeType = "video/mp4",
            sizeBytes = 1_640_250_000L, // 1.53 GB
            parentFolderId = "root",
            folderPath = "/",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            thumbnailKey = "nature",
            durationSec = 15L,
            resolution = "4K 60fps",
            tags = listOf("Nature", "Drone", "HDR10")
        ).getOrNull()

        createCloudFile(
            name = "Tears_of_Steel_Director_Master.mp4",
            category = "video",
            mimeType = "video/mp4",
            sizeBytes = 3_892_314_112L, // 3.62 GB
            parentFolderId = moviesFolderId,
            folderPath = "/Movies & 4K Streams",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            thumbnailKey = "scifi",
            durationSec = 60L,
            resolution = "4K 2160p",
            tags = listOf("Blender", "Sci-Fi")
        )

        // Create photos, documents, audio, archive, and a vault-locked item
        createCloudFile(
            name = "DiskWala_Cloud_Architecture_v4.pdf",
            category = "document",
            mimeType = "application/pdf",
            sizeBytes = 28_450_816L,
            parentFolderId = "root",
            folderPath = "/",
            thumbnailKey = "doc",
            tags = listOf("Architecture", "PDF")
        )

        createCloudFile(
            name = "Neon_Tokyo_RAW_Shoot_Pack.zip",
            category = "archive",
            mimeType = "application/zip",
            sizeBytes = 842_100_400L,
            parentFolderId = "root",
            folderPath = "/",
            thumbnailKey = "archive",
            tags = listOf("RAW", "Photos")
        )

        createCloudFile(
            name = "Synthwave_Night_Drive_Master_24bit.flac",
            category = "audio",
            mimeType = "audio/flac",
            sizeBytes = 94_371_840L,
            parentFolderId = "root",
            folderPath = "/",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
            thumbnailKey = "audio",
            durationSec = 47L,
            resolution = "Lossless FLAC",
            tags = listOf("Audio", "Master")
        )

        createCloudFile(
            name = "Passport_And_Tax_Keys_Backup.pdf",
            category = "document",
            mimeType = "application/pdf",
            sizeBytes = 14_200_000L,
            parentFolderId = "root",
            folderPath = "/",
            thumbnailKey = "doc",
            tags = listOf("Confidential", "Vault"),
            isVaultLocked = true
        )

        // Seed an active shared link and a sample transfer task
        if (scifiId != null) {
            updateFileState(fileId = scifiId, isStarred = true)
            val fileSnap = getFileById(scifiId).getOrNull()
            if (fileSnap != null) {
                createSharedLink(
                    file = fileSnap,
                    ownerHandle = userHandle,
                    allowDownload = true,
                    accessPin = ""
                )
            }
        }
        if (natureId != null) {
            createTransferTask(
                fileId = natureId,
                fileName = "Iceland_Bioluminescence_Cinema_Cut.mp4",
                category = "video",
                direction = "upload",
                totalBytes = 1_640_250_000L,
                transferredBytes = 1_640_250_000L,
                speedKbps = 64_200L,
                isTurboBoosted = true,
                status = "completed"
            )
        }
    }
}
