package com.n0tez.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.unit.dp
import com.n0tez.app.ui.components.ActionCard
import com.n0tez.app.ui.components.AppScreen
import com.n0tez.app.ui.components.HeroPanel
import com.n0tez.app.ui.components.MetricChip
import com.n0tez.app.ui.theme.N0tezTheme

class MainActivity : AppCompatActivity() {

    private val overlayPermissionRequestCode = 100
    private val notificationPermissionRequestCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        setContent {
            N0tezTheme {
                MainScreen(
                    onLaunchWidget = {
                        if (hasOverlayPermission()) {
                            startFloatingWidget()
                        } else {
                            requestOverlayPermission()
                        }
                    },
                    onOpenNotes = {
                        startActivity(Intent(this, NotesListActivity::class.java))
                    },
                    onOpenStudio = {
                        startActivity(Intent(this, MultimediaActivity::class.java))
                    },
                    onOpenSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                )
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    notificationPermissionRequestCode
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

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivityForResult(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ),
                overlayPermissionRequestCode
            )
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
        if (requestCode == overlayPermissionRequestCode) {
            if (hasOverlayPermission()) {
                startFloatingWidget()
            } else {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == notificationPermissionRequestCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}

private data class HomeFeature(
    val eyebrow: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color,
    val footerText: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainScreen(
    onLaunchWidget: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenStudio: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val features = listOf(
        HomeFeature(
            eyebrow = "Overlay",
            title = "Floating Widget",
            description = "Keep quick capture and transparent note controls floating above any app.",
            icon = Icons.Rounded.Widgets,
            accent = MaterialTheme.colorScheme.primary,
            footerText = "Launch overlay",
            onClick = onLaunchWidget
        ),
        HomeFeature(
            eyebrow = "Notes",
            title = "Notes Vault",
            description = "Write, pin, shred, and manage encrypted personal notes in a calmer workspace.",
            icon = Icons.Rounded.EditNote,
            accent = MaterialTheme.colorScheme.secondary,
            footerText = "Open vault",
            onClick = onOpenNotes
        ),
        HomeFeature(
            eyebrow = "Studio",
            title = "Media Studio",
            description = "Jump into photo, video, audio, and gallery tools with a more cinematic shell.",
            icon = Icons.Rounded.ViewInAr,
            accent = MaterialTheme.colorScheme.tertiary,
            footerText = "Enter studio",
            onClick = onOpenStudio
        ),
        HomeFeature(
            eyebrow = "Control",
            title = "Settings",
            description = "Refine privacy, PIN protection, and the overall app experience from one place.",
            icon = Icons.Rounded.Settings,
            accent = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            footerText = "Adjust system",
            onClick = onOpenSettings
        )
    )

    AppScreen(title = "FaceShot", subtitle = "Director dashboard") { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                HeroPanel(
                    eyebrow = "Command",
                    title = "A sharper control center for the full FaceShot suite.",
                    description = "The new shell leans into premium depth, cleaner spacing, and quicker access so the product feels closer to a polished pro tool than a utility list.",
                    metrics = {
                        MetricChip("Core hubs", "4 modes", Icons.Rounded.RocketLaunch)
                        MetricChip("Security", "PIN ready", Icons.Rounded.Lock)
                        MetricChip("Visual system", "Material 3", Icons.Rounded.AutoAwesome)
                    }
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    features.forEach { feature ->
                        ActionCard(
                            eyebrow = feature.eyebrow,
                            title = feature.title,
                            description = feature.description,
                            icon = feature.icon,
                            accent = feature.accent,
                            footerText = feature.footerText,
                            onClick = feature.onClick
                        )
                    }
                }
            }
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricChip("Experience", "Futuristic", Icons.Rounded.AutoAwesome)
                    MetricChip("Access", "One tap", Icons.Rounded.Widgets)
                    MetricChip("Tools", "Notes + media", Icons.Rounded.ViewInAr)
                }
            }
        }
    }
}
