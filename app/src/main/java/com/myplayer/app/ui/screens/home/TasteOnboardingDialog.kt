package com.myplayer.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.myplayer.app.ui.theme.*

private val AVAILABLE_LANGUAGES = listOf(
    "Hindi",
    "Punjabi",
    "Haryanvi",
    "English",
    "Tamil",
    "Telugu",
    "Bhojpuri",
    "Sufi / Urdu",
    "K-Pop"
)

private val AVAILABLE_GENRES = listOf(
    "Bollywood",
    "Punjabi Pop",
    "Desi Hip-Hop",
    "Romantic",
    "Lo-Fi Chill",
    "Sad / Heartbreak",
    "Workout / Gym",
    "Party / Club",
    "Acoustic / Sufi",
    "Phonk / EDM",
    "Pop",
    "Retro 90s"
)

private val SUGGESTED_ARTISTS = listOf(
    "Arijit Singh",
    "Karan Aujla",
    "Diljit Dosanjh",
    "Sidhu Moose Wala",
    "Atif Aslam",
    "Pritam",
    "Shreya Ghoshal",
    "Dhanda Nyoliwala",
    "Masoom Sharma",
    "AP Dhillon",
    "Seedhe Maut",
    "KR\$NA",
    "Yo Yo Honey Singh",
    "Badshah",
    "KK",
    "A.R. Rahman",
    "Vishal Mishra",
    "Anuv Jain",
    "The Weeknd",
    "Eminem",
    "Shubh",
    "Talha Anjum",
    "Nusrat Fateh Ali Khan",
    "Anirudh Ravichander"
).map { it.trim() }

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TasteOnboardingDialog(
    initialLanguages: Set<String>,
    initialGenres: Set<String>,
    initialArtists: Set<String>,
    onDismiss: () -> Unit,
    onSave: (languages: Set<String>, genres: Set<String>, artists: Set<String>) -> Unit
) {
    var languages by remember(initialLanguages) { mutableStateOf(initialLanguages.ifEmpty { setOf("Hindi", "Punjabi") }) }
    var genres by remember(initialGenres) { mutableStateOf(initialGenres.ifEmpty { setOf("Bollywood", "Romantic", "Punjabi Pop") }) }
    var artists by remember(initialArtists) { mutableStateOf(initialArtists) }
    var customArtistInput by remember { mutableStateOf("") }

    val allArtistOptions = remember(artists) {
        (SUGGESTED_ARTISTS + artists).distinct()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(28.dp),
            color = DeepOliveBackground,
            border = BorderStroke(1.dp, CardBorderOlive)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = NeonLimePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Personalize Your Music Taste",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = OnSurface
                        )
                        Text(
                            text = "Select your favorite languages, vibes & artists",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Scrollable Selection Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // 1. Languages Section
                    Text(
                        text = "1. Select Languages (${languages.size} selected)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeonLimePrimary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AVAILABLE_LANGUAGES.forEach { lang ->
                            val selected = lang in languages
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    languages = if (selected) languages - lang else languages + lang
                                },
                                label = { Text(lang, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonLimePrimary,
                                    selectedLabelColor = OnNeonLime,
                                    selectedLeadingIconColor = OnNeonLime,
                                    containerColor = SurfaceLight,
                                    labelColor = OnSurface
                                )
                            )
                        }
                    }

                    // 2. Genres Section
                    Text(
                        text = "2. Select Vibes & Genres (${genres.size} selected)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeonLimePrimary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AVAILABLE_GENRES.forEach { genre ->
                            val selected = genre in genres
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    genres = if (selected) genres - genre else genres + genre
                                },
                                label = { Text(genre, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonLimePrimary,
                                    selectedLabelColor = OnNeonLime,
                                    selectedLeadingIconColor = OnNeonLime,
                                    containerColor = SurfaceLight,
                                    labelColor = OnSurface
                                )
                            )
                        }
                    }

                    // 3. Favorite Artists Section + Custom Artist Input
                    Text(
                        text = "3. Choose Favorite Artists (${artists.size} selected)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeonLimePrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customArtistInput,
                            onValueChange = { customArtistInput = it },
                            singleLine = true,
                            placeholder = { Text("Add any artist (e.g. Pritam, Shubh)...", color = TextMuted) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonLimePrimary,
                                unfocusedBorderColor = CardBorderOlive,
                                focusedTextColor = OnSurface,
                                unfocusedTextColor = OnSurface,
                                cursorColor = NeonLimePrimary
                            )
                        )
                        FilledIconButton(
                            onClick = {
                                val trimmed = customArtistInput.trim()
                                if (trimmed.isNotBlank()) {
                                    artists = artists + trimmed
                                    customArtistInput = ""
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = NeonLimePrimary,
                                contentColor = OnNeonLime
                            )
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Artist")
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allArtistOptions.forEach { artist ->
                            val selected = artist in artists
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    artists = if (selected) artists - artist else artists + artist
                                },
                                label = { Text(artist, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonLimePrimary,
                                    selectedLabelColor = OnNeonLime,
                                    selectedLeadingIconColor = OnNeonLime,
                                    containerColor = SurfaceLight,
                                    labelColor = OnSurface
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CardBorderOlive)
                    ) {
                        Text("Later", color = OnSurfaceVariant)
                    }

                    Button(
                        onClick = { onSave(languages, genres, artists) },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonLimePrimary,
                            contentColor = OnNeonLime
                        )
                    ) {
                        Text("Save & Refresh Feed", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
