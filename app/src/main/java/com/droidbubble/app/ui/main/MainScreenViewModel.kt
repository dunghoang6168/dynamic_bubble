package com.droidbubble.app.ui.main

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidbubble.app.data.SettingsRepository
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.service.IslandOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isBlocked: Boolean = false
)

class MainScreenViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<IslandSettings> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = IslandSettings()
        )

    private val _pendingPermissions = MutableStateFlow<List<PermissionItem>>(emptyList())
    val pendingPermissions: StateFlow<List<PermissionItem>> = _pendingPermissions.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    fun checkPermissions(context: Context) {
        _pendingPermissions.value = PermissionHelper.getPendingPermissions(context)
    }

    fun updateSettings(update: (IslandSettings) -> IslandSettings) {
        viewModelScope.launch {
            repository.updateSettings(update)
        }
    }

    fun toggleEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(isEnabled = enabled) }
            if (enabled && PermissionHelper.isOverlayGranted(context)) {
                IslandOverlayService.start(context)
            } else {
                IslandOverlayService.stop(context)
            }
        }
    }

    fun loadInstalledApps(context: Context) {
        if (_installedApps.value.isNotEmpty()) return
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(intent, 0)
                val currentBlocked = settings.value.blockedPackages

                resolveInfos.mapNotNull { resolveInfo ->
                    val pkg = resolveInfo.activityInfo.packageName
                    if (pkg == context.packageName) return@mapNotNull null
                    val name = resolveInfo.loadLabel(pm).toString()
                    val icon = resolveInfo.loadIcon(pm)
                    AppItem(
                        packageName = pkg,
                        appName = name,
                        icon = icon,
                        isBlocked = currentBlocked.contains(pkg)
                    )
                }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
            }
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }

    fun toggleAppBlocked(packageName: String) {
        viewModelScope.launch {
            val currentBlocked = settings.value.blockedPackages.toMutableSet()
            if (currentBlocked.contains(packageName)) {
                currentBlocked.remove(packageName)
            } else {
                currentBlocked.add(packageName)
            }
            repository.updateSettings { it.copy(blockedPackages = currentBlocked) }
            _installedApps.value = _installedApps.value.map { app ->
                if (app.packageName == packageName) {
                    app.copy(isBlocked = currentBlocked.contains(packageName))
                } else app
            }
        }
    }
}
