package com.example.directlaunchtv

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var launchRunnable: Runnable? = null
    private var routing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        route()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        route()
    }

    private fun route() {
        if (routing) return
        routing = true

        val targetPackage = Config.targetPackage(this)
        if (targetPackage.isNullOrBlank()) {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
            return
        }

        val targetIntent = packageManager.getLeanbackLaunchIntentForPackage(targetPackage)
            ?: packageManager.getLaunchIntentForPackage(targetPackage)

        if (targetIntent == null) {
            Config.prefs(this).edit().remove(Config.KEY_TARGET_PACKAGE).apply()
            startActivity(Intent(this, SettingsActivity::class.java).apply {
                putExtra("error", "The selected app is no longer installed or launchable.")
            })
            finish()
            return
        }

        if (Config.splashEnabled(this)) {
            showConfiguredSplash()
        } else {
            setContentView(FrameLayout(this).apply { setBackgroundColor(Color.BLACK) })
        }

        val delay = if (Config.splashEnabled(this)) Config.splashDurationMs(this) else 0L
        launchRunnable = Runnable {
            targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            runCatching { startActivity(targetIntent) }
                .onFailure {
                    startActivity(Intent(this, SettingsActivity::class.java).apply {
                        putExtra("error", "Couldn't launch the selected app.")
                    })
                }
            finish()
        }.also { handler.postDelayed(it, delay) }
    }

    private fun showConfiguredSplash() {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Config.splashBackground(this@MainActivity))
            isFocusable = true
            isFocusableInTouchMode = true
        }

        val uriString = Config.splashImageUri(this)
        if (!uriString.isNullOrBlank()) {
            val image = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                runCatching { setImageURI(Uri.parse(uriString)) }
            }
            root.addView(image, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        } else {
            val label = TextView(this).apply {
                text = "DirectLaunch TV"
                textSize = 42f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            root.addView(label, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }

        setContentView(root)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP && event.keyCode == KeyEvent.KEYCODE_BACK) {
            launchRunnable?.let { handler.removeCallbacks(it) }
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        launchRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }
}
