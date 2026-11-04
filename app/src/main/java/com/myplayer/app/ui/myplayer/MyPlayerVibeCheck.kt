package com.myplayer.app.ui.myplayer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

// Funny desi mood / vibe-check popup: tracks a same-vibe listening streak and shows a dialog after N minutes.
enum class MyPlayerVibeCategory(
    val label: String,
    val emoji: String,
    val badgeText: String,
    val accentColor: Color,
    val secondaryColor: Color,
    val primaryButtonText: String,
    val switchButtonText: String
) {
    SAD_HEARTBREAK(
        label = "Sad / Heartbreak",
        emoji = "😭💔",
        badgeText = "AASHIQUI MODE DETECTED",
        accentColor = Color(0xFF7E57C2),
        secondaryColor = Color(0xFF3949AB),
        primaryButtonText = "Sab Theek Hai Bhai 😎",
        switchButtonText = "Mood Change Karo! 🔥"
    ),
    PARTY_BANGER(
        label = "Party / Dance",
        emoji = "🕺🎉",
        badgeText = "FULL MAHAUL MODE",
        accentColor = Color(0xFFFF4081),
        secondaryColor = Color(0xFFFF9100),
        primaryButtonText = "Full Party Chal Rahi Hai! 🥳",
        switchButtonText = "Agla Banger Bajao ⚡"
    ),
    ROMANTIC_LOVE(
        label = "Romantic / Love",
        emoji = "😍🌹",
        badgeText = "PYAAR MOHABBAT ALERT",
        accentColor = Color(0xFFE91E63),
        secondaryColor = Color(0xFFAD1457),
        primaryButtonText = "Nazar Mat Laga Bhai 🧿😂",
        switchButtonText = "Next Song ▶"
    ),
    WORKOUT_HUSTLE(
        label = "Gym / Beast Mode",
        emoji = "💪🔥",
        badgeText = "BEAST & HUSTLE MODE",
        accentColor = Color(0xFF38E54D),
        secondaryColor = Color(0xFF00BFA5),
        primaryButtonText = "Aaj Toofani Karenge! 💪",
        switchButtonText = "Next Banger 🔥"
    ),
    DEVOTIONAL_PEACE(
        label = "Bhakti / Sukoon",
        emoji = "🙏✨",
        badgeText = "SUKOON & BHAKTI VIBE",
        accentColor = Color(0xFFFFB300),
        secondaryColor = Color(0xFFF57C00),
        primaryButtonText = "Dil Ko Sukoon Mila 🙏",
        switchButtonText = "Continue Listening ✨"
    ),
    CHILL_LOFI(
        label = "Chill / Late Night",
        emoji = "🌙🎧",
        badgeText = "CHILL & OVERTHINKING ZONE",
        accentColor = Color(0xFF26C6DA),
        secondaryColor = Color(0xFF5C6BC0),
        primaryButtonText = "Bas Vibe Le Raha Hu 😌",
        switchButtonText = "Kuch Energetic Bajao ⚡"
    )
}

data class MyPlayerVibePopupEvent(
    val category: MyPlayerVibeCategory,
    val elapsedMinutes: Int,
    val headline: String,
    val funnyMessage: String,
    val currentSongTitle: String
)

object MyPlayerVibeCheckManager {
    var enabled by mutableStateOf(true)
    var thresholdMinutes by mutableIntStateOf(15)
    var activePopup by mutableStateOf<MyPlayerVibePopupEvent?>(null)

    private var currentStreakCategory: MyPlayerVibeCategory? = null
    private var accumulatedListeningMs: Long = 0L
    private var lastTickRealtimeMs: Long = 0L
    private var msgVariantIdx: Int = 0

    private val sadKeywords = listOf(
        "sad", "bewafa", "judaai", "judaiyaan", "adhura", "tanha", "rona", "yaad", "yaadein",
        "dard", "toot", "channa mereya", "agar tum saath", "tum hi ho", "bedardi", "bekhayali",
        "kabira", "bhula diya", "adhuri kahani", "bairaagi", "alvida", "khairiyat", "mann bharrya",
        "qismat", "pachtaoge", "filhall", "zaroori tha", "heartbreak", "lonely", "broken", "cry",
        "miss you", "hurt", "tears", "hamdard", "phir bhi tumko", "ae dil hai mushkil",
        "tadap", "bikhra", "shikayat", "husn", "khat", "jo tum mere ho", "choo lo", "afsos"
    )

    private val partyKeywords = listOf(
        "party", "dance", "dj", "remix", "club", "nachde", "bhangra", "kala chashma", "chull",
        "badshah", "honey singh", "diljit", "karan aujla", "ap dhillon", "brown munde", "tauba",
        "zingaat", "ghungroo", "baby doll", "bom diggy", "saturday", "gallan goodiyan", "thumakda",
        "lalkara", "goat", "hass hass", "softly", "hookah", "balma", "sharara", "naatu",
        "besharam", "jhoome", "chaleya", "illuminati", "aayi nai", "stree", "banger", "disco"
    )

    private val romanticKeywords = listOf(
        "ishq", "pyaar", "pyar", "mohabbat", "kesariya", "raataan lambiyan", "tum se hi",
        "apna bana le", "satranga", "heeriye", "pehle bhi main", "tere hawale", "mast magan",
        "shayad", "hawayein", "zaalima", "dil diyan gallan", "love", "romance", "jaan", "sanam",
        "mahiye", "tu hai to", "ve kamleya", "o maahi", "sajni", "akhiyaan gulab", "tera ban jaunga"
    )

    private val workoutKeywords = listOf(
        "sidhu moose", "moose wala", "kr\$na", "krsna", "seedhe maut", "divine", "raftaar",
        "emiway", "workout", "gym", "hustle", "beast", "phonk", "believer", "unstoppable",
        "lose yourself", "sultan", "zinda", "chak de", "kar har maidan", "aarambh", "dangal",
        "millionaire", "hanumankind", "big dawgs", "295", "levels", "warrior", "power"
    )

    private val devotionalKeywords = listOf(
        "bhajan", "aarti", "chalisa", "hanuman", "shiv", "mahadev", "krishna", "ram", "radhe",
        "kun faya", "arziyan", "bhar do jholi", "sufi", "qawwali", "namo namo", "ganesha", "bholenath"
    )

    fun classifySongVibe(title: String, artist: String, album: String): MyPlayerVibeCategory {
        val combined = "$title $artist $album".lowercase(Locale.ROOT)
        if (MyPlayerAudioFxDspBridge.reverbEnabled && !MyPlayerAudioFxDspBridge.spatialIsSpeakersMode &&
            sadKeywords.any { combined.contains(it) }
        ) {
            return MyPlayerVibeCategory.SAD_HEARTBREAK
        }

        return when {
            devotionalKeywords.any { combined.contains(it) } -> MyPlayerVibeCategory.DEVOTIONAL_PEACE
            sadKeywords.any { combined.contains(it) } -> MyPlayerVibeCategory.SAD_HEARTBREAK
            partyKeywords.any { combined.contains(it) } -> MyPlayerVibeCategory.PARTY_BANGER
            workoutKeywords.any { combined.contains(it) } -> MyPlayerVibeCategory.WORKOUT_HUSTLE
            romanticKeywords.any { combined.contains(it) } -> MyPlayerVibeCategory.ROMANTIC_LOVE
            combined.contains("arijit") || combined.contains("atif") || combined.contains("kk") ||
                    combined.contains("jubin") || combined.contains("vishal mishra") ||
                    combined.contains("anuv jain") || combined.contains("prateek kuhad") -> MyPlayerVibeCategory.SAD_HEARTBREAK
            else -> MyPlayerVibeCategory.CHILL_LOFI
        }
    }

    fun onPlaybackTick(
        title: String,
        artist: String,
        album: String,
        isPlaying: Boolean
    ) {
        if (!enabled || !isPlaying || title.isBlank()) {
            lastTickRealtimeMs = System.currentTimeMillis()
            return
        }

        val now = System.currentTimeMillis()
        val deltaMs = if (lastTickRealtimeMs > 0L) (now - lastTickRealtimeMs).coerceIn(0L, 2500L) else 0L
        lastTickRealtimeMs = now

        val detectedVibe = classifySongVibe(title, artist, album)
        if (currentStreakCategory == detectedVibe) {
            accumulatedListeningMs += deltaMs
        } else {
            currentStreakCategory = detectedVibe
            accumulatedListeningMs = deltaMs
        }

        val targetMs = thresholdMinutes.coerceAtLeast(1) * 60_000L
        if (accumulatedListeningMs >= targetMs && activePopup == null) {
            val mins = (accumulatedListeningMs / 60_000L).toInt().coerceAtLeast(thresholdMinutes)
            accumulatedListeningMs = 0L
            activePopup = buildFunnyPopup(detectedVibe, mins, title)
        }
    }

    fun triggerPreviewPopup(category: MyPlayerVibeCategory, currentSongTitle: String = "Current Track") {
        activePopup = buildFunnyPopup(category, thresholdMinutes, currentSongTitle)
    }

    fun dismissPopup() {
        activePopup = null
    }

    private fun buildFunnyPopup(
        category: MyPlayerVibeCategory,
        mins: Int,
        songTitle: String
    ): MyPlayerVibePopupEvent {
        val pick = msgVariantIdx++
        return when (category) {
            MyPlayerVibeCategory.SAD_HEARTBREAK -> {
                val options = listOf(
                    "भाई बहुत सैड लग रहा है... क्या बात हो गई? 🥺💔" to
                            "पिछले $mins मिनट से लगातार Sad Songs सुन रहा है! सब ठीक तो है भाई? किसने दिल तोड़ दिया या पुरानी यादें ताज़ा हो गईं? पानी पी ले यार, हम हैं ना!",
                    "अरे भाई बस कर, रुलाएगा क्या? 😭🎻" to
                            "पिछले $mins मिनट से फुल आशिकी/दर्द भरे गाने बज रहे हैं! Arijit Singh भी फोन से बाहर आकर पूछने वाला है — 'भाई टिशू पेपर भिजवाऊं या मूड चेंज करें?'",
                    "चेतावनी ⚠️: Sadness Overload! 💔" to
                            "$mins मिनट से सैड सॉन्ग चल रहे हैं! इतनी देर दर्द भरे गाने सुनने से पुरानी चैट्स स्क्रॉल करने का खतरा 99% बढ़ जाता है। चलो अब थोड़ा माहौल बदलते हैं!"
                )
                val (head, body) = options[pick % options.size]
                MyPlayerVibePopupEvent(category, mins, head, body, songTitle)
            }

            MyPlayerVibeCategory.PARTY_BANGER -> {
                val options = listOf(
                    "ओहो! आज तो फुल मूड में लग रहा है भाई! 🕺🎉" to
                            "पिछले $mins मिनट से लगातार Party Bangers बज रहे हैं! क्या खुशी की बात है भाई? लॉटरी लग गई, सैलरी आ गई या क्रश का रिप्लाई आ गया?",
                    "अकेले-अकेले पार्टी हो रही है भाई? 🥳🔥" to
                            "$mins मिनट से नॉन-स्टॉप डांस और पार्टी ट्रैक चल रहे हैं! पड़ोसी भी पूछ रहे हैं कि आज किस खुशी में DJ बना बैठा है?",
                    "आज तो अलग ही लेवल का माहौल है! ⚡🍾" to
                            "लगातार $mins मिनट से फुल एनर्जी पार्टी म्यूजिक चल रहा है! ऐसे ही खुश रह मेरे भाई, पार्टी रुकनी नहीं चाहिए!"
                )
                val (head, body) = options[pick % options.size]
                MyPlayerVibePopupEvent(category, mins, head, body, songTitle)
            }

            MyPlayerVibeCategory.ROMANTIC_LOVE -> {
                val options = listOf(
                    "आय हाय! बड़े रोमांटिक गाने सुने जा रहे हैं! 😍🌹" to
                            "पिछले $mins मिनट से सिर्फ प्यार-मोहब्बत वाले गाने चल रहे हैं! भाई चक्कर क्या है? कोई खास मिल गया है या हवाओं में इश्क घुल रहा है?",
                    "चेहरे की मुस्कान छुपाए नहीं छुप रही भाई! 💘✨" to
                            "$mins मिनट से नॉन-स्टॉप Romantic Vibe चल रही है! किसके ख्यालों में खोया हुआ है मेरे भाई? नज़र ना लगे!"
                )
                val (head, body) = options[pick % options.size]
                MyPlayerVibePopupEvent(category, mins, head, body, songTitle)
            }

            MyPlayerVibeCategory.WORKOUT_HUSTLE -> {
                val options = listOf(
                    "भाई आज ही हिमालय फतेह करेगा क्या? 💪🔥" to
                            "पिछले $mins मिनट से फुल Beast Mode और Banger गाने बज रहे हैं! जिम में लोहा पिघला रहा है या किसी से बदला लेने की तैयारी चल रही है?",
                    "एनर्जी मीटर 200% पार हो गया भाई! ⚡😤" to
                            "$mins मिनट से लगातार तगड़े Hustle/Hip-Hop ट्रैक चल रहे हैं! आज तो भाई को कोई नहीं रोक सकता!"
                )
                val (head, body) = options[pick % options.size]
                MyPlayerVibePopupEvent(category, mins, head, body, songTitle)
            }

            MyPlayerVibeCategory.DEVOTIONAL_PEACE -> {
                val (head, body) = "वाह भाई! आज तो एकदम भक्ति और सुकून का माहौल है 🙏✨" to
                        "पिछले $mins मिनट से एकदम शांत और सुकून देने वाले गाने बज रहे हैं। मन एकदम हल्का हो गया ना?"
                MyPlayerVibePopupEvent(category, mins, head, body, songTitle)
            }

            MyPlayerVibeCategory.CHILL_LOFI -> {
                val options = listOf(
                    "भाई बड़ी गहरी सोच में लग रहा है? 🌙🎧" to
                            "पिछले $mins मिनट से लगातार Chill / Lo-Fi वाइब चल रही है! ओवरथिंकिंग चल रही है या बस सुकून के पल जी रहा है?",
                    "एकदम सुकून वाला माहौल सेट है भाई! ✨😌" to
                            "$mins मिनट से नॉन-स्टॉप स्मूद म्यूजिक सुन रहा है! चाय/कॉफी साथ में है या बस हेडफोन लगा के दुनिया ऑफ कर रखी है?"
                )
                val (head, body) = options[pick % options.size]
                MyPlayerVibePopupEvent(category, mins, head, body, songTitle)
            }
        }
    }
}

@Composable
fun MyPlayerVibeCheckPopupDialog(
    event: MyPlayerVibePopupEvent,
    onDismiss: () -> Unit,
    onSwitchVibeClick: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "vibeEmojiPulse")
    val emojiScale by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(720, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emojiBounce"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .shadow(24.dp, RoundedCornerShape(30.dp)),
            shape = RoundedCornerShape(30.dp),
            color = Color(0xFF121217),
            border = BorderStroke(
                width = 1.8.dp,
                brush = Brush.linearGradient(
                    colors = listOf(event.category.accentColor, event.category.secondaryColor)
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                event.category.accentColor.copy(alpha = 0.22f),
                                Color(0xFF121217),
                                Color(0xFF0C0C10)
                            )
                        )
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = event.category.accentColor.copy(alpha = 0.20f),
                    border = BorderStroke(1.dp, event.category.accentColor.copy(alpha = 0.60f))
                ) {
                    Text(
                        text = "${event.category.badgeText} • ${event.elapsedMinutes} MIN STREAK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .scale(emojiScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    event.category.accentColor.copy(alpha = 0.38f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .border(1.5.dp, event.category.accentColor.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = event.category.emoji, fontSize = 38.sp)
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = event.headline,
                    fontSize = 21.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = event.funnyMessage,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = Color.White.copy(alpha = 0.86f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(22.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = event.category.accentColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = event.category.primaryButtonText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onSwitchVibeClick()
                    },
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = event.category.switchButtonText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MyPlayerFunnyVibeCheckSettingsCard(
    myplayerState: MyPlayerUiStateHolder
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MyPlayerSurface,
        border = BorderStroke(1.dp, MyPlayerAccentPurple.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MyPlayerAccentPurple.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("😭🎉", fontSize = 18.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Desi Mood & Vibe-Check Pop-up",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Shows a funny pop-up when you listen to Sad, Party, Romantic or Gym songs non-stop",
                        fontSize = 12.sp,
                        color = MyPlayerTextSecondary
                    )
                }
                Switch(
                    checked = myplayerState.vibePopupEnabled,
                    onCheckedChange = { myplayerState.vibePopupEnabled = it }
                )
            }

            AnimatedVisibility(visible = myplayerState.vibePopupEnabled) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "POP-UP TIME INTERVAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MyPlayerTextSecondary
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            1 to "1 min (Test)",
                            10 to "10 min",
                            15 to "15 min",
                            30 to "30 min"
                        ).forEach { (mins, label) ->
                            val selected = myplayerState.vibePopupIntervalMin == mins
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (selected) Color.White else MyPlayerSurfaceHigh,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { myplayerState.vibePopupIntervalMin = mins }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.Black else Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "INSTANT PREVIEW (TAP TO TEST POP-UP NOW)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MyPlayerAccentGreen
                    )
                    Spacer(Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MyPlayerVibeCategory.entries) { cat ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = cat.accentColor.copy(alpha = 0.20f),
                                border = BorderStroke(1.dp, cat.accentColor.copy(alpha = 0.65f)),
                                modifier = Modifier.clickable {
                                    MyPlayerVibeCheckManager.triggerPreviewPopup(cat)
                                }
                            ) {
                                Text(
                                    text = "${cat.emoji} ${cat.label}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyPlayerPlaybackAndMoodSettingsScreen(
    myplayerState: MyPlayerUiStateHolder,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MyPlayerBlack).statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(42.dp).clip(CircleShape).background(MyPlayerSurface)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Spacer(Modifier.height(10.dp))
            Text("Playback &\nMood Pop-up", fontSize = 30.sp, lineHeight = 35.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
        item { MyPlayerFunnyVibeCheckSettingsCard(myplayerState = myplayerState) }
        item { Spacer(Modifier.height(90.dp)) }
    }
}