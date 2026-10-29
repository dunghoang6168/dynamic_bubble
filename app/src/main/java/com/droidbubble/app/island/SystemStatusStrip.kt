package com.droidbubble.app.island

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Date

private data class SystemStatus(
    val nextAlarm: String? = null,
    val bluetoothOn: Boolean = false,
    val bluetoothConnected: Boolean = false,
    val ringerMode: Int = AudioManager.RINGER_MODE_NORMAL,
    val dnd: Boolean = false,
    val wifi: Boolean = false,
    val vpn: Boolean = false,
    val mobileLevel: Int? = null,
    val airplane: Boolean = false,
    val batteryPct: Int = 0,
    val charging: Boolean = false
)

@SuppressLint("MissingPermission")
private fun readSystemStatus(context: Context): SystemStatus {
    val alarm = context.getSystemService(AlarmManager::class.java)?.nextAlarmClock
    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    val bluetoothOn = runCatching { adapter?.isEnabled == true }.getOrDefault(false)
    // Profile state needs BLUETOOTH_CONNECT; without it we only know the radio is on
    val bluetoothConnected = bluetoothOn && runCatching {
        adapter?.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED ||
            adapter?.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED
    }.getOrDefault(false)
    val filter = context.getSystemService(NotificationManager::class.java)?.currentInterruptionFilter
    val cm = context.getSystemService(ConnectivityManager::class.java)
    val caps = runCatching { cm?.getNetworkCapabilities(cm.activeNetwork) }.getOrNull()
    val tm = context.getSystemService(TelephonyManager::class.java)
    val battery = context.getSystemService(BatteryManager::class.java)
    return SystemStatus(
        nextAlarm = alarm?.let { DateFormat.getTimeFormat(context).format(Date(it.triggerTime)) },
        bluetoothOn = bluetoothOn,
        bluetoothConnected = bluetoothConnected,
        ringerMode = context.getSystemService(AudioManager::class.java)?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL,
        dnd = filter != null && filter != NotificationManager.INTERRUPTION_FILTER_ALL && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN,
        wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true,
        vpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true,
        mobileLevel = if (tm?.simState == TelephonyManager.SIM_STATE_READY) runCatching { tm.signalStrength?.level }.getOrNull() else null,
        airplane = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1,
        batteryPct = battery?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0,
        charging = battery?.isCharging == true
    )
}

/**
 * Row of the status bar icons the user hid via icon_blacklist, shown above the expanded island cards.
 */
@Composable
fun SystemStatusStrip(slots: Set<String>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // ponytail: polls every 2s while the island is expanded; switch to system callbacks if it shows up in battery stats
    val status by produceState(initialValue = readSystemStatus(context)) {
        while (true) {
            delay(2000)
            value = readSystemStatus(context)
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var shown = 0
        if ("alarm_clock" in slots && status.nextAlarm != null) {
            StatusItem(Icons.Default.Alarm, status.nextAlarm); shown++
        }
        if ("bluetooth" in slots && status.bluetoothOn) {
            StatusItem(if (status.bluetoothConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth); shown++
        }
        if ("volume" in slots && status.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            StatusItem(if (status.ringerMode == AudioManager.RINGER_MODE_VIBRATE) Icons.Default.Vibration else Icons.AutoMirrored.Filled.VolumeOff); shown++
        }
        if ("zen" in slots && status.dnd) {
            StatusItem(Icons.Default.DoNotDisturbOn); shown++
        }
        if ("airplane" in slots && status.airplane) {
            StatusItem(Icons.Default.AirplanemodeActive); shown++
        }
        if ("vpn" in slots && status.vpn) {
            StatusItem(Icons.Default.VpnKey); shown++
        }
        if ("wifi" in slots && status.wifi) {
            StatusItem(Icons.Default.Wifi); shown++
        }
        if ("mobile" in slots && !status.airplane && status.mobileLevel != null) {
            CellularSignalBars(level = status.mobileLevel!!); shown++
        }
        if ("battery" in slots) {
            StatusItem(if (status.charging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd, "${status.batteryPct}%"); shown++
        }
        if (shown == 0) {
            Text("No status icons", color = Color(0xFFA1A1AA), fontSize = 12.sp)
        }
    }
}

@Composable
private fun StatusItem(icon: ImageVector, label: String? = null) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        if (label != null) {
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
