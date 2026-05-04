package com.n0tez.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.n0tez.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val OVERLAY_PERMISSION_REQUEST_CODE = 100
    private val STORAGE_PERMISSION_REQUEST_CODE = 101
    private val ACCESSIBILITY_PERMISSION_REQUEST_CODE = 102
    private var shouldStartFloatingWidgetAfterSetup = false
    private var hasAutoNavigatedOverlayThisSession = false
    private var hasAutoNavigatedAccessibilityThisSession = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        maybePromptRequiredSetup()
    }
    
    private fun setupUI() {
        binding.apply {
            // Card taps
            
            cardFloatingWidget.setOnClickListener {
                if (hasRequiredWidgetPermissions()) {
                    startFloatingWidget()
                } else {
                    shouldStartFloatingWidgetAfterSetup = true
                    maybePromptRequiredSetup(forceNavigation = true)
                }
            }

            cardSettings.setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }

            cardMyNotes.setOnClickListener {
                startActivity(Intent(this@MainActivity, NotesListActivity::class.java))
            }

            cardMultimedia.setOnClickListener {
                startActivity(Intent(this@MainActivity, MultimediaActivity::class.java))
            }
        }
    }
    
    private fun checkPermissions() {
        // Check storage permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    STORAGE_PERMISSION_REQUEST_CODE
                )
            }
        }

    }
    
    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun hasAccessibilityPermission(): Boolean {
        return TextCaptureAccessibilityService.isEnabled(this)
    }

    private fun hasRequiredWidgetPermissions(): Boolean {
        return hasOverlayPermission() && hasAccessibilityPermission()
    }

    private fun maybePromptRequiredSetup(forceNavigation: Boolean = false) {
        resetPermissionNavigationState()

        when {
            !hasOverlayPermission() && (forceNavigation || !hasAutoNavigatedOverlayThisSession) -> {
                hasAutoNavigatedOverlayThisSession = true
                openOverlayPermissionSettings()
            }
            !hasAccessibilityPermission() && (forceNavigation || !hasAutoNavigatedAccessibilityThisSession) -> {
                hasAutoNavigatedAccessibilityThisSession = true
                openAccessibilityPermissionSettings()
            }
            shouldStartFloatingWidgetAfterSetup -> {
                shouldStartFloatingWidgetAfterSetup = false
                startFloatingWidget()
            }
        }
    }

    private fun resetPermissionNavigationState() {
        if (hasOverlayPermission()) {
            hasAutoNavigatedOverlayThisSession = false
        }
        if (hasAccessibilityPermission()) {
            hasAutoNavigatedAccessibilityThisSession = false
        }
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            if (intent.resolveActivity(packageManager) != null) {
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
            } else {
                Toast.makeText(this, R.string.permission_settings_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, ACCESSIBILITY_PERMISSION_REQUEST_CODE)
        } else {
            openAppDetailsSettings()
        }
    }

    private fun openOverlayPermissionSettings() {
        Toast.makeText(this, R.string.overlay_permission_dialog_message, Toast.LENGTH_LONG).show()
        requestOverlayPermission()
    }

    private fun openAccessibilityPermissionSettings() {
        Toast.makeText(this, R.string.accessibility_permission_dialog_message, Toast.LENGTH_LONG).show()
        requestAccessibilityPermission()
    }

    private fun openAppDetailsSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, R.string.permission_settings_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun startFloatingWidget() {
        try {
            val intent = Intent(this, FloatingWidgetService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "Floating widget started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "startFloatingWidget error", e)
            Toast.makeText(this, "Failed to start widget: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (hasOverlayPermission()) {
                    maybePromptRequiredSetup(forceNavigation = true)
                } else {
                    Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show()
                    shouldStartFloatingWidgetAfterSetup = false
                }
            }
            ACCESSIBILITY_PERMISSION_REQUEST_CODE -> {
                if (hasAccessibilityPermission()) {
                    maybePromptRequiredSetup(forceNavigation = true)
                } else {
                    Toast.makeText(
                        this,
                        R.string.accessibility_permission_denied,
                        Toast.LENGTH_SHORT,
                    ).show()
                    shouldStartFloatingWidgetAfterSetup = false
                }
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                } else {
                    Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
