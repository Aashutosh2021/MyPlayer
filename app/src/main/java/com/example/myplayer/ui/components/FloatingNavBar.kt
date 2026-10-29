package com.example.myplayer.ui.components

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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myplayer.ui.navigation.Screen
import com.example.myplayer.ui.theme.*

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
    val navShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    // Frosted glass translucent bottom dock
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = navShape,
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(navShape)
            .background(Color(0xD9121217))
            .border(
                BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f)),
                shape = navShape
            )
            .navigationBarsPadding()
            .padding(top = 10.dp, bottom = 8.dp, start = 12.dp, end = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.route
            val tint by animateColorAsState(
                targetValue = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary else Color(0xFF8E8E98),
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
