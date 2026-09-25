package com.example.myplayer.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToNowPlaying: () -> Unit,
    bottomPadding: Dp = 100.dp,
    onlineViewModel: OnlineSearchViewModel = hiltViewModel()
) {
    val query by onlineViewModel.query.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CloudBlueBackground)
            .statusBarsPadding()
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                "Search",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = OnSurface
            )
            Spacer(Modifier.height(16.dp))

            // ── Search bar ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .claySurface(borderRadius = 24.dp, backgroundColor = SurfaceLight)
            ) {
                TextField(
                    value = query,
                    onValueChange = onlineViewModel::onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp, vertical = 1.5.dp),
                    placeholder = {
                        Text(
                            "Search songs, artists, albums…",
                            color = OnSurfaceVariant
                        )
                    },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = ClayPrimary, modifier = Modifier.size(24.dp)) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onlineViewModel.onQueryChange("") }) {
                                Icon(Icons.Filled.Close, null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = OnSurface)
                )
            }
        }

        // ── Content ───────────────────────────────────────────────────────────
        OnlineSearchScreen(
            onNavigateToNowPlaying = onNavigateToNowPlaying,
            bottomPadding = bottomPadding,
            viewModel = onlineViewModel
        )
    }
}
