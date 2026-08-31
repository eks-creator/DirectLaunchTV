package com.example.directlaunchtv

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

class AppPickerActivity : Activity() {
    data class LaunchableApp(val label: String, val packageName: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Choose app"

        val apps = loadApps()
        val list = ListView(this).apply {
            adapter = AppAdapter(apps)
            dividerHeight = 1
            isFocusable = true
            isFocusableInTouchMode = false
            setItemsCanFocus(false)

            setOnItemClickListener { _, _, position, _ ->
                selectApp(apps, position)
            }

            setOnKeyListener { _, keyCode, event ->
                if (event.action != KeyEvent.ACTION_UP) return@setOnKeyListener false
                if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER &&
                    keyCode != KeyEvent.KEYCODE_ENTER &&
                    keyCode != KeyEvent.KEYCODE_NUMPAD_ENTER
                ) return@setOnKeyListener false

                val position = selectedItemPosition.takeIf { it != ListView.INVALID_POSITION } ?: 0
                if (apps.isNotEmpty()) selectApp(apps, position)
                true
            }

            post {
                if (apps.isNotEmpty()) {
                    setSelection(0)
                    requestFocus()
                }
            }
        }
        setContentView(list)
    }

    private fun selectApp(apps: List<LaunchableApp>, position: Int) {
        if (position !in apps.indices) return
        val app = apps[position]

        Config.prefs(this)
            .edit()
            .putString(Config.KEY_TARGET_PACKAGE, app.packageName)
            .commit()

        setResult(
            RESULT_OK,
            Intent().putExtra(Config.KEY_TARGET_PACKAGE, app.packageName)
        )
        finish()
    }

    private fun loadApps(): List<LaunchableApp> {
        val pm = packageManager
        val found = linkedMapOf<String, LaunchableApp>()

        val queries = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        )

        for (query in queries) {
            for (resolveInfo in pm.queryIntentActivities(query, PackageManager.MATCH_ALL)) {
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg == packageName) continue
                val label = resolveInfo.loadLabel(pm)?.toString()?.ifBlank { pkg } ?: pkg
                found[pkg] = LaunchableApp(label, pkg)
            }
        }

        return found.values.sortedBy { it.label.lowercase() }
    }

    private inner class AppAdapter(private val apps: List<LaunchableApp>) : BaseAdapter() {
        override fun getCount() = apps.size
        override fun getItem(position: Int) = apps[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val app = apps[position]
            val row = (convertView as? LinearLayout) ?: LinearLayout(this@AppPickerActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(36, 20, 36, 20)
                isFocusable = false
                isFocusableInTouchMode = false
                isClickable = false
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

                addView(ImageView(this@AppPickerActivity).apply {
                    id = 1001
                    isFocusable = false
                    isClickable = false
                    layoutParams = LinearLayout.LayoutParams(72, 72).apply { marginEnd = 24 }
                })

                addView(TextView(this@AppPickerActivity).apply {
                    id = 1002
                    textSize = 22f
                    isFocusable = false
                    isClickable = false
                    setTextColor(android.graphics.Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
            }

            val icon = row.findViewById<ImageView>(1001)
            val text = row.findViewById<TextView>(1002)
            runCatching { icon.setImageDrawable(packageManager.getApplicationIcon(app.packageName)) }
            text.text = "${app.label}\n${app.packageName}"
            return row
        }
    }
}
