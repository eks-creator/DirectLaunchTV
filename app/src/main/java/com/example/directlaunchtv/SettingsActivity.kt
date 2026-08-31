package com.example.directlaunchtv

import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var targetText: TextView
    private lateinit var shortcutText: TextView
    private lateinit var splashDurationText: TextView
    private lateinit var shortcutCountText: TextView
    private lateinit var splashColorButton: Button

    private val splashColors = intArrayOf(
        Color.BLACK,
        Color.rgb(16, 17, 20),
        Color.rgb(18, 35, 64),
        Color.rgb(45, 15, 62),
        Color.WHITE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        intent.getStringExtra("error")?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
    }

    override fun onResume() {
        super.onResume()
        if (::targetText.isInitialized) refreshSummary()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(54, 36, 54, 72)
        }

        root.addView(title("DirectLaunch TV"))
        root.addView(body("Choose the app your TV should jump into. Once DirectLaunch is selected as the Home app, pressing Home routes back to that chosen app."))

        section("Target app")
        targetText = body("")
        root.addView(targetText)
        addButton("Choose target app") { startActivityForResult(Intent(this, AppPickerActivity::class.java), REQ_PICK_APP) }
        addButton("Launch chosen app now") { launchChosenApp() }
        addButton("Make DirectLaunch the default Home app") { requestHomeRole() }

        section("Google TV / Android TV escape hatch")
        root.addView(body("The remote shortcut is global only after you enable the DirectLaunch accessibility service. Choose a key your remote actually sends. MENU, INFO, GUIDE, or a colored key are usually better choices than Home/Power."))
        shortcutText = body("")
        root.addView(shortcutText)
        addButton("Capture shortcut key") { captureShortcutKey() }

        shortcutCountText = body("")
        root.addView(shortcutCountText)
        val pressCount = SeekBar(this).apply {
            max = 4
            progress = Config.shortcutPressCount(this@SettingsActivity) - 1
            isFocusable = true
            setOnSeekBarChangeListener(simpleSeekListener { progress ->
                val count = progress + 1
                Config.prefs(this@SettingsActivity).edit().putInt(Config.KEY_SHORTCUT_PRESS_COUNT, count).apply()
                shortcutCountText.text = "Shortcut presses required: $count"
            })
        }
        root.addView(pressCount, matchWidth())

        addButton("Enable / configure remote shortcut service") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        addButton("Open original system Home now") {
            if (!SystemHome.open(this)) toast("Couldn't locate another Home app on this device.")
        }

        section("Splash screen")
        val splashEnabled = CheckBox(this).apply {
            text = "Show custom splash before target app"
            textSize = 20f
            setTextColor(Color.WHITE)
            isChecked = Config.splashEnabled(this@SettingsActivity)
            isFocusable = true
            setOnCheckedChangeListener { _, checked ->
                Config.prefs(this@SettingsActivity).edit().putBoolean(Config.KEY_SPLASH_ENABLED, checked).apply()
            }
        }
        root.addView(splashEnabled, matchWidth())

        addButton("Choose splash image") { chooseSplashImage() }
        addButton("Clear splash image") {
            Config.prefs(this).edit().remove(Config.KEY_SPLASH_IMAGE_URI).apply()
            toast("Splash image cleared.")
        }

        splashDurationText = body("")
        root.addView(splashDurationText)
        val durationSeek = SeekBar(this).apply {
            max = 20
            progress = (Config.splashDurationMs(this@SettingsActivity) / 250L).toInt().coerceIn(0, 20)
            isFocusable = true
            setOnSeekBarChangeListener(simpleSeekListener { progress ->
                val duration = progress * 250L
                Config.prefs(this@SettingsActivity).edit().putLong(Config.KEY_SPLASH_DURATION, duration).apply()
                splashDurationText.text = "Splash duration: ${duration / 1000.0} seconds"
            })
        }
        root.addView(durationSeek, matchWidth())

        splashColorButton = addButton("") { cycleSplashColor() }

        section("Power & system")
        root.addView(body("Android does not let a normal app silently power off or reboot the TV. These controls use supported system actions instead."))
        addButton("Show TV power menu") {
            val service = RemoteShortcutService.current()
            if (service == null || !service.showPowerDialog()) toast("Enable the DirectLaunch accessibility service first.")
        }
        addButton("Lock / sleep screen") {
            val service = RemoteShortcutService.current()
            if (service == null || !service.lockScreen()) toast("Enable the DirectLaunch accessibility service first.")
        }
        addButton("Open Android display / power settings") {
            val opened = runCatching { startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)); true }.getOrDefault(false)
            if (!opened) startActivity(Intent(Settings.ACTION_SETTINGS))
        }

        section("Safety access")
        root.addView(body("If you ever lose the global shortcut, press BACK while the DirectLaunch splash is visible to open this settings screen. The settings app also remains visible in the original Google TV app list."))

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }
        setContentView(scroll)
        refreshSummary()
    }

    private fun refreshSummary() {
        val targetPackage = Config.targetPackage(this)
        targetText.text = if (targetPackage.isNullOrBlank()) {
            "Selected app: none"
        } else {
            val label = runCatching {
                val info = packageManager.getApplicationInfo(targetPackage, 0)
                packageManager.getApplicationLabel(info).toString()
            }.getOrDefault(targetPackage)
            "Selected app: $label\n$targetPackage"
        }

        val keyCode = Config.shortcutKeyCode(this)
        shortcutText.text = "Shortcut key: ${KeyEvent.keyCodeToString(keyCode)} (code $keyCode)"
        shortcutCountText.text = "Shortcut presses required: ${Config.shortcutPressCount(this)}"
        splashDurationText.text = "Splash duration: ${Config.splashDurationMs(this) / 1000.0} seconds"
        splashColorButton.text = "Splash background: ${colorName(Config.splashBackground(this))} — change"
    }

    private fun requestHomeRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) toast("DirectLaunch is already the default Home app.")
                else startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME), REQ_HOME_ROLE)
                return
            }
        }
        runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
            .onFailure { toast("Open Settings and choose DirectLaunch as the Home/launcher app.") }
    }

    private fun launchChosenApp() {
        val pkg = Config.targetPackage(this)
        if (pkg.isNullOrBlank()) { toast("Choose a target app first."); return }
        val intent = packageManager.getLeanbackLaunchIntentForPackage(pkg) ?: packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) { toast("That app can't be launched."); return }
        startActivity(intent)
    }

    private fun chooseSplashImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQ_SPLASH_IMAGE)
    }

    private fun captureShortcutKey() {
        val startedAt = SystemClock.elapsedRealtime()
        val message = TextView(this).apply {
            text = "Press the remote button you want to use to open the original Google TV / Android TV Home screen.\n\nInput is armed after a short delay so the OK press that opened this dialog isn't captured."
            textSize = 22f
            setTextColor(Color.WHITE)
            setPadding(36, 36, 36, 36)
            isFocusableInTouchMode = true
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Capture remote key")
            .setView(message)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_UP) return@setOnKeyListener false
            if (SystemClock.elapsedRealtime() - startedAt < 600L) return@setOnKeyListener true
            Config.prefs(this).edit().putInt(Config.KEY_SHORTCUT_KEYCODE, keyCode).apply()
            dialog.dismiss()
            refreshSummary()
            toast("Shortcut set to ${KeyEvent.keyCodeToString(keyCode)}")
            true
        }
        dialog.show()
    }

    private fun cycleSplashColor() {
        val current = Config.splashBackground(this)
        val index = splashColors.indexOf(current).let { if (it < 0) 0 else it }
        val next = splashColors[(index + 1) % splashColors.size]
        Config.prefs(this).edit().putInt(Config.KEY_SPLASH_BG, next).apply()
        refreshSummary()
    }

    @Deprecated("Deprecated in Android SDK; retained for broad TV compatibility in this starter.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SPLASH_IMAGE && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            runCatching { contentResolver.takePersistableUriPermission(uri, flags) }
            Config.prefs(this).edit().putString(Config.KEY_SPLASH_IMAGE_URI, uri.toString()).apply()
            toast("Splash image saved.")
        }
        refreshSummary()
    }

    private fun section(text: String) {
        val view = TextView(this).apply {
            this.text = text
            textSize = 27f
            setTextColor(Color.WHITE)
            setPadding(0, 34, 0, 10)
        }
        root.addView(view, matchWidth())
    }

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        textSize = 36f
        setTextColor(Color.WHITE)
        setPadding(0, 0, 0, 12)
    }

    private fun body(text: String) = TextView(this).apply {
        this.text = text
        textSize = 19f
        setTextColor(Color.LTGRAY)
        setLineSpacing(2f, 1.1f)
        setPadding(0, 4, 0, 12)
    }

    private fun addButton(text: String, action: () -> Unit): Button {
        val button = Button(this).apply {
            this.text = text
            textSize = 20f
            isAllCaps = false
            isFocusable = true
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setPadding(30, 12, 30, 12)
            setOnClickListener { action() }
        }
        root.addView(button, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 72).apply {
            topMargin = 8
            bottomMargin = 8
        })
        return button
    }

    private fun matchWidth() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun simpleSeekListener(onChanged: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) onChanged(progress) }
        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    }

    private fun colorName(color: Int): String = when (color) {
        Color.BLACK -> "Black"
        Color.WHITE -> "White"
        Color.rgb(16, 17, 20) -> "Charcoal"
        Color.rgb(18, 35, 64) -> "Navy"
        Color.rgb(45, 15, 62) -> "Deep purple"
        else -> "Custom"
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    companion object {
        private const val REQ_PICK_APP = 100
        private const val REQ_HOME_ROLE = 101
        private const val REQ_SPLASH_IMAGE = 102
    }
}
