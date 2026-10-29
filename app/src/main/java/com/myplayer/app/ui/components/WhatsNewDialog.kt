package com.myplayer.app.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.myplayer.app.BuildConfig

data class WhatsNewFeature(
    val icon: ImageVector,
    val title: String,
    val description: String
)

object WhatsNewRepository {
    private const val PREFS_NAME = "whats_new_prefs"
    private const val KEY_LAST_SEEN_VERSION = "last_seen_version_code"

    fun shouldShow(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSeen = prefs.getInt(KEY_LAST_SEEN_VERSION, 0)
        return lastSeen < BuildConfig.VERSION_CODE
    }

    fun markSeen(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_LAST_SEEN_VERSION, BuildConfig.VERSION_CODE).apply()
    }

    fun getCurrentFeatures(): List<WhatsNewFeature> = listOf(
        WhatsNewFeature(
            icon = Icons.Filled.Dashboard,
            title = "New Miniplayer & Frosted Floating Nav",
            description = "Fully redesigned miniplayer with a frosted-glass floating navigation bar that adapts seamlessly to light and dark themes."
        ),
        WhatsNewFeature(
            icon = Icons.Filled.Album,
            title = "Custom Now Playing — 6 Styles",
            description = "Pick your vibe: Radial dial, Default, vinyl Turntable, editorial Poster, Timeline and immersive Lyrics Stage — every style fully theme-aware."
        ),
        WhatsNewFeature(
            icon = Icons.Filled.DarkMode,
            title = "Day & Night Mode",
            description = "A complete dark theme across the whole app — library, player, settings, dialogs and the audio lab all follow your choice."
        ),
        WhatsNewFeature(
            icon = Icons.Filled.SurroundSound,
            title = "FX Audio Lab — Reverb & Spatial",
            description = "New studio-grade effects chain: lush reverb with room & depth control, True Dolby-style spatial widening, and loudness compression."
        ),
        WhatsNewFeature(
            icon = Icons.Filled.GraphicEq,
            title = "15-Band Studio Equalizer",
            description = "15 ISO 2/3-octave center frequencies (25Hz - 16kHz) with real-time frequency curve canvas, haptic tactile faders, and 12 acoustic presets."
        ),
        WhatsNewFeature(
            icon = Icons.Filled.Waves,
            title = "Studio Master Clarity (C++17 DSP)",
            description = "8-stage psychoacoustic mastering chain: dynamic sub-bass foundation, low-mid decongestion, vocal presence lift, 1.22x stereo widener & analog air exciter."
        ),
        WhatsNewFeature(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            title = "ReplayGain & -14 LUFS Normalization",
            description = "Automatic Vorbis & ID3 TXXX tag loudness normalization with Dolby Atmos +6 dB compensation and true-peak protection."
        ),
        WhatsNewFeature(
            icon = Icons.Filled.Translate,
            title = "Lyrics Script Converter",
            description = "Read lyrics your way — convert Hindi (Devanagari), Hinglish (Latin) and Urdu (Arabic script) into each other, right inside the player."
        ),
        WhatsNewFeature(
            icon = Icons.Filled.CloudSync,
            title = "Spotify Integration",
            description = "Log in during onboarding or from Settings to sync your Spotify taste into the AI engine and get personal recommendations."
        ),
        WhatsNewFeature(
            icon = Icons.Filled.Backup,
            title = "Smart Backup & Restore",
            description = "Save your library, playlists and settings as a portable JSON file — restore on any device and missing songs redownload automatically."
        )
    )
}

/**
 * Modern "What's New" Changelog Dialog shown automatically after an app update.
 */
@Composable
fun WhatsNewDialog(
    onDismiss: () -> Unit,
    features: List<WhatsNewFeature> = WhatsNewRepository.getCurrentFeatures(),
    versionName: String = BuildConfig.VERSION_NAME
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(28.dp)
                ),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // ── Header: Badge & Close Button ────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "v$versionName UPDATE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "What's New in MyPlayer",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Here's what just got improved and added to your music experience.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(18.dp))

                // ── Feature Highlights List ──────────────────────────────────
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(features) { feature ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = feature.icon,
                                    contentDescription = feature.title,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = feature.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = feature.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Primary Action Button ────────────────────────────────────
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Awesome, Got It!",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }
    }
}
