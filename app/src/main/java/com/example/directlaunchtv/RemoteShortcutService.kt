package com.example.directlaunchtv

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

class RemoteShortcutService : AccessibilityService() {
    private var count = 0
    private var firstPressAt = 0L

    override fun onServiceConnected() {
        instance = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
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
