package com.example.directlaunchtv

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

class RemoteShortcutService : AccessibilityService() {
    private var count = 0
    private var firstPressAt = 0L
    private var lastRedirectAt = 0L

    override fun onServiceConnected() {
        instance = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (Config.googleTvBypassActive(this)) return
        if (!SystemHome.isSystemHomePackage(this, packageName)) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastRedirectAt < 1200L) return
        lastRedirectAt = now
        launchTarget()
    }

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_UP) return false

        val expectedKey = Config.shortcutKeyCode(this)
        if (event.keyCode != expectedKey) {
            resetCounter()
            return false
        }

        val now = SystemClock.elapsedRealtime()
        val window = Config.shortcutWindowMs(this)
        if (firstPressAt == 0L || now - firstPressAt > window) {
            firstPressAt = now
            count = 1
        } else {
            count++
        }

        if (count >= Config.shortcutPressCount(this)) {
            resetCounter()
            SystemHome.open(this)
        }

        return false
    }

    private fun launchTarget() {
        val pkg = Config.targetPackage(this) ?: return
        val intent = packageManager.getLeanbackLaunchIntentForPackage(pkg)
            ?: packageManager.getLaunchIntentForPackage(pkg)
            ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        runCatching { startActivity(intent) }
    }

    private fun resetCounter() {
        count = 0
        firstPressAt = 0L
    }

    fun showPowerDialog(): Boolean = performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)

    fun lockScreen(): Boolean = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)

    override fun onDestroy() {
        instance?.clear()
        instance = null
        super.onDestroy()
    }

    companion object {
        var instance: WeakReference<RemoteShortcutService>? = null
            private set

        fun current(): RemoteShortcutService? = instance?.get()
    }
}
