package com.n0tez.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.n0tez.app.ui.compose.FaceshotTheme
import com.n0tez.app.ui.compose.FeatureRow
import com.n0tez.app.ui.compose.FuturisticScreen
import com.n0tez.app.ui.compose.GlassPanel
import com.n0tez.app.ui.compose.HeroCard
import com.n0tez.app.ui.compose.MetricCard
import com.n0tez.app.ui.compose.SectionHeader
import com.n0tez.app.ui.compose.SettingToggleRow
import com.n0tez.app.ui.compose.StatusPillRow

class SettingsActivity : AppCompatActivity() {

    private var pinEnabled by mutableStateOf(false)
    private var transparencyLevel by mutableFloatStateOf(70f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshSettingState()
        setContent {
            FaceshotTheme {
                SettingsScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSettingState()
    }

    private fun refreshSettingState() {
        pinEnabled = PinLockActivity.isPinEnabled(this)
        transparencyLevel = getSharedPreferences("n0tez_prefs", MODE_PRIVATE)
            .getInt("transparency_level", 70)
            .toFloat()
    }

    @Composable
    private fun SettingsScreen() {
        val rows = listOf(
            Triple(
                getString(R.string.settings_privacy_title),
                getString(R.string.settings_privacy_summary),
                Icons.Outlined.Security,
            ),
            Triple(
                getString(R.string.settings_customize_title),
                getString(R.string.settings_customize_summary),
                Icons.Outlined.Tune,
            ),
            Triple(
                getString(R.string.privacy_policy),
                getString(R.string.privacy_policy_description),
                Icons.Outlined.Description,
            ),
        )

        FuturisticScreen(
            title = getString(R.string.settings),
            onBack = { finish() },
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
                        eyebrow = "Settings",
                        title = "A cleaner control center for security, transparency, and trust.",
                        description = "This section now looks like part of the premium product, while keeping the important preferences close and easy to understand.",
                    )
                }
                item {
                    StatusPillRow(
                        statuses = listOf(
                            "PIN Active" to pinEnabled,
                            "Overlay ${transparencyLevel.toInt()}%" to true,
                            "Privacy Ready" to true,
                        ),
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        MetricCard(
                            label = "Security",
                            value = if (pinEnabled) "Enabled" else "Open",
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        MetricCard(
                            label = "Default Overlay",
                            value = "${transparencyLevel.toInt()}%",
                            accent = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    SettingToggleRow(
                        title = getString(R.string.enable_pin),
                        subtitle = getString(R.string.settings_pin_summary),
                        checked = pinEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                startActivity(Intent(this@SettingsActivity, PinLockActivity::class.java).apply {
                                    putExtra("SET_PIN", true)
                                })
                            } else {
                                disablePin()
                                refreshSettingState()
                            }
                        },
                    )
                }
                item {
                    GlassPanel {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            SectionHeader(
                                title = getString(R.string.transparency),
                                subtitle = getString(R.string.settings_transparency_summary),
                            )
                            Text(
                                text = getString(R.string.settings_overlay_level, transparencyLevel.toInt()),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Slider(
                                value = transparencyLevel,
                                onValueChange = {
                                    transparencyLevel = it
                                    saveTransparencyPreference(it.toInt())
                                },
                                valueRange = 35f..100f,
                            )
                        }
                    }
                }
                item {
                    SectionHeader(
                        title = "Resources",
                        subtitle = "Quick access to policy and app information without leaving the polished shell.",
                    )
                }
                items(rows) { (title, subtitle, icon) ->
                    FeatureRow(
                        title = title,
                        subtitle = subtitle,
                        icon = icon,
                        accent = MaterialTheme.colorScheme.primary,
                        onClick = {
                            when (title) {
                                getString(R.string.privacy_policy) -> openExternalUrl(
                                    getString(R.string.privacy_policy_url),
                                    R.string.privacy_policy_unavailable,
                                )

                                getString(R.string.settings_privacy_title) -> openExternalUrl(
                                    getString(R.string.privacy_policy_url),
                                    R.string.privacy_policy_unavailable,
                                )

                                getString(R.string.settings_customize_title) -> Unit
                            }
                        },
                    )
                }
            }
        }
    }

    private fun saveTransparencyPreference(level: Int) {
        getSharedPreferences("n0tez_prefs", MODE_PRIVATE)
            .edit()
            .putInt("transparency_level", level)
            .apply()
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
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )

            sharedPreferences.edit()
                .putBoolean("pin_enabled", false)
                .remove("user_pin")
                .apply()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.settings_pin_disable_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openExternalUrl(url: String, failureMessageResId: Int) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, failureMessageResId, Toast.LENGTH_SHORT).show()
        }
    }
}
