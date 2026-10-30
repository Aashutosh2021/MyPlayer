package com.myplayer.app.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.myplayer.app.ui.theme.ThemeState

data class ManualFeature(
    val id: String,
    val title: String,
    val category: String,
    val path: String,
    val icon: ImageVector,
    val summary: String,
    val instructions: List<String>,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManualDialog(
    onDismiss: () -> Unit,
    onNavigateToPlayerStyles: (() -> Unit)? = null,
    onNavigateToAudioPlayback: (() -> Unit)? = null,
    onNavigateToAudioLab: (() -> Unit)? = null,
    onNavigateToDualBud: (() -> Unit)? = null,
    onNavigateToSyncPlay: (() -> Unit)? = null,
    onConnectSpotify: (() -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Gestures", "Audio & EQ", "Player", "Social & Sharing", "Library & Offline")

    val allFeatures = remember(
        onNavigateToPlayerStyles,
        onNavigateToAudioPlayback,
        onNavigateToAudioLab,
        onNavigateToDualBud,
        onNavigateToSyncPlay,
        onConnectSpotify
    ) {
        listOf(
            ManualFeature(
                id = "miniplayer_gestures",
                title = "Miniplayer Gestures & Swipe Controls",
                category = "Gestures",
                path = "Bottom Floating Bar across Home, Search, Library & Trending",
                icon = Icons.Filled.Swipe,
                summary = "Quickly switch tracks, play/pause, or expand the player using smooth touch gestures.",
                instructions = listOf(
                    "Slide Left / Right: Swipe horizontally on the floating miniplayer bar to instantly skip to the Next or Previous track without opening the full player screen.",
                    "Tap to Expand: Tap anywhere on the miniplayer (song title/cover art) to slide up the full Now Playing screen.",
                    "Quick Play / Pause: Single-tap the dedicated circular play/pause button on the right side."
                )
            ),
            ManualFeature(
                id = "custom_player_styles",
                title = "Custom Player Styles (6 Unique Looks)",
                category = "Player",
                path = "Settings > Appearance > Customize Player Styles",
                icon = Icons.Filled.Palette,
                summary = "Pick from 6 handcrafted Now Playing styles including signature 3D Tilt Radial and Vinyl Turntable.",
                instructions = listOf(
                    "Explore 6 Styles: Radial (MyPlayer Neon + 3D Gyro Tilt), Turntable Vinyl, Poster Minimal, Timeline Waveform, Lyrics Stage, and Default Modern.",
                    "Interactive Lyrics: Tap the album art on any style to reveal synchronized lyrics.",
                    "Fast Switching: Swipe horizontally in the Customize Player screen to preview styles with live artwork."
                ),
                actionLabel = "Open Player Styles",
                onAction = onNavigateToPlayerStyles
            ),
            ManualFeature(
                id = "audio_engine_quality",
                title = "Audio Playback Engine & Hi-Res Bit-Perfect",
                category = "Audio & EQ",
                path = "Settings > Playback & Audio > Audio & Playback Engine",
                icon = Icons.Filled.HighQuality,
                summary = "Configure lossless sound quality tiers and audiophile Bit-Perfect mode.",
                instructions = listOf(
                    "7 Quality Tiers: Select between Auto, Data Saver (48kbps), Standard, High, Very High, Ultra High, and Hi-Res Bit-Perfect FLAC.",
                    "Bit-Perfect Audio: Bypasses Android's internal audio resampler to deliver exact bit-for-bit audio directly to USB DACs and high-end headphones.",
                    "Cache & Preload: Next songs preload in the background for zero-buffering playback."
                ),
                actionLabel = "Open Audio Engine",
                onAction = onNavigateToAudioPlayback
            ),
            ManualFeature(
                id = "pro_equalizer",
                title = "15-Band Pro Equalizer & AutoEQ",
                category = "Audio & EQ",
                path = "Settings > Playback & Audio > Audio & Playback Engine > Equalizer",
                icon = Icons.Filled.GraphicEq,
                summary = "Professional 15-band frequency tuning with headphone calibration profiles.",
                instructions = listOf(
                    "15-Band Tuning: Adjust individual frequency bands from 32 Hz sub-bass to 16 kHz treble with real-time curve visualization.",
                    "AutoEQ Calibration: Apply frequency response corrections calibrated for 2,500+ headphones (AirPods, Sony WH-1000XM, Sennheiser, Samsung Galaxy Buds).",
                    "Presets: Switch between Bass Boost, Vocal, Rock, Pop, Jazz, Electronic, and custom user presets."
                ),
                actionLabel = "Open Equalizer",
                onAction = onNavigateToAudioPlayback
            ),
            ManualFeature(
                id = "audio_fx_lab",
                title = "Audio Lab & DSP Sound Effects",
                category = "Audio & EQ",
                path = "Settings > Playback & Audio > Audio & Playback Engine > Audio Lab & Effects",
                icon = Icons.Filled.Tune,
                summary = "60fps real-time DSP effects including Spatial 3D, Reverb, Loudness & Pitch.",
                instructions = listOf(
                    "Spatial Audio & 3D Surround: Simulates expansive acoustic room models and crossfeed headphone listening.",
                    "Studio Reverb: Custom decay and acoustic dampening (Room, Studio, Hall, Cathedral).",
                    "Loudness Booster: Dynamic enhancement for quiet recordings without clipping.",
                    "ReplayGain & Limiter: Auto-levels volume differences across tracks.",
                    "Speed & Pitch: Change playback speed (0.5x to 2.0x) or enjoy Slowed + Reverb mode."
                ),
                actionLabel = "Open Audio Lab",
                onAction = onNavigateToAudioLab ?: onNavigateToAudioPlayback
            ),
            ManualFeature(
                id = "dual_bud_mode",
                title = "Dual Bud Mode (Two Songs, One Earphone)",
                category = "Social & Sharing",
                path = "Settings > Experimental Features > Dual Bud Mode",
                icon = Icons.Filled.Headphones,
                summary = "Split your stereo earphones so two people can listen to different songs at once.",
                instructions = listOf(
                    "Independent Channels: Left Earbud plays Track A, Right Earbud plays Track B simultaneously.",
                    "Independent Volume: Separate volume sliders and seeker controls for each person.",
                    "Separate Queues: Add, remove, and search tracks for Left and Right channels independently."
                ),
                actionLabel = "Open Dual Bud",
                onAction = onNavigateToDualBud
            ),
            ManualFeature(
                id = "sync_play",
                title = "Sync Play (Listen Together Over Wi-Fi)",
                category = "Social & Sharing",
                path = "Settings > Experimental Features > Sync Play",
                icon = Icons.Filled.GroupWork,
                summary = "Broadcast and synchronize music across multiple nearby phones without internet.",
                instructions = listOf(
                    "Host or Join: One device acts as Host while nearby phones join over local Wi-Fi or mobile hotspot.",
                    "Zero Latency: Sub-millisecond clock sync ensures all connected phones play simultaneously like a distributed sound system.",
                    "Party Controls: Host controls the queue and playback for the entire group."
                ),
                actionLabel = "Open Sync Play",
                onAction = onNavigateToSyncPlay
            ),
            ManualFeature(
                id = "spotify_sync",
                title = "Spotify Integration & AI Taste Profile",
                category = "Social & Sharing",
                path = "Settings > Spotify & AI Taste > Connect Spotify",
                icon = Icons.Filled.MusicNote,
                summary = "Import Spotify taste, playlists, and artists to train your on-device AI mix engine.",
                instructions = listOf(
                    "One-Tap Login: Connect your Spotify account securely inside the app.",
                    "AI Taste Profile: Analyzes your favorite artists and genres to generate personalized daily recommendations.",
                    "100% Private: All learning and recommendation weights run directly on your phone."
                ),
                actionLabel = "Connect Spotify",
                onAction = onConnectSpotify
            ),
            ManualFeature(
                id = "offline_downloads",
                title = "Offline Downloads with Embedded ID3 Tags",
                category = "Library & Offline",
                path = "Tap (⋮) on any song card > Download | Library > Downloads",
                icon = Icons.Filled.Download,
                summary = "Save high-bitrate audio with album artwork and complete metadata directly to storage.",
                instructions = listOf(
                    "One-Tap Download: Tap the three dots (⋮) on any track or playlist and choose 'Download'.",
                    "Full Tagging: Files are saved with high-res album cover, artist, title, and album ID3 tags.",
                    "Custom Folder: Change download destination anytime in Settings > Download Folder.",
                    "Smart Retry: Network drops are caught automatically and resumed in the background."
                )
            ),
            ManualFeature(
                id = "backup_restore",
                title = "Cloud & Local Backup / Restore",
                category = "Library & Offline",
                path = "Settings > Backup & Restore > Create Backup / Restore Backup",
                icon = Icons.Filled.Backup,
                summary = "Save and transfer your playlists, liked songs, and preferences to any device.",
                instructions = listOf(
                    "Create Backup: Exports your library, playlists, and custom settings into a lightweight JSON file.",
                    "Restore: Import the JSON backup on a new device; all playlists and songs restore instantly.",
                    "Auto-Redownload: Offline tracks in the backup will automatically re-queue for download on restore."
                )
            ),
            ManualFeature(
                id = "autoplay_recommendations",
                title = "Smart Autoplay & Queue Management",
                category = "Player",
                path = "Settings > Playback & Audio > Autoplay Recommendations | Now Playing Queue",
                icon = Icons.Filled.AutoAwesome,
                summary = "Continuous music playback tailored to your current listening vibe.",
                instructions = listOf(
                    "Endless Playback: When your current playlist ends, MyPlayer automatically queues similar songs matching tempo, artist, and mood.",
                    "Queue Reorder: Tap the Queue icon in Now Playing to drag and reorder or swipe to remove tracks."
                )
            )
        )
    }

    val filteredFeatures = remember(searchQuery, selectedCategory, allFeatures) {
        allFeatures.filter { feat ->
            val matchCategory = selectedCategory == "All" || feat.category.equals(selectedCategory, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() ||
                feat.title.contains(searchQuery, ignoreCase = true) ||
                feat.path.contains(searchQuery, ignoreCase = true) ||
                feat.summary.contains(searchQuery, ignoreCase = true) ||
                feat.instructions.any { it.contains(searchQuery, ignoreCase = true) }
            matchCategory && matchSearch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "User Manual & Guides",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Features, gestures, and settings navigation paths",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            "Search features, paths (e.g. equalizer, swipe)...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(Modifier.height(10.dp))

                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(categories) { category ->
                        val selected = category == selectedCategory
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = category },
                            label = { Text(category, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Features List
                if (filteredFeatures.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No features found matching \"$searchQuery\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredFeatures, key = { it.id }) { feat ->
                            FeatureManualCard(
                                feature = feat,
                                onDismissParent = onDismiss
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureManualCard(
    feature: ManualFeature,
    onDismissParent: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title & Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        feature.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = feature.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Navigation Path Badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "📍 Path:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = feature.path,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = feature.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(8.dp))

            // Step-by-step instructions
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                feature.instructions.forEach { instr ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = instr,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Direct Open Button (if action provided)
            if (feature.actionLabel != null && feature.onAction != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            onDismissParent()
                            feature.onAction.invoke()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = feature.actionLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

