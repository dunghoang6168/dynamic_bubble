package com.droidbubble.app.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsPaused
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.STATUS_ICON_SLOTS
import com.droidbubble.app.util.ShizukuHelper

private val STATUS_ICON_LABELS = mapOf(
    "alarm_clock" to "Alarm",
    "bluetooth" to "Bluetooth",
    "volume" to "Ringer",
    "zen" to "Do Not Disturb",
    "wifi" to "Wi-Fi",
    "mobile" to "Mobile Signal",
    "airplane" to "Airplane",
    "vpn" to "VPN",
    "battery" to "Battery"
)

@Composable
fun ReplaceSystemPopupsItem(
    settings: IslandSettings,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit,
    position: GroupPosition = GroupPosition.MIDDLE
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(ShizukuHelper.hasWriteSecureSettings(context)) }
    LifecycleResumeEffect(Unit) {
        hasPermission = ShizukuHelper.hasWriteSecureSettings(context)
        onPauseOrDispose { }
    }
    val active = settings.replaceSystemPopups && hasPermission

    SettingsCardItem(position = position, isActive = active) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text("Pop-ups Shown by Island", fontWeight = FontWeight.SemiBold) },
            supportingContent = {
                Text(
                    when {
                        !hasPermission -> "Needs WRITE_SECURE_SETTINGS; grant it from Move Status Icons Into Island in Appearance."
                        active -> "Android's own pop-ups are off. Apps turned on below pop up on the island; incoming calls always do."
                        else -> "Android is showing its own pop-ups, so apps turned on below pop up twice."
                    }
                )
            },
            leadingContent = {
                Icon(Icons.Default.NotificationsPaused, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingContent = {
                Switch(
                    checked = active,
                    enabled = hasPermission,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            // Restore right away; the overlay service may not be running to do it
                            ShizukuHelper.setSystemHeadsUpEnabled(context, true)
                        }
                        onUpdateSettings { it.copy(replaceSystemPopups = enabled) }
                    }
                )
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HideStatusIconsSection(
    settings: IslandSettings,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(ShizukuHelper.hasWriteSecureSettings(context)) }
    LifecycleResumeEffect(Unit) {
        hasPermission = ShizukuHelper.hasWriteSecureSettings(context)
        onPauseOrDispose { }
    }
    val active = settings.hideStatusIcons && hasPermission

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SettingsCardItem(position = if (active) GroupPosition.TOP else GroupPosition.SINGLE, isActive = active) {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text("Move Status Icons Into Island", fontWeight = FontWeight.SemiBold) },
                supportingContent = {
                    Text(
                        if (hasPermission) "Hides the chosen status bar icons and shows them when you tap the pill. Works on AOSP-like ROMs; Samsung and HyperOS may ignore it."
                        else "Needs WRITE_SECURE_SETTINGS, granted once via Shizuku or adb."
                    )
                },
                leadingContent = {
                    Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingContent = {
                    Switch(
                        checked = active,
                        enabled = hasPermission,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                // Restore right away; the overlay service may not be running to do it
                                ShizukuHelper.applyHiddenStatusIcons(context, emptySet())
                            }
                            onUpdateSettings { it.copy(hideStatusIcons = enabled) }
                        }
                    )
                }
            )
        }

        if (!hasPermission) {
            SettingsCardItem(position = GroupPosition.BOTTOM) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (!ShizukuHelper.isShizukuAvailable()) {
                                ShizukuHelper.openShizukuOrStore(context)
                                return@FilledTonalButton
                            }
                            ShizukuHelper.grantWriteSecureSettingsViaShizuku(context) { granted ->
                                hasPermission = ShizukuHelper.hasWriteSecureSettings(context)
                                if (!granted) {
                                    Toast.makeText(context, "Shizuku did not grant the permission", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant via Shizuku")
                    }
                    OutlinedButton(
                        onClick = {
                            val command = "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
                            context.getSystemService(ClipboardManager::class.java)
                                ?.setPrimaryClip(ClipData.newPlainText("adb command", command))
                            Toast.makeText(context, "adb command copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copy adb Command")
                    }
                }
            }
        }

        if (active) {
            SettingsCardItem(position = GroupPosition.BOTTOM) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Icons to hide",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        STATUS_ICON_SLOTS.forEach { slot ->
                            val selected = slot in settings.hiddenStatusIcons
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onUpdateSettings {
                                        it.copy(hiddenStatusIcons = if (selected) it.hiddenStatusIcons - slot else it.hiddenStatusIcons + slot)
                                    }
                                },
                                label = { Text(STATUS_ICON_LABELS[slot] ?: slot) }
                            )
                        }
                    }
                    if (!settings.alwaysShowPill) {
                        Text(
                            "Turn on Always Show Pill so there is always a pill to tap.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
