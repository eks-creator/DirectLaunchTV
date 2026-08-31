package com.example.directlaunchtv

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

object AccessibilityHelper {
    fun hasSecureSettingsPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    fun isServiceEnabled(context: Context): Boolean {
        val wanted = ComponentName(context, RemoteShortcutService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabled.split(':').any { it.equals(wanted, ignoreCase = true) }
    }

    fun enableService(context: Context): Boolean {
        if (!hasSecureSettingsPermission(context)) return false
        val wanted = ComponentName(context, RemoteShortcutService::class.java).flattenToString()
        val current = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        val services = current.split(':')
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "null" }
            .toMutableList()
        if (services.none { it.equals(wanted, ignoreCase = true) }) services.add(wanted)

        val wroteList = Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            services.distinct().joinToString(":")
        )
        val wroteEnabled = Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            1
        )
        return wroteList && wroteEnabled
    }
}
