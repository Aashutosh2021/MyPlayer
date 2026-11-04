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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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
            icon = Icons.Filled.Link,
            title = "Search via YouTube Link",
            description = "Paste any YouTube or YouTube Music link directly into the search bar to instantly stream, match, or download that exact song."
        ),
        WhatsNewFeature(
            icon = Icons.Filled.Headphones,
            title = "DualBud — Dual Audio Player",
            description = "Play two completely separate tracks through Left and Right earbuds simultaneously with dedicated volumes and controls."
        ),
        WhatsNewFeature(
            icon = Icons.Filled.Group,
            title = "SyncPlay — Listen Together",
            description = "Host or join synchronized listening rooms to experience music in real-time with friends across multiple devices."
        ),
        WhatsNewFeature(
            icon = Icons.Filled.Waves,
            title = "Studio Master DSP & Audio Lab",
            description = "10-band ISO equalizer, Studio Master default clarity preset, dynamic sub-bass, 1.22x stereo widener, and analog air exciter."
        ),
        WhatsNewFeature(
            icon = Icons.Filled.AutoAwesome,
            title = "Apple-Grade AutoMix & Crossfade",
            description = "Seamless DJ-style crossfade transitions between songs with equal-power volume curves and automatic silence removal."
        ),
        WhatsNewFeature(
            icon = Icons.Filled.Album,
            title = "Custom Now Playing (6 Styles)",
            description = "Personalize your vibe: Radial Dial, Turntable Vinyl, Editorial Poster, Timeline, Lyrics Stage, or Classic style."
        ),
        WhatsNewFeature(
            icon = Icons.Filled.DarkMode,
            title = "Dark Mode by Default",
            description = "Refined true-black aesthetics on first launch, theme-adaptive floating navigation bar, and fix for tab redirects."
        ),
        WhatsNewFeature(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = "Interactive Tour & User Manual",
            description = "Step-by-step guided walkthrough of every power feature plus an in-app User Manual accessible anytime from Settings."
        ),
        WhatsNewFeature(
            icon = Icons.Filled.LocalCafe,
            title = "Support Development (UPI / Coffee)",
            description = "MyPlayer is 100% free and open with zero ads. You can now support ongoing development directly via GPay, PhonePe, or UPI."
        ),
        WhatsNewFeature(
            icon = Icons.Filled.Backup,
            title = "Smart Backup & Restore",
            description = "Export and import your entire library, custom playlists, and audio preferences with one tap across any device."
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
