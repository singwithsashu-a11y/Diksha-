package com.example.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CloudFile
import com.example.data.model.SharedLink
import com.example.data.model.StorageBreakdown
import com.example.data.model.TransferTask
import com.example.data.model.UserStorageConfig
import com.example.data.repository.DiskWalaRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class FolderBreadcrumb(
    val id: String,
    val name: String,
    val path: String
)

enum class FileSortOption(val label: String) {
    NEWEST("Newest"),
    SIZE_DESC("Largest Size"),
    NAME_ASC("Name (A-Z)")
}

class DiskWalaViewModel(
    private val repository: DiskWalaRepository,
    private val currentUserId: String,
    private val userDisplayName: String = "DiskWala Creator"
) : ViewModel() {

    val filesState: StateFlow<UiState<List<CloudFile>>> = repository.observeFiles(currentUserId)
        .map<List<CloudFile>, UiState<List<CloudFile>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing files", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load cloud files"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val sharedLinksState: StateFlow<UiState<List<SharedLink>>> = repository.observeSharedLinks(currentUserId)
        .map<List<SharedLink>, UiState<List<SharedLink>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing shared links", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load shared links"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val transfersState: StateFlow<UiState<List<TransferTask>>> = repository.observeTransfers(currentUserId)
        .map<List<TransferTask>, UiState<List<TransferTask>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing transfers", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load transfers"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val storageConfigState: StateFlow<UiState<UserStorageConfig?>> = repository.observeStorageConfig(currentUserId)
        .map<UserStorageConfig?, UiState<UserStorageConfig?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing storage config", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load storage config"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _folderStack = MutableStateFlow(
        listOf(FolderBreadcrumb(id = "root", name = "My Cloud", path = "/"))
    )
    val folderStack: StateFlow<List<FolderBreadcrumb>> = _folderStack.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _sortOption = MutableStateFlow(FileSortOption.NEWEST)
    val sortOption: StateFlow<FileSortOption> = _sortOption.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _activeVideoFile = MutableStateFlow<CloudFile?>(null)
    val activeVideoFile: StateFlow<CloudFile?> = _activeVideoFile.asStateFlow()

    private val _openedSharedLink = MutableStateFlow<SharedLink?>(null)
    val openedSharedLink: StateFlow<SharedLink?> = _openedSharedLink.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedStarterCloudDriveIfEmpty(userDisplayName)
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun postBanner(message: String) {
        _statusBanner.value = message
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSortOption(option: FileSortOption) {
        _sortOption.value = option
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    fun openFolder(folder: CloudFile) {
        val current = _folderStack.value
        val nextPath = if (current.last().path == "/") "/${folder.name}" else "${current.last().path}/${folder.name}"
        _folderStack.value = current + FolderBreadcrumb(id = folder.id, name = folder.name, path = nextPath)
    }

    fun navigateToBreadcrumb(index: Int) {
        val current = _folderStack.value
        if (index in current.indices) {
            _folderStack.value = current.take(index + 1)
        }
    }

    fun navigateUpFolder(): Boolean {
        val current = _folderStack.value
        return if (current.size > 1) {
            _folderStack.value = current.dropLast(1)
            true
        } else {
            false
        }
    }

    fun createNewFolder(folderName: String, tags: List<String> = emptyList()) {
        val trimmed = folderName.trim()
        if (trimmed.isEmpty()) return
        val parent = _folderStack.value.last()
        viewModelScope.launch {
            repository.createCloudFile(
                name = trimmed,
                category = "folder",
                mimeType = "inode/directory",
                sizeBytes = 0L,
                parentFolderId = parent.id,
                folderPath = parent.path,
                thumbnailKey = "folder",
                tags = tags
            ).onSuccess {
                _statusBanner.value = "Folder '$trimmed' created in ${parent.name}"
            }.onFailure {
                _statusBanner.value = "Could not create folder: ${it.localizedMessage}"
            }
        }
    }

    fun uploadDeviceUri(context: Context, uri: Uri, forceCategory: String? = null) {
        viewModelScope.launch {
            val resolver = context.contentResolver
            var displayName = "Cloud_Upload_${System.currentTimeMillis()}"
            var sizeBytes = 15_728_640L // fallback 15 MB
            try {
                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex >= 0) {
                            displayName = cursor.getString(nameIndex) ?: displayName
                        }
                        if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                            sizeBytes = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Metadata extraction fallback for URI", e)
            }

            val mimeType = resolver.getType(uri) ?: guessMimeType(displayName)
            val category = forceCategory ?: categorizeByMimeOrName(mimeType, displayName)
            val parent = _folderStack.value.last()
            val isVideo = category == "video"
            val streamUrl = if (isVideo) {
                uri.toString()
            } else {
                ""
            }
            val resolution = if (isVideo) "1080p Cloud Stream" else ""
            val durationSec = if (isVideo) 120L else 0L
            val thumbKey = when (category) {
                "video" -> "scifi"
                "photo" -> "nature"
                "audio" -> "audio"
                "archive" -> "archive"
                else -> "doc"
            }

            val createRes = repository.createCloudFile(
                name = displayName,
                category = category,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                parentFolderId = parent.id,
                folderPath = parent.path,
                streamUrl = streamUrl,
                thumbnailKey = thumbKey,
                durationSec = durationSec,
                resolution = resolution,
                tags = listOf("Uploaded", category.replaceFirstChar { it.uppercase() })
            )

            createRes.onSuccess { fileId ->
                repository.createTransferTask(
                    fileId = fileId,
                    fileName = displayName,
                    category = category,
                    direction = "upload",
                    totalBytes = sizeBytes,
                    transferredBytes = sizeBytes,
                    speedKbps = 52_400L,
                    isTurboBoosted = true,
                    status = "completed"
                )
                _statusBanner.value = "Uploaded '$displayName' to ${parent.name}"
            }.onFailure {
                _statusBanner.value = "Upload failed: ${it.localizedMessage}"
            }
        }
    }

    fun uploadSampleCloudMedia(
        title: String,
        category: String,
        sizeBytes: Long,
        streamUrl: String,
        resolution: String,
        durationSec: Long,
        thumbnailKey: String,
        tags: List<String>
    ) {
        val parent = _folderStack.value.last()
        viewModelScope.launch {
            val mimeType = when (category) {
                "video" -> "video/mp4"
                "audio" -> "audio/flac"
                "photo" -> "image/jpeg"
                "archive" -> "application/zip"
                else -> "application/pdf"
            }
            repository.createCloudFile(
                name = title,
                category = category,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                parentFolderId = parent.id,
                folderPath = parent.path,
                streamUrl = streamUrl,
                thumbnailKey = thumbnailKey,
                durationSec = durationSec,
                resolution = resolution,
                tags = tags
            ).onSuccess { fileId ->
                repository.createTransferTask(
                    fileId = fileId,
                    fileName = title,
                    category = category,
                    direction = "upload",
                    totalBytes = sizeBytes,
                    transferredBytes = sizeBytes,
                    speedKbps = 78_900L,
                    isTurboBoosted = true,
                    status = "completed"
                )
                _statusBanner.value = "Added '$title' to Cloud Drive"
            }
        }
    }

    fun toggleStar(file: CloudFile) {
        viewModelScope.launch {
            repository.updateFileState(
                fileId = file.id,
                isStarred = !file.isStarred
            )
        }
    }

    fun toggleOfflineAvailable(file: CloudFile) {
        viewModelScope.launch {
            val nextOffline = !file.isOfflineAvailable
            repository.updateFileState(
                fileId = file.id,
                isOfflineAvailable = nextOffline
            ).onSuccess {
                if (nextOffline) {
                    repository.createTransferTask(
                        fileId = file.id,
                        fileName = file.name,
                        category = if (file.category == "folder") "archive" else file.category,
                        direction = "download",
                        totalBytes = file.sizeBytes,
                        transferredBytes = file.sizeBytes,
                        speedKbps = 68_400L,
                        isTurboBoosted = true,
                        status = "completed"
                    )
                    _statusBanner.value = "'${file.name}' cached for Offline Turbo playback"
                } else {
                    _statusBanner.value = "Removed '${file.name}' from offline cache"
                }
            }
        }
    }

    fun toggleVaultLock(file: CloudFile) {
        viewModelScope.launch {
            val nextVault = !file.isVaultLocked
            repository.updateFileState(
                fileId = file.id,
                isVaultLocked = nextVault
            ).onSuccess {
                _statusBanner.value = if (nextVault) {
                    "Moved '${file.name}' to Private Safe Vault"
                } else {
                    "Restored '${file.name}' from Safe Vault to Cloud Drive"
                }
            }
        }
    }

    fun moveToTrash(file: CloudFile) {
        viewModelScope.launch {
            repository.updateFileState(
                fileId = file.id,
                isTrashed = true
            ).onSuccess {
                _statusBanner.value = "Moved '${file.name}' to Recycle Bin"
            }
        }
    }

    fun restoreFromTrash(file: CloudFile) {
        viewModelScope.launch {
            repository.updateFileState(
                fileId = file.id,
                isTrashed = false
            ).onSuccess {
                _statusBanner.value = "Restored '${file.name}' to Cloud Drive"
            }
        }
    }

    fun deleteFilePermanently(file: CloudFile) {
        viewModelScope.launch {
            repository.deleteFilePermanently(file.id).onSuccess {
                _statusBanner.value = "Permanently deleted '${file.name}'"
            }
        }
    }

    fun renameFile(file: CloudFile, newName: String, newTags: List<String>) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.updateFileState(
                fileId = file.id,
                name = trimmed,
                tags = newTags
            ).onSuccess {
                _statusBanner.value = "Updated '${trimmed}'"
            }
        }
    }

    fun startVideoStream(file: CloudFile) {
        _activeVideoFile.value = file
    }

    fun closeVideoStream() {
        _activeVideoFile.value = null
    }

    fun savePlaybackPosition(file: CloudFile, positionSec: Long) {
        if (file.id.isBlank()) return
        viewModelScope.launch {
            repository.updateFileState(
                fileId = file.id,
                playbackPositionSec = positionSec
            )
            _activeVideoFile.value = file.copy(playbackPositionSec = positionSec)
        }
    }

    fun generateShareLink(
        file: CloudFile,
        allowDownload: Boolean,
        accessPin: String,
        onLinkReady: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.createSharedLink(
                file = file,
                ownerHandle = userDisplayName,
                allowDownload = allowDownload,
                accessPin = accessPin.trim()
            ).onSuccess { code ->
                val fullUrl = "https://diskwala.app/s/$code"
                _statusBanner.value = "Share link ready: $fullUrl"
                onLinkReady(fullUrl)
            }.onFailure {
                _statusBanner.value = "Could not create share link: ${it.localizedMessage}"
            }
        }
    }

    fun updateSharedLinkSettings(
        link: SharedLink,
        allowDownload: Boolean? = null,
        accessPin: String? = null,
        isActive: Boolean? = null
    ) {
        viewModelScope.launch {
            repository.updateSharedLinkPermissions(
                linkId = link.id,
                allowDownload = allowDownload,
                accessPin = accessPin,
                isActive = isActive
            ).onSuccess {
                _statusBanner.value = "Updated share link permissions"
            }
        }
    }

    fun lookupSharedLink(codeOrUrl: String) {
        viewModelScope.launch {
            repository.openSharedLinkByCode(codeOrUrl)
                .onSuccess { link ->
                    _openedSharedLink.value = link
                }
                .onFailure { err ->
                    _statusBanner.value = err.localizedMessage ?: "Shared link not found"
                }
        }
    }

    fun clearOpenedSharedLink() {
        _openedSharedLink.value = null
    }

    fun saveOpenedLinkToDrive(link: SharedLink) {
        viewModelScope.launch {
            repository.saveSharedLinkToMyDrive(link)
                .onSuccess {
                    _statusBanner.value = "Saved '${link.fileName}' to /Saved Links in your Cloud Drive!"
                    _openedSharedLink.value = null
                }
                .onFailure { err ->
                    _statusBanner.value = err.localizedMessage ?: "Failed to save shared link"
                }
        }
    }

    fun streamFromSharedLink(link: SharedLink) {
        val virtualFile = CloudFile(
            id = link.fileId,
            userId = currentUserId,
            name = link.fileName,
            category = link.category,
            mimeType = link.mimeType,
            sizeBytes = link.sizeBytes,
            streamUrl = link.streamUrl.ifBlank {
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
            },
            thumbnailKey = link.thumbnailKey,
            durationSec = link.durationSec.coerceAtLeast(15L),
            resolution = link.resolution.ifBlank { "1080p Shared Stream" },
            shareCode = link.id
        )
        _openedSharedLink.value = null
        _activeVideoFile.value = virtualFile
    }

    fun toggleTransferPause(task: TransferTask) {
        if (task.status == "completed") return
        val nextStatus = if (task.status == "paused") "transferring" else "paused"
        viewModelScope.launch {
            repository.updateTransferTask(
                transferId = task.id,
                status = nextStatus
            )
        }
    }

    fun boostTransferTurbo(task: TransferTask) {
        viewModelScope.launch {
            repository.updateTransferTask(
                transferId = task.id,
                isTurboBoosted = true,
                speedKbps = 84_500L,
                transferredBytes = task.totalBytes,
                status = "completed"
            ).onSuccess {
                _statusBanner.value = "Turbo High-Speed Channel completed '${task.fileName}'"
            }
        }
    }

    fun removeTransferTask(task: TransferTask) {
        viewModelScope.launch {
            repository.deleteTransferTask(task.id)
        }
    }

    fun clearCompletedTransfers(tasks: List<TransferTask>) {
        viewModelScope.launch {
            tasks.filter { it.status == "completed" }.forEach {
                repository.deleteTransferTask(it.id)
            }
            _statusBanner.value = "Cleared completed transfer history"
        }
    }

    fun unlockVault(pinInput: String, config: UserStorageConfig?): Boolean {
        val expected = config?.vaultPinHash?.ifBlank { "2580" } ?: "2580"
        return if (pinInput.trim() == expected) {
            _isVaultUnlocked.value = true
            _statusBanner.value = "Private Safe Vault unlocked"
            true
        } else {
            false
        }
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    fun updateVaultPin(newPin: String) {
        if (newPin.length < 4) return
        viewModelScope.launch {
            repository.updateStorageConfig(vaultPinHash = newPin.take(8)).onSuccess {
                _statusBanner.value = "Updated Safe Vault PIN"
            }
        }
    }

    fun toggleTurboVipPlan(currentConfig: UserStorageConfig?) {
        val isCurrentlyTurbo = currentConfig?.planTier == "turbo_vip"
        val nextTier = if (isCurrentlyTurbo) "free" else "turbo_vip"
        val nextQuota = if (isCurrentlyTurbo) 1099511627776L else 4398046511104L // 1 TB vs 4 TB
        viewModelScope.launch {
            repository.updateStorageConfig(
                planTier = nextTier,
                quotaBytes = nextQuota,
                defaultStreamQuality = if (nextTier == "turbo_vip") "4k_original" else "1080p"
            ).onSuccess {
                _statusBanner.value = if (nextTier == "turbo_vip") {
                    "Turbo VIP Activated: 4 TB Cloud Storage + 4K Original Quality Streaming!"
                } else {
                    "Switched to Standard 1 TB Free Plan"
                }
            }
        }
    }

    fun updatePreferences(
        defaultStreamQuality: String? = null,
        autoBackupVideos: Boolean? = null,
        telegramLinkSync: Boolean? = null
    ) {
        viewModelScope.launch {
            repository.updateStorageConfig(
                defaultStreamQuality = defaultStreamQuality,
                autoBackupVideos = autoBackupVideos,
                telegramLinkSync = telegramLinkSync
            )
        }
    }

    fun computeStorageBreakdown(files: List<CloudFile>, config: UserStorageConfig?): StorageBreakdown {
        val activeFiles = files.filter { !it.isTrashed }
        val videoBytes = activeFiles.filter { it.category == "video" }.sumOf { it.sizeBytes }
        val photoBytes = activeFiles.filter { it.category == "photo" }.sumOf { it.sizeBytes }
        val documentBytes = activeFiles.filter { it.category == "document" }.sumOf { it.sizeBytes }
        val audioBytes = activeFiles.filter { it.category == "audio" }.sumOf { it.sizeBytes }
        val archiveBytes = activeFiles.filter { it.category == "archive" }.sumOf { it.sizeBytes }
        val trashedBytes = files.filter { it.isTrashed }.sumOf { it.sizeBytes }
        val totalUsed = videoBytes + photoBytes + documentBytes + audioBytes + archiveBytes + trashedBytes
        return StorageBreakdown(
            totalUsedBytes = totalUsed,
            quotaBytes = config?.quotaBytes ?: 1099511627776L,
            videoBytes = videoBytes,
            photoBytes = photoBytes,
            documentBytes = documentBytes,
            audioBytes = audioBytes,
            archiveBytes = archiveBytes,
            trashedBytes = trashedBytes,
            vaultCount = activeFiles.count { it.isVaultLocked }
        )
    }

    private fun guessMimeType(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4", "mkv", "mov", "webm" -> "video/mp4"
            "jpg", "jpeg", "png", "webp", "heic" -> "image/jpeg"
            "mp3", "flac", "wav", "m4a" -> "audio/mpeg"
            "zip", "rar", "7z", "tar" -> "application/zip"
            "pdf" -> "application/pdf"
            else -> "application/octet-stream"
        }
    }

    private fun categorizeByMimeOrName(mime: String, name: String): String {
        val lowerMime = mime.lowercase()
        val ext = name.substringAfterLast('.', "").lowercase()
        return when {
            lowerMime.startsWith("video/") || ext in listOf("mp4", "mkv", "mov", "avi", "webm") -> "video"
            lowerMime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "webp", "gif", "heic") -> "photo"
            lowerMime.startsWith("audio/") || ext in listOf("mp3", "flac", "wav", "aac", "m4a") -> "audio"
            ext in listOf("zip", "rar", "7z", "tar", "gz", "apk") || lowerMime.contains("zip") -> "archive"
            else -> "document"
        }
    }

    private companion object {
        const val TAG = "DiskWalaVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
