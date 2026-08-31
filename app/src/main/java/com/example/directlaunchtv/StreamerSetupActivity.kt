package com.example.directlaunchtv

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class StreamerSetupActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showStatus()
    }

    override fun onResume() {
        super.onResume()
        if (AccessibilityHelper.hasSecureSettingsPermission(this)) {
            AccessibilityHelper.enableService(this)
        }
    }

    private fun showStatus() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(54, 40, 54, 40)
        }

        root.addView(TextView(this).apply {
            text = "Google TV Streamer Setup"
            textSize = 34f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 20)
        })

        val granted = AccessibilityHelper.hasSecureSettingsPermission(this)
        val enabled = AccessibilityHelper.isServiceEnabled(this)
        root.addView(TextView(this).apply {
            text = if (granted) {
                if (enabled) "DirectLaunch redirect service: ENABLED" else "Secure setup permission is granted. Select Enable below."
            } else {
                "Google TV Streamer blocks Accessibility for sideloaded apps. Run this one-time ADB command from a computer on the same network:\n\nadb shell pm grant com.example.directlaunchtv android.permission.WRITE_SECURE_SETTINGS\n\nThen reopen this screen."
            }
            textSize = 21f
            setTextColor(Color.LTGRAY)
            setLineSpacing(4f, 1.1f)
            setPadding(0, 0, 0, 24)
        })

        root.addView(Button(this).apply {
            text = "Enable DirectLaunch redirect service"
            textSize = 20f
            isAllCaps = false
            isFocusable = true
            gravity = Gravity.CENTER_VERTICAL
            setOnClickListener {
                AccessibilityHelper.enableService(this@StreamerSetupActivity)
                recreate()
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 76))

        root.addView(Button(this).apply {
            text = "Open DirectLaunch Settings"
            textSize = 20f
            isAllCaps = false
            isFocusable = true
            setOnClickListener { startActivity(Intent(this@StreamerSetupActivity, SettingsActivity::class.java)) }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 76).apply { topMargin = 12 })

        root.addView(Button(this).apply {
            text = "Test Google TV escape"
            textSize = 20f
            isAllCaps = false
            isFocusable = true
            setOnClickListener { SystemHome.open(this@StreamerSetupActivity) }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 76).apply { topMargin = 12 })

        setContentView(root)
    }
}
