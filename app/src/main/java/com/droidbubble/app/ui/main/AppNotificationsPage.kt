package com.droidbubble.app.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.droidbubble.app.model.IslandSettings

/** Which apps notify on the island, laid out like a watch companion app's notification settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNotificationsPage(
    settings: IslandSettings,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit,
    apps: List<AppItem>,
    isLoadingApps: Boolean,
    onLoadApps: () -> Unit,
    onToggleApp: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { onLoadApps() }

    var query by rememberSaveable { mutableStateOf("") }
    val shownApps = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.appName.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("App Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingsCardItem(position = GroupPosition.TOP, isActive = settings.showNotifications) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Notifications on Island", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("New notifications from the apps turned on below pop up on the island") },
                            leadingContent = {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.showNotifications,
                                    onCheckedChange = { show -> onUpdateSettings { it.copy(showNotifications = show) } }
                                )
                            }
                        )
                    }
                    ReplaceSystemPopupsItem(
                        settings = settings,
                        onUpdateSettings = onUpdateSettings,
                        position = GroupPosition.BOTTOM
                    )
                }
            }

            item {
                Text(
                    text = "Pop up on island",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 4.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            if (isLoadingApps) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            itemsIndexed(shownApps, key = { _, app -> app.packageName }) { index, app ->
                val position = when {
                    shownApps.size == 1 -> GroupPosition.SINGLE
                    index == 0 -> GroupPosition.TOP
                    index == shownApps.lastIndex -> GroupPosition.BOTTOM
                    else -> GroupPosition.MIDDLE
                }
                SettingsCardItem(position = position) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(app.appName, fontWeight = FontWeight.Medium) },
                        leadingContent = {
                            if (app.icon != null) {
                                val bitmap = remember(app.icon) { app.icon.toBitmap(96, 96) }
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(36.dp))
                            }
                        },
                        trailingContent = {
                            Switch(
                                checked = !app.isBlocked,
                                enabled = settings.showNotifications,
                                onCheckedChange = { onToggleApp(app.packageName) }
                            )
                        }
                    )
                }
            }
        }
    }
}
