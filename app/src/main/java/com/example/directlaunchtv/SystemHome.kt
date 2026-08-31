package com.example.directlaunchtv

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings

object SystemHome {
    fun findSystemHome(context: Context): ComponentName? {
        val pm = context.packageManager
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val candidates = pm.queryIntentActivities(homeIntent, PackageManager.MATCH_ALL)

        val systemCandidate = candidates.firstOrNull { info ->
            val pkg = info.activityInfo.packageName
            val flags = info.activityInfo.applicationInfo.flags
            pkg != context.packageName &&
                (flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
                 flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0)
        }

        val anyOtherCandidate = candidates.firstOrNull {
            it.activityInfo.packageName != context.packageName
        }

        val selected = systemCandidate ?: anyOtherCandidate ?: return null
        return ComponentName(selected.activityInfo.packageName, selected.activityInfo.name)
    }

    fun isSystemHomePackage(context: Context, packageName: String): Boolean {
        if (packageName == context.packageName) return false
        val pm = context.packageManager
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return pm.queryIntentActivities(homeIntent, PackageManager.MATCH_ALL).any {
            it.activityInfo.packageName == packageName
        } || packageName == "com.google.android.apps.tv.launcherx" ||
            packageName == "com.google.android.tungsten.setupwraith" ||
            packageName == "com.google.android.tvlauncher"
    }

    fun open(context: Context, bypassMs: Long = 60000L): Boolean {
        Config.allowGoogleTvTemporarily(context, bypassMs)
        val component = findSystemHome(context)
        if (component != null) {
            return runCatching {
                val intent = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .setComponent(component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }

        return runCatching {
            context.startActivity(
                Intent(Settings.ACTION_HOME_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        }.getOrDefault(false)
    }
}
