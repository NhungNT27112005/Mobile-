package com.example.mobile.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mobile.navigation.Routes

@Composable
fun NewsHubBottomBar(
    selectedRoute: String,
    onRouteSelected: (String) -> Unit
) {
    val items = listOf(
        Triple(Routes.HOME, "Trang chủ", Icons.Outlined.Home),
        Triple(Routes.SEARCH, "Tìm kiếm", Icons.Outlined.Search),
        Triple(Routes.SAVED, "Đã lưu", Icons.Outlined.List),
        Triple(Routes.PROFILE, "Cá nhân", Icons.Outlined.AccountCircle)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = selectedRoute == route,
                enabled = route == Routes.HOME ||
                    route == Routes.SEARCH ||
                    route == Routes.PROFILE,
                onClick = { onRouteSelected(route) },
                icon = {
                    Icon(icon, contentDescription = null)
                },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
