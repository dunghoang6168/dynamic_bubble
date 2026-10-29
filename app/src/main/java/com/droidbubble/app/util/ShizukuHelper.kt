package com.droidbubble.app.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.droidbubble.app.model.STATUS_ICON_SLOTS
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors

object ShizukuHelper {

    private const val TAG = "ShizukuHelper"
    const val SHIZUKU_PERMISSION_REQUEST_CODE = 4001
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private const val ICON_BLACKLIST = "icon_blacklist"
    // Extra SystemUI slots behind one offered slot (Android 14+ moved the silent icon from "volume" to "mute")
    private val LINKED_SLOTS = mapOf("volume" to listOf("mute"))
    private val MANAGED_SLOTS = STATUS_ICON_SLOTS + LINKED_SLOTS.values.flatten()

    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        if (!isShizukuAvailable()) return false
        return try {
            if (Shizuku.isPreV11()) false
            else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun hasWriteSecureSettings(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    fun requestShizukuPermission(): Boolean {
        return try {
            if (isShizukuAvailable() && !hasShizukuPermission()) {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
                true
            } else {
                false
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to request Shizuku permission", e)
            false
        }
    }

    fun grantWriteSecureSettings(packageName: String, onResult: (Boolean) -> Unit) {
        if (!isShizukuAvailable() || !hasShizukuPermission()) {
            onResult(false)
            return
        }
        executor.execute {
            val success = runShellCommand("pm grant $packageName android.permission.WRITE_SECURE_SETTINGS")
            mainHandler.post { onResult(success) }
        }
    }

    /**
     * Requests Shizuku access if needed, then grants WRITE_SECURE_SETTINGS to this app.
     */
    fun grantWriteSecureSettingsViaShizuku(context: Context, onResult: (Boolean) -> Unit) {
        if (!isShizukuAvailable() || Shizuku.isPreV11()) {
            onResult(false)
            return
        }
        if (hasShizukuPermission()) {
            grantWriteSecureSettings(context.packageName, onResult)
            return
        }
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (requestCode != SHIZUKU_PERMISSION_REQUEST_CODE) return
                Shizuku.removeRequestPermissionResultListener(this)
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    grantWriteSecureSettings(context.packageName, onResult)
                } else {
                    onResult(false)
                }
            }
        }
        Shizuku.addRequestPermissionResultListener(listener, mainHandler)
        if (!requestShizukuPermission()) {
            Shizuku.removeRequestPermissionResultListener(listener)
            onResult(false)
        }
    }

    /**
     * Hides exactly [slots] among [STATUS_ICON_SLOTS] in SystemUI's icon_blacklist; an empty set restores them.
     * Slots outside [STATUS_ICON_SLOTS] already in the list are kept.
     */
    fun applyHiddenStatusIcons(context: Context, slots: Set<String>): Boolean {
        if (!hasWriteSecureSettings(context)) return false
        val resolver = context.contentResolver
        // Hidden keys are not readable for apps targeting S+ on most builds; then we start from an empty list.
        // ponytail: an offered slot hidden by another tool gets unhidden on restore; persist the original list if that matters
        val current = runCatching { Settings.Secure.getString(resolver, ICON_BLACKLIST) }.getOrNull()
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        val wanted = slots.filter { it in STATUS_ICON_SLOTS }.flatMap { listOf(it) + LINKED_SLOTS[it].orEmpty() }
        val updated = (current.filter { it !in MANAGED_SLOTS } + wanted).distinct()
        return runCatching {
            Settings.Secure.putString(resolver, ICON_BLACKLIST, updated.joinToString(","))
        }.onFailure { Log.e(TAG, "Failed to write icon_blacklist", it) }.getOrDefault(false)
    }

    private fun runShellCommand(command: String): Boolean {
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as java.lang.Process
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val reader = BufferedReader(InputStreamReader(process.errorStream))
                val err = reader.readText()
                Log.e(TAG, "Command failed ($exitCode): $err")
            }
            exitCode == 0
        } catch (e: Throwable) {
            Log.e(TAG, "Error executing shell command via Shizuku: $command", e)
            false
        }
    }

    fun getShizukuLaunchIntent(context: Context): Intent? {
        val pm = context.packageManager
        return pm.getLaunchIntentForPackage("moe.shizuku.privileged.api")
    }

    fun openShizukuOrStore(context: Context) {
        val launchIntent = getShizukuLaunchIntent(context)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        } else {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")))
            } catch (_: Exception) {
            }
        }
    }
}
