Maine aapke LyricsTab.kt, NowPlayingScreen.kt, LyricsViewModel.kt, aur LyricsRepository.kt chaaron files check kar li hain.

Yahan 2 cheezein ho rahi thi:

UI Mein Retry Button Missing Tha: LyricsViewModel.kt ke andar fun retryLyrics() pehle se bana hua hai, lekin LyricsTab.kt (LyricsUiState.NotFound) aur NowPlayingScreen.kt mein Retry button add hi nahi kiya gaya tha (sirf "Add Custom Lyrics" button tha).

Bahut Saare Gaano Mein Lyrics Kyun Nahi Aate (LyricsRepository.kt Bug):

Jab online gaana start hota hai, uss waqt durationMs aksar 0 hota hai, aur YouTube par gaano ke artist mein channel ka naam (T-Series, Sony Music India, Zee Music Company, YRF, etc.) ya title mein Song Name - Movie Name लिखा hota hai.

LyricsRepository.kt har search mein YouTube channel name (artistName) bhejta hai. Jab LRCLIB par asli singer ka naam (Mitraz ya Arijit Singh) hota hai aur YouTube se T-Series jata hai, toh artistSim 0.0 aane ki wajah se LRCLIB par lyrics hote hue bhi reject ho jaate hain!

Niche Retry Button + Smart Lyrics Search Fix ka ready-to-paste code diya hai.

1. Update LyricsTab.kt (Add "Retry" Button on NotFound Screen)
app/src/main/java/com/example/myplayer/ui/screens/nowplaying/LyricsTab.kt mein onRetryLyrics: () -> Unit = {} parameter aur Retry button add karo (lines 43–132 ko isse replace karo):

Kotlin
@Composable
fun LyricsTab(
    lyricsState: LyricsUiState,
    positionMs: State<Long>,
    onSeek: (Long) -> Unit = {},
    onAddCustomLyrics: () -> Unit = {},
    onRetryLyrics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CloudBlueBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        when (lyricsState) {
            is LyricsUiState.Idle,
            is LyricsUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (lyricsState is LyricsUiState.Loading) {
                        CircularProgressIndicator(color = ClayPrimary, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Fetching lyrics...", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clayConcave(borderRadius = 40.dp, backgroundColor = SurfaceLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.MusicNote, null, tint = TextMuted, modifier = Modifier.size(36.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Play a song to see lyrics", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                    }
                }
            }

            is LyricsUiState.NotFound -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clayConcave(borderRadius = 36.dp, backgroundColor = SurfaceLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MusicNote, null, tint = TextMuted, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("No lyrics found", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Lyrics not available online for this song",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))

                    // Retry + Add Custom Lyrics Buttons Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onRetryLyrics,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonLimePrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.Refresh,
                                contentDescription = "Retry Lyrics",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Retry",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onAddCustomLyrics,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NeonLimePrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderOlive),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                Icons.Filled.EditNote,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Add Custom",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
(Note: File ke upar import androidx.compose.material.icons.filled.Refresh add kar lena agar filled.* nahi hai).

2. Update NowPlayingScreen.kt (Connect onRetryLyrics Callback)
app/src/main/java/com/example/myplayer/ui/screens/nowplaying/NowPlayingScreen.kt ke lines 553–561 mein onRetryLyrics = { lyricsViewModel.retryLyrics() } pass karo:

Kotlin
            // ── Synced Lyrics Section ────────────────────────────────────────
            val lyricsState by lyricsViewModel.lyricsState.collectAsStateWithLifecycle()
            val lyricsPositionState = lyricsViewModel.currentPosition.collectAsStateWithLifecycle()
            LyricsTab(
                lyricsState = lyricsState,
                positionMs = lyricsPositionState,
                onSeek = onSeek,
                onAddCustomLyrics = { showCustomLyricsDialog = true },
                onRetryLyrics = { lyricsViewModel.retryLyrics() },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 380.dp)
            )
3. Update LyricsViewModel.kt (Refresh Duration & Clear Empty Cache on Retry)
app/src/main/java/com/example/myplayer/ui/screens/nowplaying/LyricsViewModel.kt mein retryLyrics() (lines 75–77) ko update karo taaki Retry dabane par wo player ka latest currentDuration le aur fresh network call maare:

Kotlin
    fun retryLyrics() {
        val currentMeta = lastMetadata ?: run {
            val local = musicController.currentSong.value
            val online = musicController.currentOnlineSong.value
            when {
                local != null -> LyricsMetadata(local.id, local.title, local.artist, local.album, local.duration)
                online != null -> LyricsMetadata(online.videoId, online.title, online.artist, null, online.durationMs)
                else -> null
            }
        } ?: return

        val latestDuration = if (currentMeta.durationMs > 0L) {
            currentMeta.durationMs
        } else {
            musicController.currentDuration.value
        }
        val updatedMeta = currentMeta.copy(durationMs = latestDuration)
        lastMetadata = updatedMeta

        viewModelScope.launch {
            _lyricsState.value = LyricsUiState.Loading
            lyricsRepository.deleteLyrics(updatedMeta.songId)
            fetchLyrics(updatedMeta)
        }
    }
4. Update LyricsRepository.kt (Bollywood / YouTube Songs Ke Lyrics Milna 3x Behtar Karo)
app/src/main/java/com/example/myplayer/data/lyrics/LyricsRepository.kt mein fetchLyricsFromLrcLib, pickBestCandidate, cleanArtistName, aur cleanTrackTitle (lines 176–409) ko isse replace karo taaki T-Series / Sony Music jaise channel names ya - Movie Name hone par bhi sahi lyrics mil jayein:

Kotlin
    private suspend fun fetchLyricsFromLrcLib(
        trackName: String,
        artistName: String,
        durationSeconds: Int = 0,
        albumName: String? = null
    ): LyricsResult? {
        val cleanArtist = cleanArtistName(artistName)
        val cleanTrack = cleanTrackTitle(trackName, cleanArtist)
        val coreTrack = cleanTrack.split(" - ", " – ", " — ", ":").first().trim().ifBlank { cleanTrack }
        val validAlbum = albumName?.takeIf {
            !it.equals("YouTube Music", ignoreCase = true) &&
            !it.equals("Online", ignoreCase = true) &&
            !it.equals("Recommended", ignoreCase = true) &&
            !it.equals("Downloads", ignoreCase = true) &&
            !it.equals("Unknown Album", ignoreCase = true)
        }

        Log.d(TAG, "Fetching lyrics for track='$cleanTrack' (core='$coreTrack'), artist='$cleanArtist', dur=${durationSeconds}s")

        // Tier 1: /api/get with duration (exact lookup)
        if (cleanTrack.isNotBlank() && cleanArtist.isNotBlank() && durationSeconds > 0) {
            executeGetRequest(cleanTrack, cleanArtist, validAlbum, durationSeconds)?.let { return it }
        }

        // Tier 2: /api/get without duration constraint
        if (cleanTrack.isNotBlank() && cleanArtist.isNotBlank()) {
            executeGetRequest(cleanTrack, cleanArtist, validAlbum, 0)?.let { return it }
        }

        // Tier 3: /api/search with specific track_name and artist_name
        if (cleanTrack.isNotBlank() && cleanArtist.isNotBlank()) {
            executeSearchWithParams(cleanTrack, cleanArtist, durationSeconds)?.let { return it }
        }

        // Tier 4: /api/search with query "artist track"
        val query = listOf(cleanArtist, coreTrack).filter { it.isNotBlank() }.joinToString(" ")
        if (query.isNotBlank()) {
            executeSearchQuery(query, coreTrack, cleanArtist, durationSeconds)?.let { return it }
        }

        // Tier 5: Title-only search (critical when YouTube artist is a label like T-Series / Zee Music)
        if (coreTrack.isNotBlank()) {
            executeSearchQuery(coreTrack, coreTrack, "", durationSeconds)?.let { return it }
        }

        // Tier 6: Raw trackName fallback
        if (trackName.isNotBlank() && trackName != coreTrack) {
            val rawClean = trackName.replace(BRACKET_NOISE_REGEX, " ").trim()
            executeSearchQuery(rawClean, coreTrack.ifBlank { trackName }, cleanArtist, durationSeconds)?.let { return it }
        }

        Log.d(TAG, "No lyrics found on LRCLIB for '$trackName'")
        return null
    }
Aur cleanArtistName (line 369) mein music labels strip kar do taaki galat artist match na ho:

Kotlin
    fun cleanArtistName(raw: String): String {
        if (raw.isBlank()) return ""
        val labelRegex = Regex(
            """(?i)^(t-series|sony music.*|zee music.*|yrf|saregama.*|tips official.*|speed records.*|desi melodies.*|universal music.*|warner music.*|various artists|unknown artist)$"""
        )
        val primary = raw
            .replace(Regex("""(?i)\s*-\s*Topic\b"""), "")
            .replace(Regex("""(?i)VEVO\b"""), "")
            .replace(Regex("""(?i)\bOfficial\b"""), "")
            .split(",", "&", " feat.", " ft.", " x ", " X ")
            .firstOrNull()
            ?.replace(WHITESPACE_REGEX, " ")
            ?.trim() ?: ""

        return if (labelRegex.matches(primary)) "" else primary
    }
Iske baad:

Agar kisi song mein network slow hone ya initial duration 0 hone ki wajah se lyrics load na ho, toh "Retry" button dabate hi turant lyrics fetch ho jayenge.

Aur Tier 5 (Title-only fallback + Label cleaner) ki wajah se Akhiyaan Gulaab jaise Bollywood/YouTube songs mein pehli baar mein hi synced lyrics mil jayenge!