package com.droidbubble.app.model

data class FpsInfo(
    val fps: Int = 60,
    val refreshRateHz: Int = 120,
    val frameTimeMs: Float = 16.6f,
    val isSmooth: Boolean = true
)

data class WeatherInfo(
    val temperatureC: Int = 22,
    val condition: String = "Partly Cloudy",
    val highC: Int = 25,
    val lowC: Int = 16,
    val humidityPct: Int = 54,
    val windKmh: Int = 14,
    val cityName: String = "Local Weather"
)

data class NetworkSpeedInfo(
    val downloadSpeedFormatted: String = "0 KB/s",
    val uploadSpeedFormatted: String = "0 KB/s",
    val downloadBytesPerSec: Long = 0L,
    val uploadBytesPerSec: Long = 0L,
    val isWifi: Boolean = true,
    val networkLabel: String = "Wi-Fi",
    val sessionTotalMb: Long = 0L
)
