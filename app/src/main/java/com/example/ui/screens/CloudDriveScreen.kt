package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CloudFile
import com.example.data.model.StorageBreakdown
import com.example.data.model.formatByteSize
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TurboGold
import com.example.ui.theme.TurboIndigo
import com.example.ui.theme.TurboViolet
import com.example.viewmodel.FileSortOption
import com.example.viewmodel.FolderBreadcrumb
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudDriveScreen(
    files: List<CloudFile>,
    storageBreakdown: StorageBreakdown,
    isTurboVip: Boolean,
    folderStack: List<FolderBreadcrumb>,
    searchQuery: String,
    selectedCategory: String,
    sortOption: FileSortOption,
    isGridView: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onSortChange: (FileSortOption) -> Unit,
    onToggleGridView: () -> Unit,
    onOpenFolder: (CloudFile) -> Unit,
    onNavigateBreadcrumb: (Int) -> Unit,
    onNavigateUpFolder: () -> Boolean,
    onUploadPhotoVideoClick: () -> Unit,
    onUploadDocumentClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
    onOpenSharedLinkClick: () -> Unit,
    onPlayVideo: (CloudFile) -> Unit,
    onShareFile: (CloudFile) -> Unit,
    onToggleStar: (CloudFile) -> Unit,
    onToggleOffline: (CloudFile) -> Unit,
    onToggleVaultLock: (CloudFile) -> Unit,
    onRenameFile: (CloudFile, String, List<String>) -> Unit,
    onMoveToTrash: (CloudFile) -> Unit
) {
    if (folderStack.size > 1) {
        BackHandler {
            onNavigateUpFolder()
        }
    }

    var selectedFileForSheet by remember { mutableStateOf<CloudFile?>(null) }
    var fileToRename by remember { mutableStateOf<CloudFile?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    val currentFolderId = folderStack.last().id
    val visibleFiles = remember(files, currentFolderId, searchQuery, selectedCategory, sortOption) {
        val base = files.filter { !it.isTrashed && !it.isVaultLocked }
        val filtered = base.filter { file ->
            val matchesQuery = if (searchQuery.isBlank()) {
                if (selectedCategory == "all") {
                    file.parentFolderId == currentFolderId
                } else {
                    true
                }
            } else {
                val q = searchQuery.trim().lowercase()
                file.name.lowercase().contains(q) ||
                    file.tags.any { it.lowercase().contains(q) } ||
                    file.folderPath.lowercase().contains(q)
            }
            val matchesCat = when (selectedCategory) {
                "all" -> true
                "starred" -> file.isStarred
                "offline" -> file.isOfflineAvailable
                else -> file.category == selectedCategory
            }
            matchesQuery && matchesCat
        }

        val sorted = when (sortOption) {
            FileSortOption.NEWEST -> filtered.sortedWith(
                compareByDescending<CloudFile> { it.category == "folder" }
                    .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            )
            FileSortOption.SIZE_DESC -> filtered.sortedWith(
                compareByDescending<CloudFile> { it.category == "folder" }
                    .thenByDescending { it.sizeBytes }
            )
            FileSortOption.NAME_ASC -> filtered.sortedWith(
                compareByDescending<CloudFile> { it.category == "folder" }
                    .thenBy { it.name.lowercase() }
            )
        }
        sorted
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cloud_drive_file_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StorageQuickHeaderCard(
                storageBreakdown = storageBreakdown,
                isTurboVip = isTurboVip,
                onUploadPhotoVideoClick = onUploadPhotoVideoClick,
                onUploadDocumentClick = onUploadDocumentClick,
                onCreateFolderClick = onCreateFolderClick,
                onOpenSharedLinkClick = onOpenSharedLinkClick
            )
        }

        item {
            // Search & View Mode Controls
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search cloud files, 4K movies, folders, tags…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ElectricCyan
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_files_input")
            )
        }

        item {
            // Category Filter Chips
            val categories = listOf(
                "all" to "All Files",
                "video" to "Videos & 4K",
                "photo" to "Photos",
                "document" to "Docs",
                "audio" to "Audio",
                "archive" to "Archives",
                "starred" to "Starred",
                "offline" to "Offline"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedCategory == key,
                        onClick = { onCategorySelect(key) },
                        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                            selectedLabelColor = ElectricCyan
                        ),
                        modifier = Modifier.testTag("category_chip_$key")
                    )
                }
            }
        }

        item {
            // Breadcrumb & Sort Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    folderStack.forEachIndexed { index, crumb ->
                        val isLast = index == folderStack.lastIndex
                        Text(
                            text = crumb.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isLast) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onNavigateBreadcrumb(index) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                        if (!isLast) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        TextButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort files",
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sortOption.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            FileSortOption.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.label) },
                                    onClick = {
                                        onSortChange(opt)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(onClick = onToggleGridView) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle Grid/List View",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (visibleFiles.isEmpty()) {
            item {
                Surface(
                    color = ObsidianCard.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Empty folder",
                            tint = ElectricCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No matching files in this view",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Use the quick actions above to upload videos, photos, or documents, or open a shared DiskWala link.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(visibleFiles, key = { it.id }) { file ->
                CloudFileRowCard(
                    file = file,
                    onClick = {
                        when (file.category) {
                            "folder" -> onOpenFolder(file)
                            "video", "audio" -> onPlayVideo(file)
                            else -> selectedFileForSheet = file
                        }
                    },
                    onShareClick = { onShareFile(file) },
                    onStarClick = { onToggleStar(file) },
                    onMoreClick = { selectedFileForSheet = file }
                )
            }
        }
    }

    // Contextual Bottom Sheet for File Actions
    val sheetFile = selectedFileForSheet
    if (sheetFile != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedFileForSheet = null },
            containerColor = ObsidianCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = sheetFile.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = "${formatByteSize(sheetFile.sizeBytes)} • ${sheetFile.mimeType} • ${sheetFile.folderPath}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (sheetFile.category == "video" || sheetFile.category == "audio") {
                    SheetActionItem(
                        icon = Icons.Default.PlayCircleFilled,
                        label = "Stream Directly in Built-in Player",
                        tint = ElectricCyan
                    ) {
                        selectedFileForSheet = null
                        onPlayVideo(sheetFile)
                    }
                }

                SheetActionItem(
                    icon = Icons.Default.Share,
                    label = if (sheetFile.shareCode.isNotBlank()) "Manage / Copy Share Link (${sheetFile.shareCode})" else "Create Instant Shareable Link",
                    tint = ElectricCyan
                ) {
                    selectedFileForSheet = null
                    onShareFile(sheetFile)
                }

                SheetActionItem(
                    icon = Icons.Default.CloudDownload,
                    label = if (sheetFile.isOfflineAvailable) "Remove from Offline Cache" else "Download for Offline Turbo Access",
                    tint = EmeraldSuccess
                ) {
                    selectedFileForSheet = null
                    onToggleOffline(sheetFile)
                }

                SheetActionItem(
                    icon = Icons.Default.Lock,
                    label = "Move to Encrypted Safe Vault",
                    tint = TurboGold
                ) {
                    selectedFileForSheet = null
                    onToggleVaultLock(sheetFile)
                }

                SheetActionItem(
                    icon = Icons.Default.Edit,
                    label = "Rename & Edit Tags",
                    tint = Color.White
                ) {
                    fileToRename = sheetFile
                    selectedFileForSheet = null
                }

                SheetActionItem(
                    icon = Icons.Default.Delete,
                    label = "Move to Recycle Bin",
                    tint = CoralDanger
                ) {
                    selectedFileForSheet = null
                    onMoveToTrash(sheetFile)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    val targetRename = fileToRename
    if (targetRename != null) {
        var nameInput by remember(targetRename.id) { mutableStateOf(targetRename.name) }
        var tagsInput by remember(targetRename.id) { mutableStateOf(targetRename.tags.joinToString(", ")) }
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Rename & Tag Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("File / Folder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        label = { Text("Tags (comma separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedTags = tagsInput.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .take(10)
                        onRenameFile(targetRename, nameInput, parsedTags)
                        fileToRename = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StorageQuickHeaderCard(
    storageBreakdown: StorageBreakdown,
    isTurboVip: Boolean,
    onUploadPhotoVideoClick: () -> Unit,
    onUploadDocumentClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
    onOpenSharedLinkClick: () -> Unit
) {
    val ratio = (storageBreakdown.totalUsedBytes.toFloat() / storageBreakdown.quotaBytes.coerceAtLeast(1L).toFloat())
        .coerceIn(0.01f, 1f)

    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        ElectricCyan.copy(alpha = 0.45f),
                        TurboIndigo.copy(alpha = 0.35f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DiskWala Cloud Space",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${formatByteSize(storageBreakdown.totalUsedBytes)} used of ${formatByteSize(storageBreakdown.quotaBytes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                }
                Surface(
                    color = if (isTurboVip) TurboGold.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (isTurboVip) "⚡ TURBO VIP 4TB" else "1 TB CLOUD",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isTurboVip) TurboGold else ElectricCyan,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { ratio },
                color = ElectricCyan,
                trackColor = ObsidianElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickActionButton(
                    icon = Icons.Default.CloudUpload,
                    label = "Upload Media",
                    color = ElectricCyan,
                    testTag = "quick_upload_media_btn",
                    onClick = onUploadPhotoVideoClick
                )
                QuickActionButton(
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    label = "Upload File",
                    color = TurboIndigo,
                    testTag = "quick_upload_doc_btn",
                    onClick = onUploadDocumentClick
                )
                QuickActionButton(
                    icon = Icons.Default.CreateNewFolder,
                    label = "New Folder",
                    color = TurboGold,
                    testTag = "quick_new_folder_btn",
                    onClick = onCreateFolderClick
                )
                QuickActionButton(
                    icon = Icons.Default.Link,
                    label = "Open Link",
                    color = EmeraldSuccess,
                    testTag = "quick_open_link_btn",
                    onClick = onOpenSharedLinkClick
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@Composable
private fun CloudFileRowCard(
    file: CloudFile,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
    onStarClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val (icon, tint) = when (file.category) {
        "folder" -> Icons.Default.Folder to TurboGold
        "video" -> Icons.Default.Movie to ElectricCyan
        "photo" -> Icons.Default.Image to EmeraldSuccess
        "audio" -> Icons.Default.AudioFile to TurboViolet
        "archive" -> Icons.Default.FolderZip to TurboIndigo
        else -> Icons.AutoMirrored.Filled.InsertDriveFile to Color(0xFF60A5FA)
    }

    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("file_item_${file.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = file.category,
                    tint = tint,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (file.resolution.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = ElectricCyan.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = file.resolution,
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (file.category == "folder") "Cloud Folder • ${file.folderPath}" else "${formatByteSize(file.sizeBytes)} • ${file.folderPath}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (file.shareCode.isNotBlank()) {
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "SHARED",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                if (file.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        file.tags.take(3).forEach { tag ->
                            Surface(
                                color = ObsidianElevated,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            IconButton(onClick = onStarClick) {
                Icon(
                    imageVector = if (file.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Star item",
                    tint = if (file.isStarred) TurboGold else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (file.category != "folder") {
                IconButton(onClick = onShareClick) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share link",
                        tint = ElectricCyan
                    )
                }
            }

            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More actions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SheetActionItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}
