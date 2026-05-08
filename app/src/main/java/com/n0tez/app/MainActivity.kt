package com.n0tez.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.n0tez.app.ui.compose.ActionGrid
import com.n0tez.app.ui.compose.DashboardAction
import com.n0tez.app.ui.compose.FaceshotTheme
import com.n0tez.app.ui.compose.FeatureRow
import com.n0tez.app.ui.compose.FuturisticScreen
import com.n0tez.app.ui.compose.HeroCard
import com.n0tez.app.ui.compose.InfoStrip
import com.n0tez.app.ui.compose.MetricCard
import com.n0tez.app.ui.compose.SectionHeader
import com.n0tez.app.ui.compose.StatusPillRow
import com.n0tez.app.ui.compose.TopAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Widgets

class MainActivity : AppCompatActivity() {

    private val overlayPermissionRequestCode = 100
    private val notificationPermissionRequestCode = 101
    private val accessibilityPermissionRequestCode = 102

    private var shouldStartFloatingWidgetAfterSetup = false
    private var hasAutoNavigatedOverlayThisSession = false
    private var hasAutoNavigatedAccessibilityThisSession = false
    private var overlayPermissionGranted by mutableStateOf(false)
    private var accessibilityPermissionGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshPermissionState()
        setContent {
            FaceshotTheme {
                MainDashboard()
            }
        }
        checkNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
        if (shouldStartFloatingWidgetAfterSetup) {
            maybePromptRequiredSetup(forceNavigation = true)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                notificationPermissionRequestCode,
            )
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
        return try {
            TextCaptureAccessibilityService.isEnabled(this)
        } catch (error: Exception) {
            Log.e("MainActivity", "Failed to resolve accessibility permission state", error)
            false
        }
    }

    private fun hasRequiredWidgetPermissions(): Boolean = hasOverlayPermission() && hasAccessibilityPermission()

    private fun refreshPermissionState() {
        overlayPermissionGranted = try {
            hasOverlayPermission()
        } catch (error: Exception) {
            Log.e("MainActivity", "Failed to resolve overlay permission state", error)
            false
        }
        accessibilityPermissionGranted = hasAccessibilityPermission()
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"),
        )
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, overlayPermissionRequestCode)
        } else {
            Toast.makeText(this, R.string.permission_settings_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, accessibilityPermissionRequestCode)
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
            Toast.makeText(this, R.string.widget_started, Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            Log.e("MainActivity", "Failed to start floating widget", error)
            Toast.makeText(
                this,
                getString(R.string.widget_start_failed, error.message ?: getString(R.string.unknown_error)),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    @Composable
    private fun MainDashboard() {
        val actions = listOf(
            DashboardAction(
                title = "Notes Hub",
                subtitle = "Capture, sort, and revisit secure notes with a cleaner command view.",
                iconRes = R.drawable.ic_note,
                accent = MaterialTheme.colorScheme.primary,
                onClick = { startActivity(Intent(this, NotesListActivity::class.java)) },
            ),
            DashboardAction(
                title = "Media Studio",
                subtitle = "Launch photo, video, voice, and gallery tools from one premium shell.",
                iconRes = R.drawable.ic_widget,
                accent = MaterialTheme.colorScheme.secondary,
                onClick = { startActivity(Intent(this, MultimediaActivity::class.java)) },
            ),
            DashboardAction(
                title = "Floating Note",
                subtitle = "Run the overlay workflow with text capture and instant note access.",
                iconRes = R.drawable.ic_add,
                accent = MaterialTheme.colorScheme.tertiary,
                onClick = {
                    if (hasRequiredWidgetPermissions()) {
                        startFloatingWidget()
                    } else {
                        shouldStartFloatingWidgetAfterSetup = true
                        maybePromptRequiredSetup(forceNavigation = true)
                    }
                },
            ),
            DashboardAction(
                title = "Control Center",
                subtitle = "Tune privacy, startup behavior, and app preferences with better clarity.",
                iconRes = R.drawable.ic_settings,
                accent = Color(0xFFFFB86C),
                onClick = { startActivity(Intent(this, SettingsActivity::class.java)) },
            ),
        )

        val workflowRows = listOf(
            Triple("Visual refresh", "Sharper cards, richer contrast, and clearer action hierarchy.", Icons.Outlined.AutoAwesome),
            Triple("Secure by default", "PIN protection and privacy tools stay close to the surface.", Icons.Outlined.Security),
            Triple("Overlay ready", "Widget launch only needs setup completed once.", Icons.Outlined.Widgets),
        )

        val permissionPrompt = if (overlayPermissionGranted && accessibilityPermissionGranted) {
            getString(R.string.dashboard_permissions_ready)
        } else {
            getString(R.string.dashboard_permissions_missing)
        }

        FuturisticScreen(
            title = getString(R.string.app_name),
            actions = listOf(
                TopAction(
                    label = getString(R.string.about),
                    onClick = { startActivity(Intent(this, AboutActivity::class.java)) },
                ),
            ),
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    HeroCard(
                        eyebrow = "Command Center",
                        title = "A polished creative workspace for notes, overlays, and media tools.",
                        description = "The entire front-end now leans into a cleaner futuristic dashboard with stronger hierarchy, better spacing, and high-confidence actions.",
                    )
                }
                item {
                    StatusPillRow(
                        statuses = listOf(
                            "Overlay Ready" to overlayPermissionGranted,
                            "Text Capture Ready" to accessibilityPermissionGranted,
                            "Launch Ready" to hasRequiredWidgetPermissions(),
                        ),
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        MetricCard(
                            label = "Modules",
                            value = actions.size.toString(),
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        MetricCard(
                            label = "Widget",
                            value = if (hasRequiredWidgetPermissions()) "Armed" else "Setup",
                            accent = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    SectionHeader(
                        title = "Launch Pads",
                        subtitle = "Jump directly into the app surfaces customers feel first.",
                    )
                }
                item {
                    ActionGrid(actions = actions)
                }
                item {
                    InfoStrip(text = permissionPrompt)
                }
                item {
                    SectionHeader(
                        title = "Why It Feels Better",
                        subtitle = "The refreshed shell makes the product look intentional, premium, and easier to trust.",
                    )
                }
                items(workflowRows) { (title, subtitle, icon) ->
                    FeatureRow(
                        title = title,
                        subtitle = subtitle,
                        icon = icon,
                        accent = MaterialTheme.colorScheme.primary,
                        onClick = {},
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            overlayPermissionRequestCode -> {
                if (hasOverlayPermission()) {
                    refreshPermissionState()
                    maybePromptRequiredSetup(forceNavigation = true)
                } else {
                    Toast.makeText(this, R.string.overlay_permission_required, Toast.LENGTH_SHORT).show()
                    shouldStartFloatingWidgetAfterSetup = false
                }
            }

            accessibilityPermissionRequestCode -> {
                if (hasAccessibilityPermission()) {
                    refreshPermissionState()
                    maybePromptRequiredSetup(forceNavigation = true)
                } else {
                    Toast.makeText(this, R.string.accessibility_permission_denied, Toast.LENGTH_SHORT).show()
                    shouldStartFloatingWidgetAfterSetup = false
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == notificationPermissionRequestCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            refreshPermissionState()
        }
    }
}
