package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CloudFile
import com.example.data.model.StorageBreakdown
import com.example.data.model.UserStorageConfig
import com.example.data.model.formatByteSize
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TurboGold
import com.example.ui.theme.TurboIndigo
import com.example.ui.theme.TurboViolet

@Composable
fun VaultAndSpaceScreen(
    files: List<CloudFile>,
    storageBreakdown: StorageBreakdown,
    config: UserStorageConfig?,
    isVaultUnlocked: Boolean,
    userEmailOrName: String,
    onUnlockVault: (String) -> Boolean,
    onLockVault: () -> Unit,
    onUpdateVaultPin: (String) -> Unit,
    onToggleVaultFile: (CloudFile) -> Unit,
    onPlayVaultVideo: (CloudFile) -> Unit,
    onRestoreTrashed: (CloudFile) -> Unit,
    onDeletePermanently: (CloudFile) -> Unit,
    onToggleTurboVip: () -> Unit,
    onUpdatePreferences: (String?, Boolean?, Boolean?) -> Unit,
    onSignOut: () -> Unit
) {
    val vaultFiles = remember(files) { files.filter { it.isVaultLocked && !it.isTrashed } }
    val trashedFiles = remember(files) { files.filter { it.isTrashed } }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var newPinInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("vault_and_space_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Storage Analytics Ring Card
            StorageBreakdownChartCard(storageBreakdown = storageBreakdown)
        }

        item {
            // Turbo VIP Upgrade Banner Card
            val isTurbo = config?.planTier == "turbo_vip"
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(TurboGold, ElectricCyan)),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Turbo VIP",
                            tint = TurboGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isTurbo) "DiskWala Turbo VIP Active (4 TB)" else "Upgrade to DiskWala Turbo VIP",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "4 TB Cloud Space • Original 4K HDR Video Playback • High-Speed Download Channel • Ad-Free",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onToggleTurboVip,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTurbo) ObsidianElevated else TurboGold,
                            contentColor = if (isTurbo) Color.White else Color(0xFF1F1200)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("toggle_turbo_vip_btn")
                    ) {
                        Text(
                            text = if (isTurbo) "Manage Plan (Switch to 1 TB Free Tier)" else "Activate 4 TB Turbo VIP Mode",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            // Encrypted Safe Vault Section
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Security,
                                contentDescription = "Safe Vault",
                                tint = TurboGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Encrypted Private Vault (${vaultFiles.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isVaultUnlocked) "Unlocked • Files visible below" else "PIN-protected cloud folder (Default PIN: 2580)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (isVaultUnlocked) {
                            OutlinedButton(
                                onClick = onLockVault,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Lock")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isVaultUnlocked) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = {
                                    pinInput = it.take(8)
                                    pinError = null
                                },
                                placeholder = { Text("Enter Vault PIN (e.g. 2580)") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("vault_pin_input")
                            )
                            Button(
                                onClick = {
                                    if (!onUnlockVault(pinInput)) {
                                        pinError = "Incorrect PIN. Default is 2580."
                                    } else {
                                        pinInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TurboGold,
                                    contentColor = Color(0xFF1F1200)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("unlock_vault_button")
                            ) {
                                Text("Unlock", fontWeight = FontWeight.Bold)
                            }
                        }
                        if (pinError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = pinError ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = CoralDanger
                            )
                        }
                    } else {
                        if (vaultFiles.isEmpty()) {
                            Text(
                                text = "No files in your Safe Vault yet. Use the 'Move to Encrypted Safe Vault' action on any file in Cloud Drive.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                vaultFiles.forEach { vf ->
                                    Surface(
                                        color = ObsidianElevated,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = TurboGold
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = vf.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = formatByteSize(vf.sizeBytes),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (vf.category == "video" || vf.category == "audio") {
                                                OutlinedButton(
                                                    onClick = { onPlayVaultVideo(vf) },
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("Play")
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            OutlinedButton(
                                                onClick = { onToggleVaultFile(vf) },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Unvault")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newPinInput,
                                onValueChange = { newPinInput = it.take(8) },
                                placeholder = { Text("New 4+ digit PIN") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = {
                                    onUpdateVaultPin(newPinInput)
                                    newPinInput = ""
                                },
                                enabled = newPinInput.length >= 4,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Update PIN")
                            }
                        }
                    }
                }
            }
        }

        // Cloud Playback & Sync Settings
        item {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Cloud Streaming & Sync Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Default Video Stream Quality",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "720p" to "720p Fast",
                            "1080p" to "1080p FHD",
                            "4k_original" to "4K Original"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = (config?.defaultStreamQuality ?: "1080p") == key,
                                onClick = { onUpdatePreferences(key, null, null) },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Backup Camera Roll Videos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Back up large videos over Wi-Fi so they never clog local storage",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config?.autoBackupVideos ?: true,
                            onCheckedChange = { onUpdatePreferences(null, it, null) },
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Direct Shared Link App Capture",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Open shared Telegram & web DiskWala links directly inside the app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config?.telegramLinkSync ?: true,
                            onCheckedChange = { onUpdatePreferences(null, null, it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan)
                        )
                    }
                }
            }
        }

        // Recycle Bin Section
        item {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Recycle Bin (${trashedFiles.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (trashedFiles.isEmpty()) {
                        Text(
                            text = "Recycle Bin is empty.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            trashedFiles.forEach { tf ->
                                Surface(
                                    color = ObsidianElevated,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tf.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formatByteSize(tf.sizeBytes),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { onRestoreTrashed(tf) }) {
                                            Icon(
                                                imageVector = Icons.Default.RestoreFromTrash,
                                                contentDescription = "Restore File",
                                                tint = EmeraldSuccess
                                            )
                                        }
                                        IconButton(onClick = { onDeletePermanently(tf) }) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteForever,
                                                contentDescription = "Delete Forever",
                                                tint = CoralDanger
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Account & Sign Out
        item {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Signed in as",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = userEmailOrName,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = CoralDanger,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out", color = CoralDanger)
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageBreakdownChartCard(
    storageBreakdown: StorageBreakdown
) {
    val totalUsed = storageBreakdown.totalUsedBytes.coerceAtLeast(1L).toFloat()
    val segments = listOf(
        Triple("Videos", storageBreakdown.videoBytes, ElectricCyan),
        Triple("Archives", storageBreakdown.archiveBytes, TurboIndigo),
        Triple("Audio", storageBreakdown.audioBytes, TurboViolet),
        Triple("Docs", storageBreakdown.documentBytes, TurboGold),
        Triple("Photos", storageBreakdown.photoBytes, EmeraldSuccess),
        Triple("Trash", storageBreakdown.trashedBytes, CoralDanger)
    )

    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Cloud Storage Analyzer",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(108.dp)) {
                        val strokeWidth = 14.dp.toPx()
                        drawArc(
                            color = ObsidianElevated,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                        var currentStart = -90f
                        segments.forEach { (_, bytes, color) ->
                            if (bytes > 0L) {
                                val sweep = (bytes.toFloat() / totalUsed) * 360f
                                drawArc(
                                    color = color,
                                    startAngle = currentStart,
                                    sweepAngle = sweep.coerceAtLeast(4f),
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                currentStart += sweep
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatByteSize(storageBreakdown.totalUsedBytes),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "of ${formatByteSize(storageBreakdown.quotaBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    segments.forEach { (label, bytes, color) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = formatByteSize(bytes),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
