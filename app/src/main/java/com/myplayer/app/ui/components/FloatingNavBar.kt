package com.myplayer.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myplayer.app.ui.navigation.Screen
import com.myplayer.app.ui.theme.ThemeState

data class NavItem(
    val route: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

val bottomNavItems = listOf(
    NavItem(Screen.Home.route,      "Home",      Icons.Filled.Home,         Icons.Outlined.Home),
    NavItem(Screen.Search.route,    "Search",    Icons.Filled.Search,       Icons.Outlined.Search),
    NavItem(Screen.Library.route,   "Library",   Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    NavItem(Screen.Trending.route,  "Hotlist",   Icons.Filled.Whatshot,     Icons.Outlined.Whatshot),
    NavItem(Screen.Settings.route,  "Settings",  Icons.Filled.Settings,     Icons.Outlined.Settings)
)

@Composable
fun FloatingNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = ThemeState.isDarkMode
    val navShape = RoundedCornerShape(28.dp)

    val glassGradient = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(Color(0xDD1C1C28), Color(0xEE12121A))
        } else {
            listOf(Color(0xEEF5F5FA), Color(0xF5E8E8EE))
        }
    )

    val borderBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f))
        } else {
            listOf(Color.White.copy(alpha = 0.70f), Color.Black.copy(alpha = 0.08f))
        }
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Frosted Glass Blurred Rounded Rectangle Container Background
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = 18.dp,
                    shape = navShape,
                    spotColor = Color.Black.copy(alpha = 0.65f),
                    ambientColor = Color.Black.copy(alpha = 0.35f)
                )
                .clip(navShape)
                .background(glassGradient)
                .border(BorderStroke(1.dp, borderBrush), shape = navShape)
        )

        // 2. Foreground Crisp Navigation Items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentRoute == item.route
                val tint by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else if (isDark) {
                        Color(0xFF9E9EA8)
                    } else {
                        Color(0xFF6E6E78)
                    },
                    animationSpec = tween(180),
                    label = "navTint_${item.route}"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onNavigate(item.route) }
                        )
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isSelected) item.activeIcon else item.inactiveIcon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = tint
                    )
                }
            }
        }
    }
}
