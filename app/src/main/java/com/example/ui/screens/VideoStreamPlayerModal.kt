package com.example.ui.screens

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.model.CloudFile
import com.example.data.model.formatByteSize
import com.example.data.model.formatDuration
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TurboGold
import com.example.ui.theme.TurboIndigo
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun VideoStreamHubScreen(
    files: List<CloudFile>,
    isTurboVip: Boolean,
    onPlayVideo: (CloudFile) -> Unit,
    onShareVideo: (CloudFile) -> Unit,
    onToggleOffline: (CloudFile) -> Unit,
    onOpenSharedLinkDialog: () -> Unit
) {
    val streamableFiles = remember(files) {
        files.filter { !it.isTrashed && !it.isVaultLocked && (it.category == "video" || it.category == "audio") }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("video_stream_hub_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Direct Link Stream Header Card
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = "Cloud Cinema",
                                tint = ElectricCyan,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Direct Cloud Video Streamer",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isTurboVip) "Turbo VIP • 4K HDR Original Bitrate Active" else "Zero-wait cloud streaming & instant seeking",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isTurboVip) TurboGold else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onOpenSharedLinkDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan.copy(alpha = 0.16f),
                            contentColor = ElectricCyan
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stream_shared_link_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Stream Link",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Paste DiskWala Link to Stream Instantly")
                    }
                }
            }
        }

        if (streamableFiles.isEmpty()) {
            item {
                Surface(
                    color = ObsidianCard.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "No videos",
                            tint = ElectricCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Cloud Videos or Audio Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Upload videos from your device or open a shared DiskWala link to stream immediately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(streamableFiles, key = { it.id }) { video ->
                VideoStreamCard(
                    file = video,
                    onPlay = { onPlayVideo(video) },
                    onShare = { onShareVideo(video) },
                    onToggleOffline = { onToggleOffline(video) }
                )
            }
        }
    }
}

@Composable
private fun VideoStreamCard(
    file: CloudFile,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onToggleOffline: () -> Unit
) {
    val posterRes = when (file.thumbnailKey) {
        "nature" -> R.drawable.img_video_poster_nature_1794309895061
        "scifi" -> R.drawable.img_video_poster_scifi_1794309894433
        else -> R.drawable.img_cloud_hero_1794309894764
    }
    val totalDur = file.durationSec.coerceAtLeast(15L)
    val progress = (file.playbackPositionSec.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f)

    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .testTag("video_stream_card_${file.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp)
            ) {
                Image(
                    painter = painterResource(id = posterRes),
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.2f),
                                    Color.Black.copy(alpha = 0.78f)
                                )
                            )
                        )
                )

                // Top badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = file.resolution.ifBlank { "1080p CLOUD" },
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (file.isOfflineAvailable) {
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "OFFLINE READY",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Center Play Button
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Stream",
                        tint = Color(0xFF031926),
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Bottom duration badge
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (file.playbackPositionSec > 0L) {
                                "${formatDuration(file.playbackPositionSec)} / ${formatDuration(totalDur)}"
                            } else {
                                formatDuration(totalDur)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = ElectricCyan,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${formatByteSize(file.sizeBytes)} • ${file.folderPath}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleOffline) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Toggle Offline Cache",
                        tint = if (file.isOfflineAvailable) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Stream Link",
                        tint = ElectricCyan
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoStreamPlayerModal(
    file: CloudFile,
    isTurboVip: Boolean,
    defaultQuality: String,
    onSavePosition: (Long) -> Unit,
    onShare: (CloudFile) -> Unit,
    onToggleOffline: (CloudFile) -> Unit,
    onClose: () -> Unit
) {
    BackHandler {
        onClose()
    }

    val totalDurationSec = file.durationSec.coerceAtLeast(30L)
    var currentSec by remember(file.id) { mutableLongStateOf(file.playbackPositionSec.coerceIn(0L, totalDurationSec)) }
    var isPlaying by remember { mutableStateOf(true) }
    var selectedQuality by remember {
        mutableStateOf(
            when (defaultQuality) {
                "4k_original" -> "4K Original"
                "720p" -> "720p Fast"
                else -> "1080p FHD"
            }
        )
    }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var captionsEnabled by remember { mutableStateOf(true) }
    var useNativeVideoView by remember { mutableStateOf(file.streamUrl.startsWith("http") || file.streamUrl.startsWith("content://")) }
    var wavePhase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying, playbackSpeed, totalDurationSec) {
        while (isPlaying) {
            delay(250L)
            wavePhase += 0.35f
            if (currentSec < totalDurationSec) {
                // Advance every ~1 sec scaled by playbackSpeed
                if ((wavePhase * 10).toInt() % 4 == 0) {
                    currentSec = (currentSec + 1L).coerceAtMost(totalDurationSec)
                }
            } else {
                isPlaying = false
            }
        }
    }

    val posterRes = when (file.thumbnailKey) {
        "nature" -> R.drawable.img_video_poster_nature_1794309895061
        else -> R.drawable.img_video_poster_scifi_1794309894433
    }

    Surface(
        color = ObsidianBg,
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("video_stream_player_modal")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        onSavePosition(currentSec)
                        onClose()
                    },
                    modifier = Modifier.testTag("close_player_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Cloud Drive",
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Cloud Direct Stream • $selectedQuality • ${playbackSpeed}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }
                IconButton(onClick = { onShare(file) }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Link",
                        tint = ElectricCyan
                    )
                }
            }

            // 16:9 Video Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                Image(
                    painter = painterResource(id = posterRes),
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (useNativeVideoView && file.streamUrl.isNotBlank()) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setOnErrorListener { _, _, _ ->
                                    useNativeVideoView = false
                                    true
                                }
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    if (currentSec > 0) {
                                        seekTo((currentSec * 1000).toInt())
                                    }
                                    if (isPlaying) start()
                                }
                                try {
                                    setVideoURI(Uri.parse(file.streamUrl))
                                } catch (e: Exception) {
                                    useNativeVideoView = false
                                }
                            }
                        },
                        update = { view ->
                            try {
                                if (isPlaying && !view.isPlaying) {
                                    view.start()
                                } else if (!isPlaying && view.isPlaying) {
                                    view.pause()
                                }
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Subtle Cinema HUD Overlay + Live Audio/Bitrate Waveform
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.72f)
                                )
                            )
                        )
                )

                // Top-Right Bitrate & Buffer Telemetry Pill
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Stream Bitrate",
                            tint = ElectricCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedQuality.startsWith("4K")) "28.4 Mbps • Zero Buffer" else "12.1 Mbps • Cloud CDN",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                // Animated Waveform Visualizer at Bottom of Video Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    val barCount = 42
                    val barWidth = size.width / (barCount * 1.6f)
                    val gap = (size.width - barWidth * barCount) / (barCount - 1).coerceAtLeast(1)
                    for (i in 0 until barCount) {
                        val ratio = i.toFloat() / barCount.toFloat()
                        val played = ratio <= (currentSec.toFloat() / totalDurationSec.toFloat())
                        val amp = if (isPlaying) {
                            (0.25f + 0.75f * kotlin.math.abs(sin(wavePhase + i * 0.45f))).coerceIn(0.15f, 1f)
                        } else {
                            0.25f
                        }
                        val barH = size.height * amp
                        val x = i * (barWidth + gap)
                        drawLine(
                            color = if (played) ElectricCyan else Color.White.copy(alpha = 0.3f),
                            start = Offset(x, size.height),
                            end = Offset(x, size.height - barH),
                            strokeWidth = barWidth
                        )
                    }
                }

                // Captions overlay
                if (captionsEnabled) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 40.dp)
                    ) {
                        Text(
                            text = "[CC] Streaming '${file.name}' directly from DiskWala Cloud Drive (${formatDuration(currentSec)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Scrubber & Transport Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Slider(
                    value = currentSec.toFloat(),
                    onValueChange = {
                        currentSec = it.toLong()
                        onSavePosition(currentSec)
                    },
                    valueRange = 0f..totalDurationSec.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricCyan,
                        activeTrackColor = ElectricCyan,
                        inactiveTrackColor = ObsidianCard
                    ),
                    modifier = Modifier.testTag("video_seek_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentSec),
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                    Text(
                        text = formatDuration(totalDurationSec),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Transport buttons (-10s, Play/Pause, +10s, CC)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            currentSec = (currentSec - 10L).coerceAtLeast(0L)
                            onSavePosition(currentSec)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Surface(
                        color = ElectricCyan,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(64.dp)
                            .clickable {
                                isPlaying = !isPlaying
                                onSavePosition(currentSec)
                            }
                            .testTag("play_pause_toggle_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color(0xFF031926),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            currentSec = (currentSec + 10L).coerceAtMost(totalDurationSec)
                            onSavePosition(currentSec)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(onClick = { captionsEnabled = !captionsEnabled }) {
                        Icon(
                            imageVector = Icons.Default.ClosedCaption,
                            contentDescription = "Toggle Captions",
                            tint = if (captionsEnabled) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Quality Selector
                Text(
                    text = "Streaming Quality",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("720p Fast", "1080p FHD", "4K Original").forEach { q ->
                        FilterChip(
                            selected = selectedQuality == q,
                            onClick = { selectedQuality = q },
                            label = {
                                Text(
                                    text = if (q == "4K Original") "$q ⚡" else q,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = ElectricCyan
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Playback Speed Selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Playback Speed",
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Playback Speed",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                        FilterChip(
                            selected = playbackSpeed == spd,
                            onClick = { playbackSpeed = spd },
                            label = { Text("${spd}x", style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TurboIndigo.copy(alpha = 0.28f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Quick actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onToggleOffline(file) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Save Offline",
                            tint = if (file.isOfflineAvailable) EmeraldSuccess else ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (file.isOfflineAvailable) "Cached Offline" else "Offline Download")
                    }
                    Button(
                        onClick = { onShare(file) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF031926)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Link",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Stream Link")
                    }
                }
            }
        }
    }
}
