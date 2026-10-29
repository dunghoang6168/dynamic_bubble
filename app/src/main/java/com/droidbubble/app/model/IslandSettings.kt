package com.droidbubble.app.model

val DEFAULT_FEATURE_PRIORITIES = listOf(
    "MEDIA",
    "PROGRESS",
    "RECORDING",
    "NOTIFICATION",
    "FLASHLIGHT"
)

// SystemUI icon_blacklist slot names that the island can show in place of the status bar icon
val STATUS_ICON_SLOTS = listOf(
    "alarm_clock",
    "bluetooth",
    "volume",
    "zen",
    "wifi",
    "mobile",
    "airplane",
    "vpn",
    "battery"
)

val DEFAULT_HIDDEN_STATUS_ICONS = setOf("alarm_clock", "bluetooth", "volume", "zen")

data class IslandSettings(
    val isEnabled: Boolean = true,
    val widthDp: Int = 180,
    val heightDp: Int = 36,
    val cornerRadiusDp: Int = 22,
    val topOffsetDp: Int = 12,
    val horizontalOffsetDp: Int = 0,
    val showOutline: Boolean = true,
    val outlineColorArgb: Long = 0x33FFFFFF, // Sleek translucent silver
    val outlineThicknessDp: Float = 1.5f,
    val expandedScale: Float = 1.1f,
    val hapticsEnabled: Boolean = true,
    val showMedia: Boolean = true,
    val showNotifications: Boolean = false,
    val showLiveProgress: Boolean = true,
    val showScreenRecording: Boolean = true,
    val showFlashlight: Boolean = true,
    val showBluetooth: Boolean = true,
    val showCharging: Boolean = true,
    val showRingerMode: Boolean = true,
    val showInLandscape: Boolean = false,
    val animationSpeed: Float = 0.7f,
    val blockedPackages: Set<String> = emptySet(),
    val restartOnBoot: Boolean = true,
    val bubbleEffectEnabled: Boolean = true,
    val dismissMediaOnPause: Boolean = false,
    val alwaysShowPill: Boolean = false,
    val showFps: Boolean = false,
    val showWeather: Boolean = false,
    val showNetworkSpeed: Boolean = false,
    val autoDismissNotifications: Boolean = false,
    val blurredAlbumArtBackground: Boolean = false,
    val expandDamping: Float = 0.74f,
    val expandStiffness: Float = 380f,
    val appearDamping: Float = 0.6f,
    val appearStiffness: Float = 280f,
    val splitDamping: Float = 0.63f,
    val splitStiffness: Float = 320f,
    val featurePriorities: List<String> = DEFAULT_FEATURE_PRIORITIES,
    val hideStatusIcons: Boolean = false,
    val popupWidthPercent: Int = 65,
    val replaceSystemPopups: Boolean = true,
    val hiddenStatusIcons: Set<String> = DEFAULT_HIDDEN_STATUS_ICONS
)
