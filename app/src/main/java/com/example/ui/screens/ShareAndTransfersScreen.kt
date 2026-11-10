package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.SharedLink
import com.example.data.model.TransferTask
import com.example.data.model.formatByteSize
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TurboGold
import com.example.ui.theme.TurboIndigo

@Composable
fun ShareLinksScreen(
    sharedLinks: List<SharedLink>,
    onLookupLink: (String) -> Unit,
    onUpdateLinkPermissions: (SharedLink, Boolean?, String?, Boolean?) -> Unit,
    onBannerMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var linkInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("share_links_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Instant Link Extractor Card
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(ElectricCyan.copy(alpha = 0.5f), TurboIndigo.copy(alpha = 0.4f))),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Instant Link Extractor",
                            tint = ElectricCyan,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Open Shared DiskWala Link",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Paste a Telegram/DiskWala link or share code to stream or save to your cloud",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = linkInput,
                        onValueChange = { linkInput = it },
                        placeholder = { Text("https://diskwala.app/s/dw_... or code") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_link_input")
                    )

                    if (sharedLinks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Try active link:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            sharedLinks.filter { it.isActive }.take(3).forEach { link ->
                                AssistChip(
                                    onClick = {
                                        linkInput = "https://diskwala.app/s/${link.id}"
                                        onLookupLink(link.id)
                                    },
                                    label = {
                                        Text(
                                            text = link.id,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ElectricCyan
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onLookupLink(linkInput) },
                        enabled = linkInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF031926)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lookup_link_button")
                    ) {
                        Text(
                            text = "Open & Preview Shared Link",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "My Shared Links (${sharedLinks.size})",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        if (sharedLinks.isEmpty()) {
            item {
                Surface(
                    color = ObsidianCard.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "No shared links",
                            tint = ElectricCyan,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Shared Links Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Tap the Share icon on any file in your Cloud Drive to generate an instant shareable link.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(sharedLinks, key = { it.id }) { link ->
                SharedLinkCard(
                    link = link,
                    onCopyLink = {
                        val url = "https://diskwala.app/s/${link.id}"
                        copyToClipboard(context, url)
                        onBannerMessage("Copied share link: $url")
                    },
                    onShareExternal = {
                        val url = "https://diskwala.app/s/${link.id}"
                        shareTextIntent(
                            context = context,
                            text = "Watch or save '${link.fileName}' (${formatByteSize(link.sizeBytes)}) on DiskWala Cloud: $url" +
                                if (link.accessPin.isNotBlank()) " [PIN: ${link.accessPin}]" else ""
                        )
                    },
                    onPreviewLink = { onLookupLink(link.id) },
                    onToggleAllowDownload = { allow ->
                        onUpdateLinkPermissions(link, allow, null, null)
                    },
                    onToggleActive = { active ->
                        onUpdateLinkPermissions(link, null, null, active)
                    }
                )
            }
        }
    }
}

@Composable
private fun SharedLinkCard(
    link: SharedLink,
    onCopyLink: () -> Unit,
    onShareExternal: () -> Unit,
    onPreviewLink: () -> Unit,
    onToggleAllowDownload: (Boolean) -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPreviewLink)
            .testTag("shared_link_card_${link.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = link.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "https://diskwala.app/s/${link.id}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                }

                Surface(
                    color = if (link.isActive) EmeraldSuccess.copy(alpha = 0.2f) else CoralDanger.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (link.isActive) "ACTIVE" else "REVOKED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (link.isActive) EmeraldSuccess else CoralDanger,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Analytics Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Views",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${link.viewsCount} views",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Saves",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${link.savesCount} cloud saves",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (link.accessPin.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "PIN protected",
                            tint = TurboGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PIN: ${link.accessPin}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TurboGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Permission Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Allow Recipients to Save / Download",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Switch(
                    checked = link.allowDownload,
                    onCheckedChange = onToggleAllowDownload,
                    colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyLink,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Link",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Link")
                }

                Button(
                    onClick = onShareExternal,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF031926)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Send Link",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }

                OutlinedButton(
                    onClick = { onToggleActive(!link.isActive) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (link.isActive) "Revoke" else "Enable",
                        color = if (link.isActive) CoralDanger else EmeraldSuccess
                    )
                }
            }
        }
    }
}

@Composable
fun TransfersScreen(
    transfers: List<TransferTask>,
    isTurboVip: Boolean,
    onTogglePause: (TransferTask) -> Unit,
    onBoostTurbo: (TransferTask) -> Unit,
    onClearCompleted: () -> Unit,
    onSimulateNewTransfer: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("transfers_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(TurboGold.copy(alpha = 0.5f), ElectricCyan.copy(alpha = 0.4f))),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Turbo Channel",
                                tint = TurboGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Turbo High-Speed Transfer Channel",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isTurboVip) "Unlocked: Up to 85 MB/s dedicated cloud fiber" else "Parallel multi-thread cloud acceleration ready",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onSimulateNewTransfer,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color(0xFF031926)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("queue_sample_transfer_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Sync Sample",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync 4K Demo Pack")
                        }

                        if (transfers.any { it.status == "completed" }) {
                            OutlinedButton(
                                onClick = onClearCompleted,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear Completed",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Done")
                            }
                        }
                    }
                }
            }
        }

        if (transfers.isEmpty()) {
            item {
                Surface(
                    color = ObsidianCard.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "No transfers",
                            tint = ElectricCyan,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Transfer Queue is Clean",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Uploads, offline downloads, and saved shared links will appear here with real-time speed telemetry.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(transfers, key = { it.id }) { task ->
                TransferTaskCard(
                    task = task,
                    onTogglePause = { onTogglePause(task) },
                    onBoostTurbo = { onBoostTurbo(task) }
                )
            }
        }
    }
}

@Composable
private fun TransferTaskCard(
    task: TransferTask,
    onTogglePause: () -> Unit,
    onBoostTurbo: () -> Unit
) {
    val progress = if (task.totalBytes > 0L) {
        (task.transferredBytes.toFloat() / task.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }
    val dirIcon = when (task.direction) {
        "upload" -> Icons.Default.CloudUpload
        "download" -> Icons.Default.CloudDownload
        else -> Icons.Default.CloudSync
    }
    val speedMbps = String.format(java.util.Locale.US, "%.1f MB/s", task.speedKbps / 1024.0)

    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = dirIcon,
                        contentDescription = task.direction,
                        tint = ElectricCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatByteSize(task.transferredBytes)} / ${formatByteSize(task.totalBytes)} • ${if (task.status == "completed") "Completed" else speedMbps}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (task.isTurboBoosted) TurboGold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (task.status == "completed") {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    IconButton(onClick = onTogglePause) {
                        Icon(
                            imageVector = if (task.status == "paused") Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause or Resume",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onBoostTurbo) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Boost with Turbo",
                            tint = TurboGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                color = if (task.status == "completed") EmeraldSuccess else ElectricCyan,
                trackColor = ObsidianElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("DiskWala Share Link", text))
}

fun shareTextIntent(context: Context, text: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(sendIntent, "Share DiskWala Link")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(chooser)
    } catch (_: Exception) {
    }
}
