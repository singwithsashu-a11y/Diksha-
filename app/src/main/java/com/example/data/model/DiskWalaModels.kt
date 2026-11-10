package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CloudFile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "document", // video, photo, document, audio, archive, folder
    val mimeType: String = "application/octet-stream",
    val sizeBytes: Long = 0L,
    val parentFolderId: String = "root",
    val folderPath: String = "/",
    val streamUrl: String = "",
    val thumbnailKey: String = "doc",
    val durationSec: Long = 0L,
    val resolution: String = "",
    val playbackPositionSec: Long = 0L,
    val isStarred: Boolean = false,
    val isTrashed: Boolean = false,
    val isVaultLocked: Boolean = false,
    val isOfflineAvailable: Boolean = false,
    val tags: List<String> = emptyList(),
    val shareCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SharedLink(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerHandle: String = "DiskWala User",
    val fileId: String = "",
    val fileName: String = "",
    val category: String = "video",
    val mimeType: String = "video/mp4",
    val sizeBytes: Long = 0L,
    val streamUrl: String = "",
    val thumbnailKey: String = "scifi",
    val durationSec: Long = 0L,
    val resolution: String = "1080p FHD",
    val allowDownload: Boolean = true,
    val accessPin: String = "",
    val isActive: Boolean = true,
    val viewsCount: Long = 0L,
    val savesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TransferTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fileId: String = "",
    val fileName: String = "",
    val category: String = "video",
    val direction: String = "upload", // upload, download, cloud_save
    val totalBytes: Long = 0L,
    val transferredBytes: Long = 0L,
    val speedKbps: Long = 0L,
    val isTurboBoosted: Boolean = false,
    val status: String = "transferring", // transferring, paused, completed, failed
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserStorageConfig(
    @DocumentId val id: String = "",
    val userId: String = "",
    val planTier: String = "free", // free, turbo_vip
    val quotaBytes: Long = 1099511627776L, // 1 TB Free Cloud Space like TeraBox/DiskWala
    val vaultPinHash: String = "2580",
    val defaultStreamQuality: String = "1080p", // auto, 720p, 1080p, 4k_original
    val autoBackupVideos: Boolean = true,
    val telegramLinkSync: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class StorageBreakdown(
    val totalUsedBytes: Long = 0L,
    val quotaBytes: Long = 1099511627776L,
    val videoBytes: Long = 0L,
    val photoBytes: Long = 0L,
    val documentBytes: Long = 0L,
    val audioBytes: Long = 0L,
    val archiveBytes: Long = 0L,
    val trashedBytes: Long = 0L,
    val vaultCount: Int = 0
)

fun formatByteSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    val tb = gb * 1024.0
    return when {
        bytes >= tb -> String.format(java.util.Locale.US, "%.2f TB", bytes / tb)
        bytes >= gb -> String.format(java.util.Locale.US, "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(java.util.Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(java.util.Locale.US, "%.0f KB", bytes / kb)
        else -> "$bytes B"
    }
}

fun formatDuration(seconds: Long): String {
    if (seconds <= 0L) return "00:00"
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
    }
}
