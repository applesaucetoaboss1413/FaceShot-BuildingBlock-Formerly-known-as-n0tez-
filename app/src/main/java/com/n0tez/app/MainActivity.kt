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
import androidx.appcompat.app.AlertDialog
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
    private var permissionDialogVisible = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        checkPermissions()
    }
    
    private fun setupUI() {
        binding.apply {
            // Card taps
            
            cardFloatingWidget.setOnClickListener {
                if (hasRequiredWidgetPermissions()) {
                    startFloatingWidget()
                } else {
                    shouldStartFloatingWidgetAfterSetup = true
                    maybePromptRequiredSetup()
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

        maybePromptRequiredSetup()
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

    private fun maybePromptRequiredSetup() {
        if (permissionDialogVisible) {
            return
        }

        when {
            !hasOverlayPermission() -> showOverlayPermissionDialog()
            !hasAccessibilityPermission() -> showAccessibilityPermissionDialog()
            shouldStartFloatingWidgetAfterSetup -> {
                shouldStartFloatingWidgetAfterSetup = false
                startFloatingWidget()
            }
        }
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivityForResult(intent, ACCESSIBILITY_PERMISSION_REQUEST_CODE)
    }

    private fun showOverlayPermissionDialog() {
        permissionDialogVisible = true
        AlertDialog.Builder(this)
            .setTitle(R.string.overlay_permission_dialog_title)
            .setMessage(R.string.overlay_permission_dialog_message)
            .setCancelable(false)
            .setPositiveButton(R.string.permission_open_settings) { _, _ ->
                permissionDialogVisible = false
                requestOverlayPermission()
            }
            .setNegativeButton(R.string.permission_not_now) { _, _ ->
                permissionDialogVisible = false
                shouldStartFloatingWidgetAfterSetup = false
            }
            .setOnCancelListener {
                permissionDialogVisible = false
                shouldStartFloatingWidgetAfterSetup = false
            }
            .show()
    }

    private fun showAccessibilityPermissionDialog() {
        permissionDialogVisible = true
        AlertDialog.Builder(this)
            .setTitle(R.string.accessibility_permission_dialog_title)
            .setMessage(R.string.accessibility_permission_dialog_message)
            .setCancelable(false)
            .setPositiveButton(R.string.permission_open_settings) { _, _ ->
                permissionDialogVisible = false
                requestAccessibilityPermission()
            }
            .setNegativeButton(R.string.permission_not_now) { _, _ ->
                permissionDialogVisible = false
                shouldStartFloatingWidgetAfterSetup = false
            }
            .setOnCancelListener {
                permissionDialogVisible = false
                shouldStartFloatingWidgetAfterSetup = false
            }
            .show()
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
                    maybePromptRequiredSetup()
                } else {
                    Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show()
                    shouldStartFloatingWidgetAfterSetup = false
                }
            }
            ACCESSIBILITY_PERMISSION_REQUEST_CODE -> {
                if (hasAccessibilityPermission()) {
                    maybePromptRequiredSetup()
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
