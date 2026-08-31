package com.example.directlaunchtv

import android.app.Activity
import android.os.Bundle

class EscapeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemHome.open(this, 60000L)
        finish()
    }
}
