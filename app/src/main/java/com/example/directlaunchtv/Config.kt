package com.example.directlaunchtv

import android.content.Context
import android.graphics.Color
import android.view.KeyEvent

object Config {
    private const val PREFS = "direct_launch_tv"

    const val KEY_TARGET_PACKAGE = "target_package"
    const val KEY_SPLASH_ENABLED = "splash_enabled"
    const val KEY_SPLASH_DURATION = "splash_duration_ms"
    const val KEY_SPLASH_BG = "splash_bg"
    const val KEY_SPLASH_IMAGE_URI = "splash_image_uri"
    const val KEY_SHORTCUT_KEYCODE = "shortcut_keycode"
    const val KEY_SHORTCUT_PRESS_COUNT = "shortcut_press_count"
    const val KEY_SHORTCUT_WINDOW_MS = "shortcut_window_ms"

    fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun targetPackage(context: Context): String? =
        prefs(context).getString(KEY_TARGET_PACKAGE, null)

    fun splashEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SPLASH_ENABLED, true)

    fun splashDurationMs(context: Context): Long =
        prefs(context).getLong(KEY_SPLASH_DURATION, 1200L).coerceIn(0L, 5000L)

    fun splashBackground(context: Context): Int =
        prefs(context).getInt(KEY_SPLASH_BG, Color.BLACK)

    fun splashImageUri(context: Context): String? =
        prefs(context).getString(KEY_SPLASH_IMAGE_URI, null)

    fun shortcutKeyCode(context: Context): Int =
        prefs(context).getInt(KEY_SHORTCUT_KEYCODE, KeyEvent.KEYCODE_MENU)

    fun shortcutPressCount(context: Context): Int =
        prefs(context).getInt(KEY_SHORTCUT_PRESS_COUNT, 1).coerceIn(1, 5)

    fun shortcutWindowMs(context: Context): Long =
        prefs(context).getLong(KEY_SHORTCUT_WINDOW_MS, 1800L).coerceIn(500L, 5000L)
}
