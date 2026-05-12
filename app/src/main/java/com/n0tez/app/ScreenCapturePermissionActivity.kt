package com.n0tez.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.core.content.ContextCompat

class ScreenCapturePermissionActivity : Activity() {
    private var requestStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            requestStarted = savedInstanceState.getBoolean(KEY_REQUEST_STARTED, false)
        }
        if (!requestStarted) {
            requestStarted = true
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_SCREEN_CAPTURE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_REQUEST_STARTED, requestStarted)
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SCREEN_CAPTURE) {
            val serviceIntent = Intent(this, FloatingWidgetService::class.java).apply {
                action = FloatingWidgetService.ACTION_SCREEN_CAPTURE_READY
                putExtra(FloatingWidgetService.EXTRA_MEDIA_PROJECTION_RESULT_CODE, resultCode)
                if (resultCode == RESULT_OK && data != null) {
                    putExtra(FloatingWidgetService.EXTRA_MEDIA_PROJECTION_DATA, data)
                }
            }
            ContextCompat.startForegroundService(this, serviceIntent)
            finish()
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        private const val REQUEST_SCREEN_CAPTURE = 9201
        private const val KEY_REQUEST_STARTED = "request_started"
    }
}
