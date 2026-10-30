package com.myplayer.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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
    "Anirudh Ravichander",
    "Neha Kakkar",
    "Jubin Nautiyal",
    "Darshan Raval",
    "B Praak",
    "Armaan Malik",
    "Sonu Nigam",
    "Kishore Kumar",
    "Lata Mangeshkar",
    "Mohammed Rafi",
    "Ed Sheeran",
    "Taylor Swift",
    "Drake",
    "Imagine Dragons",
    "BTS"
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
    val pickerViewModel: ArtistPickerViewModel = hiltViewModel()
    val searchState by pickerViewModel.results.collectAsStateWithLifecycle()
    val photos by pickerViewModel.photos.collectAsStateWithLifecycle()

    val allArtistOptions = remember(artists) {
        (SUGGESTED_ARTISTS + artists).distinct()
    }

    LaunchedEffect(allArtistOptions) { pickerViewModel.loadPhotos(allArtistOptions) }

    // Add a searched artist (name is Deezer's real artist name); offline we can only take the typed text.
    fun addArtist(name: String) {
        artists = artists + name
        customArtistInput = ""
        pickerViewModel.clearSearch()
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

                    // 3. Favorite Artists: search the internet for real artists, pick from photos
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
                            onValueChange = {
                                customArtistInput = it
                                pickerViewModel.onQueryChange(it)
                            },
                            singleLine = true,
                            placeholder = { Text("Search any artist (e.g. Pritam, Shubh)...", color = TextMuted) },
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
                                val typed = customArtistInput.trim()
                                when (val st = searchState) {
                                    is ArtistSearchState.Found ->
                                        addArtist((st.artists.firstOrNull { it.name.equals(typed, true) } ?: st.artists.first()).name)
                                    ArtistSearchState.Offline -> if (typed.isNotBlank()) addArtist(typed)
                                    else -> Unit // not found / still searching: only real artists are added
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

                    when (val st = searchState) {
                        ArtistSearchState.Idle -> Unit
                        ArtistSearchState.Loading -> CircularProgressIndicator(
                            color = NeonLimePrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp)
                        )
                        ArtistSearchState.NotFound -> Text(
                            "No artist found for \"${customArtistInput.trim()}\". Check the spelling.",
                            style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                        )
                        ArtistSearchState.Offline -> Text(
                            "Can't reach the internet to verify. Tap + to add the name as typed.",
                            style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                        )
                        is ArtistSearchState.Found -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            st.artists.forEach { found ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { addArtist(found.name) }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    ArtistPhoto(found.name, found.imageUrl, size = 44.dp)
                                    Text(
                                        found.name,
                                        modifier = Modifier.weight(1f),
                                        color = OnSurface,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (found.name in artists) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = NeonLimePrimary, modifier = Modifier.size(18.dp))
                                    } else {
                                        Icon(Icons.Filled.Add, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        allArtistOptions.forEach { artist ->
                            val selected = artist in artists
                            Column(
                                modifier = Modifier
                                    .width(84.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { artists = if (selected) artists - artist else artists + artist }
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    ArtistPhoto(
                                        name = artist,
                                        imageUrl = photos[artist],
                                        size = 68.dp,
                                        ring = if (selected) NeonLimePrimary else null
                                    )
                                    if (selected) {
                                        Box(
                                            modifier = Modifier.size(22.dp).clip(CircleShape).background(NeonLimePrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = OnNeonLime, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = artist,
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) NeonLimePrimary else OnSurface
                                )
                            }
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

/** Round artist photo; the first letter is shown until (or if) no picture is available. */
@Composable
private fun ArtistPhoto(name: String, imageUrl: String?, size: androidx.compose.ui.unit.Dp, ring: Color? = null) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(SurfaceLight)
            .let { if (ring != null) it.border(3.dp, ring, CircleShape) else it },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "?",
            color = OnSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value / 2.6f).sp
        )
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}
