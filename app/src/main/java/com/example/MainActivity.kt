package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.DiskWalaSignInScreen
import com.example.auth.signOut
import com.example.data.model.CloudFile
import com.example.data.model.formatByteSize
import com.example.data.repository.DiskWalaRepository
import com.example.ui.screens.CloudDriveScreen
import com.example.ui.screens.ShareLinksScreen
import com.example.ui.screens.TransfersScreen
import com.example.ui.screens.VaultAndSpaceScreen
import com.example.ui.screens.VideoStreamHubScreen
import com.example.ui.screens.VideoStreamPlayerModal
import com.example.ui.screens.copyToClipboard
import com.example.ui.screens.shareTextIntent
import com.example.ui.theme.DiskWalaTheme
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TurboGold
import com.example.viewmodel.DiskWalaViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiskWalaTheme {
                AppNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        DiskWalaSignInScreen(
            onAuthSuccess = {}
        )
    } else {
        key(user.uid) {
            DiskWalaMainApp(
                currentUserId = user.uid,
                userDisplayName = user.displayName?.ifBlank { null }
                    ?: user.email?.substringBefore("@")
                    ?: "DiskWala Creator",
                userEmailOrName = user.email ?: user.displayName ?: "Authenticated Cloud Account"
            )
        }
    }
}

enum class DiskWalaTab(val route: String) {
    DRIVE("drive"),
    CINEMA("cinema"),
    LINKS("links"),
    TRANSFERS("transfers"),
    VAULT("vault")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiskWalaMainApp(
    currentUserId: String,
    userDisplayName: String,
    userEmailOrName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: DiskWalaViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                DiskWalaViewModel(
                    repository = DiskWalaRepository(db),
                    currentUserId = currentUserId,
                    userDisplayName = userDisplayName
                )
            }
        }
    )

    val filesState by viewModel.filesState.collectAsStateWithLifecycle()
    val sharedLinksState by viewModel.sharedLinksState.collectAsStateWithLifecycle()
    val transfersState by viewModel.transfersState.collectAsStateWithLifecycle()
    val storageConfigState by viewModel.storageConfigState.collectAsStateWithLifecycle()
    val folderStack by viewModel.folderStack.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val activeVideoFile by viewModel.activeVideoFile.collectAsStateWithLifecycle()
    val openedSharedLink by viewModel.openedSharedLink.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()

    val files = (filesState as? UiState.Success)?.data ?: emptyList()
    val sharedLinks = (sharedLinksState as? UiState.Success)?.data ?: emptyList()
    val transfers = (transfersState as? UiState.Success)?.data ?: emptyList()
    val storageConfig = (storageConfigState as? UiState.Success)?.data
    val isTurboVip = storageConfig?.planTier == "turbo_vip"
    val storageBreakdown = remember(files, storageConfig) {
        viewModel.computeStorageBreakdown(files, storageConfig)
    }

    var currentTab by rememberSaveable { mutableStateOf(DiskWalaTab.DRIVE) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showOpenLinkDialog by remember { mutableStateOf(false) }
    var fileToShareDialog by remember { mutableStateOf<CloudFile?>(null) }

    // Zero-permission Android Photo Picker for uploading photos & videos
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadDeviceUri(context, uri)
        }
    }

    // Zero-permission SAF Document Picker for uploading PDFs, archives, audio, etc.
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadDeviceUri(context, uri)
        }
    }

    // Fullscreen Built-in Cloud Video Streamer Modal
    val currentPlayingFile = activeVideoFile
    if (currentPlayingFile != null) {
        VideoStreamPlayerModal(
            file = currentPlayingFile,
            isTurboVip = isTurboVip,
            defaultQuality = storageConfig?.defaultStreamQuality ?: "1080p",
            onSavePosition = { pos -> viewModel.savePlaybackPosition(currentPlayingFile, pos) },
            onShare = { fileToShareDialog = it },
            onToggleOffline = { viewModel.toggleOfflineAvailable(it) },
            onClose = { viewModel.closeVideoStream() }
        )
        return
    }

    Scaffold(
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(
                color = ObsidianSurface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets(top = 8.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "DiskWala Logo",
                                tint = ElectricCyan,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Cloud Drive & 4K Stream Hub",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = if (isTurboVip) TurboGold.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(50),
                                onClick = { viewModel.toggleTurboVipPlan(storageConfig) },
                                modifier = Modifier.testTag("top_turbo_badge_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Turbo Status",
                                        tint = if (isTurboVip) TurboGold else ElectricCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isTurboVip) "TURBO 4TB" else "TURBO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isTurboVip) TurboGold else ElectricCyan
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showOpenLinkDialog = true },
                                modifier = Modifier.testTag("top_open_shared_link_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "Open Shared Link",
                                    tint = ElectricCyan
                                )
                            }
                        }
                    }

                    // Transient status banner
                    AnimatedVisibility(visible = statusBanner != null) {
                        Surface(
                            color = ElectricCyan.copy(alpha = 0.16f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = statusBanner ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ElectricCyan,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearStatusBanner() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss notification",
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentTab == DiskWalaTab.DRIVE || currentTab == DiskWalaTab.CINEMA) {
                ExtendedFloatingActionButton(
                    onClick = {
                        mediaPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    containerColor = ElectricCyan,
                    contentColor = Color(0xFF031926),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Upload to Cloud"
                        )
                    },
                    text = {
                        Text(
                            text = "Upload",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("fab_upload_button")
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = ObsidianSurface,
                windowInsets = WindowInsets.navigationBars
            ) {
                val navItems = listOf(
                    Triple(DiskWalaTab.DRIVE, stringResource(R.string.nav_drive), Icons.Default.Cloud to Icons.Outlined.Cloud),
                    Triple(DiskWalaTab.CINEMA, stringResource(R.string.nav_cinema), Icons.Default.PlayCircleFilled to Icons.Outlined.PlayCircleOutline),
                    Triple(DiskWalaTab.LINKS, stringResource(R.string.nav_links), Icons.Default.Share to Icons.Outlined.Share),
                    Triple(DiskWalaTab.TRANSFERS, stringResource(R.string.nav_transfers), Icons.Default.SwapVert to Icons.Outlined.SwapVert),
                    Triple(DiskWalaTab.VAULT, stringResource(R.string.nav_vault), Icons.Default.Security to Icons.Outlined.Security)
                )
                navItems.forEach { (tab, label, icons) ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selected) icons.first else icons.second,
                                contentDescription = label
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF031926),
                            selectedTextColor = ElectricCyan,
                            indicatorColor = ElectricCyan,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBg)
        ) {
            when (currentTab) {
                DiskWalaTab.DRIVE -> {
                    CloudDriveScreen(
                        files = files,
                        storageBreakdown = storageBreakdown,
                        isTurboVip = isTurboVip,
                        folderStack = folderStack,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        sortOption = sortOption,
                        isGridView = isGridView,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onCategorySelect = viewModel::selectCategory,
                        onSortChange = viewModel::updateSortOption,
                        onToggleGridView = viewModel::toggleGridView,
                        onOpenFolder = viewModel::openFolder,
                        onNavigateBreadcrumb = viewModel::navigateToBreadcrumb,
                        onNavigateUpFolder = viewModel::navigateUpFolder,
                        onUploadPhotoVideoClick = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        onUploadDocumentClick = {
                            documentPickerLauncher.launch(arrayOf("*/*"))
                        },
                        onCreateFolderClick = { showNewFolderDialog = true },
                        onOpenSharedLinkClick = { showOpenLinkDialog = true },
                        onPlayVideo = viewModel::startVideoStream,
                        onShareFile = { fileToShareDialog = it },
                        onToggleStar = viewModel::toggleStar,
                        onToggleOffline = viewModel::toggleOfflineAvailable,
                        onToggleVaultLock = viewModel::toggleVaultLock,
                        onRenameFile = viewModel::renameFile,
                        onMoveToTrash = viewModel::moveToTrash
                    )
                }

                DiskWalaTab.CINEMA -> {
                    VideoStreamHubScreen(
                        files = files,
                        isTurboVip = isTurboVip,
                        onPlayVideo = viewModel::startVideoStream,
                        onShareVideo = { fileToShareDialog = it },
                        onToggleOffline = viewModel::toggleOfflineAvailable,
                        onOpenSharedLinkDialog = { showOpenLinkDialog = true }
                    )
                }

                DiskWalaTab.LINKS -> {
                    ShareLinksScreen(
                        sharedLinks = sharedLinks,
                        onLookupLink = viewModel::lookupSharedLink,
                        onUpdateLinkPermissions = viewModel::updateSharedLinkSettings,
                        onBannerMessage = viewModel::postBanner
                    )
                }

                DiskWalaTab.TRANSFERS -> {
                    TransfersScreen(
                        transfers = transfers,
                        isTurboVip = isTurboVip,
                        onTogglePause = viewModel::toggleTransferPause,
                        onBoostTurbo = viewModel::boostTransferTurbo,
                        onClearCompleted = { viewModel.clearCompletedTransfers(transfers) },
                        onSimulateNewTransfer = {
                            viewModel.uploadSampleCloudMedia(
                                title = "Cyber_Odyssey_2026_4K_Remaster_${(100..999).random()}.mp4",
                                category = "video",
                                sizeBytes = 1_920_000_000L,
                                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoy.mp4",
                                resolution = "4K 2160p HDR",
                                durationSec = 15L,
                                thumbnailKey = "scifi",
                                tags = listOf("4K", "TurboSync")
                            )
                        }
                    )
                }

                DiskWalaTab.VAULT -> {
                    VaultAndSpaceScreen(
                        files = files,
                        storageBreakdown = storageBreakdown,
                        config = storageConfig,
                        isVaultUnlocked = isVaultUnlocked,
                        userEmailOrName = userEmailOrName,
                        onUnlockVault = { pin -> viewModel.unlockVault(pin, storageConfig) },
                        onLockVault = viewModel::lockVault,
                        onUpdateVaultPin = viewModel::updateVaultPin,
                        onToggleVaultFile = viewModel::toggleVaultLock,
                        onPlayVaultVideo = viewModel::startVideoStream,
                        onRestoreTrashed = viewModel::restoreFromTrash,
                        onDeletePermanently = viewModel::deleteFilePermanently,
                        onToggleTurboVip = { viewModel.toggleTurboVipPlan(storageConfig) },
                        onUpdatePreferences = viewModel::updatePreferences,
                        onSignOut = {
                            signOut(
                                credentialManager = credentialManager,
                                scope = scope
                            )
                        }
                    )
                }
            }
        }
    }

    // Dialog: New Folder
    if (showNewFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        var folderTags by remember { mutableStateOf("Cloud, Shared") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create Cloud Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Folder Name") },
                        placeholder = { Text("e.g. 4K Movies & Series") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_folder_name_input")
                    )
                    OutlinedTextField(
                        value = folderTags,
                        onValueChange = { folderTags = it },
                        label = { Text("Tags (comma separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tags = folderTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        viewModel.createNewFolder(folderName, tags)
                        showNewFolderDialog = false
                    },
                    enabled = folderName.isNotBlank(),
                    modifier = Modifier.testTag("confirm_create_folder_btn")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Open Shared Link by URL/Code
    if (showOpenLinkDialog) {
        var codeInput by remember {
            mutableStateOf(
                sharedLinks.firstOrNull { it.isActive }?.let { "https://diskwala.app/s/${it.id}" } ?: ""
            )
        }
        AlertDialog(
            onDismissRequest = { showOpenLinkDialog = false },
            title = { Text("Open Shared DiskWala Link") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Paste any DiskWala link or share code to stream the video immediately or save it to your Cloud Drive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it },
                        label = { Text("DiskWala Link or Share Code") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_open_link_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOpenLinkDialog = false
                        viewModel.lookupSharedLink(codeInput)
                    },
                    enabled = codeInput.isNotBlank()
                ) {
                    Text("Open Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenLinkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Generate / Share File Link
    val targetShareFile = fileToShareDialog
    if (targetShareFile != null) {
        var allowDownload by remember { mutableStateOf(true) }
        var accessPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { fileToShareDialog = null },
            title = { Text("Share '${targetShareFile.name}'") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Generate an instant DiskWala cloud link for Telegram, WhatsApp, or direct app streaming.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Allow Save / Download", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = allowDownload,
                            onCheckedChange = { allowDownload = it }
                        )
                    }
                    OutlinedTextField(
                        value = accessPin,
                        onValueChange = { accessPin = it.take(8) },
                        label = { Text("Optional Access PIN (leave blank for public)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.generateShareLink(
                            file = targetShareFile,
                            allowDownload = allowDownload,
                            accessPin = accessPin
                        ) { generatedUrl ->
                            copyToClipboard(context, generatedUrl)
                            shareTextIntent(
                                context,
                                "Stream or download '${targetShareFile.name}' (${formatByteSize(targetShareFile.sizeBytes)}) on DiskWala: $generatedUrl"
                            )
                        }
                        fileToShareDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF031926)
                    )
                ) {
                    Text("Generate & Share Link", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToShareDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Preview Opened Shared Link
    val previewLink = openedSharedLink
    if (previewLink != null) {
        var enteredPin by remember(previewLink.id) { mutableStateOf("") }
        val isPinUnlocked = previewLink.accessPin.isBlank() || enteredPin.trim() == previewLink.accessPin

        AlertDialog(
            onDismissRequest = { viewModel.clearOpenedSharedLink() },
            title = { Text(previewLink.fileName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = ObsidianCard,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Shared by ${previewLink.ownerHandle}",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                            Text(
                                text = "${formatByteSize(previewLink.sizeBytes)} • ${previewLink.resolution.ifBlank { previewLink.category.uppercase() }}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${previewLink.viewsCount} views • ${previewLink.savesCount} cloud saves",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess
                            )
                        }
                    }

                    if (!isPinUnlocked) {
                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = { enteredPin = it },
                            label = { Text("Enter Access PIN Required by Owner") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (previewLink.category == "video" || previewLink.category == "audio") {
                        OutlinedButton(
                            onClick = { viewModel.streamFromSharedLink(previewLink) },
                            enabled = isPinUnlocked
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleFilled,
                                contentDescription = "Stream Now",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stream")
                        }
                    }
                    Button(
                        onClick = { viewModel.saveOpenedLinkToDrive(previewLink) },
                        enabled = isPinUnlocked && previewLink.allowDownload,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF031926)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Save to Drive",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save to Cloud")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearOpenedSharedLink() }) {
                    Text("Close")
                }
            }
        )
    }
}
