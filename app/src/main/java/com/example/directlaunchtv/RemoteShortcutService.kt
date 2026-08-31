package com.example.directlaunchtv

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

class RemoteShortcutService : AccessibilityService() {
    private var count = 0
    private var firstPressAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()

        val info = serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info

        instance = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val expectedKey = Config.shortcutKeyCode(this)
        if (event.keyCode != expectedKey) {
            if (event.action == KeyEvent.ACTION_UP) resetCounter()
            return false
        }

        // Consume the configured shortcut key so the foreground app does not also act on it.
        if (event.action != KeyEvent.ACTION_UP) return true

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

        return true
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
