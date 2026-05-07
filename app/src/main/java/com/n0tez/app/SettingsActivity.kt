package com.n0tez.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.n0tez.app.ui.components.AppScreen
import com.n0tez.app.ui.components.HeroPanel
import com.n0tez.app.ui.components.MetricChip
import com.n0tez.app.ui.theme.N0tezTheme

class SettingsActivity : AppCompatActivity() {

    private var pinEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinEnabled = PinLockActivity.isPinEnabled(this)
        setContent {
            N0tezTheme {
                SettingsScreen(
                    pinEnabled = pinEnabled,
                    onBack = ::finish,
                    onPinToggle = ::handlePinToggle,
                    onAppearanceClick = { recreate() },
                    onBackupClick = { },
                    onAboutClick = {
                        startActivity(Intent(this, AboutActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        pinEnabled = PinLockActivity.isPinEnabled(this)
    }

    private fun handlePinToggle(enabled: Boolean) {
        if (enabled) {
            startActivity(
                Intent(this, PinLockActivity::class.java).apply {
                    putExtra("SET_PIN", true)
                }
            )
        } else {
            disablePin()
            pinEnabled = false
        }
    }

    private fun disablePin() {
        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                this,
                "pin_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            sharedPreferences.edit()
                .putBoolean("pin_enabled", false)
                .remove("user_pin")
                .apply()
        } catch (_: Exception) {
        }
    }
}

private data class SettingRowModel(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
private fun SettingsScreen(
    pinEnabled: Boolean,
    onBack: () -> Unit,
    onPinToggle: (Boolean) -> Unit,
    onAppearanceClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    AppScreen(
        title = "Settings",
        subtitle = "Refined controls",
        navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
        onNavigationClick = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HeroPanel(
                    eyebrow = "Control",
                    title = "Sharper settings with cleaner grouping.",
                    description = "Security, appearance, and support actions are now easier to scan and feel more intentional.",
                    metrics = {
                        MetricChip("Security", if (pinEnabled) "Enabled" else "Optional", Icons.Rounded.Shield)
                        MetricChip("Theme", "Material 3", Icons.Rounded.Palette)
                        MetricChip("Support", "Built in", Icons.Rounded.Info)
                    }
                )
            }
            item {
                SettingSwitchRow(
                    model = SettingRowModel(
                        title = "PIN Protection",
                        description = "Secure sensitive notes behind a 4-digit access gate.",
                        icon = Icons.Rounded.Lock
                    ),
                    checked = pinEnabled,
                    onCheckedChange = onPinToggle
                )
            }
            item {
                SettingActionRow(
                    model = SettingRowModel(
                        title = "Appearance",
                        description = "Refresh the active look and keep the high-end dark theme applied.",
                        icon = Icons.Rounded.Palette
                    ),
                    onClick = onAppearanceClick
                )
            }
            item {
                SettingActionRow(
                    model = SettingRowModel(
                        title = "Backup & Restore",
                        description = "Reserve space for future sync and migration tools.",
                        icon = Icons.Rounded.Backup
                    ),
                    onClick = onBackupClick
                )
            }
            item {
                SettingActionRow(
                    model = SettingRowModel(
                        title = "About",
                        description = "Open the redesigned about screen and app details.",
                        icon = Icons.Rounded.Info
                    ),
                    onClick = onAboutClick
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    model: SettingRowModel,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = model.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(model.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SettingActionRow(
    model: SettingRowModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = model.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(model.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
