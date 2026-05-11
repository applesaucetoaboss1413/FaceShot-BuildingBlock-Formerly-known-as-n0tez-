package com.n0tez.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.n0tez.app.ui.compose.FaceshotTheme
import com.n0tez.app.ui.compose.FeatureRow
import com.n0tez.app.ui.compose.FuturisticScreen
import com.n0tez.app.ui.compose.HeroCard
import com.n0tez.app.ui.compose.MetricCard
import com.n0tez.app.ui.compose.PrimaryButton
import com.n0tez.app.ui.compose.SectionHeader
import com.n0tez.app.ui.compose.SecondaryButton

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FaceshotTheme {
                AboutScreen()
            }
        }
    }

    @Composable
    private fun AboutScreen() {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }

        val rows = listOf(
            Triple(
                getString(R.string.about_product_title),
                getString(R.string.about_product_summary),
                Icons.Outlined.VerifiedUser,
            ),
            Triple(
                getString(R.string.about_site_title),
                getString(R.string.about_site_summary),
                Icons.Outlined.Language,
            ),
            Triple(
                getString(R.string.privacy_policy),
                getString(R.string.privacy_policy_description),
                Icons.Outlined.Description,
            ),
        )

        FuturisticScreen(
            title = getString(R.string.about),
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
                        eyebrow = "About",
                        title = "A cleaner brand presentation that matches the upgraded product shell.",
                        description = "This screen now looks like part of the same premium experience, with trust signals, version clarity, and cleaner external actions.",
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        MetricCard(
                            label = "Version",
                            value = versionName,
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        MetricCard(
                            label = "Profile",
                            value = "Live",
                            accent = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    SectionHeader(
                        title = "Actions",
                        subtitle = "Open the public web presence or review the privacy page from the same visual system.",
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PrimaryButton(
                            text = getString(R.string.visit_chopshop),
                            onClick = { openExternalUrl(getString(R.string.app_site_url)) },
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            text = getString(R.string.privacy_policy),
                            onClick = {
                                openExternalUrl(
                                    getString(R.string.privacy_policy_url),
                                    R.string.privacy_policy_unavailable,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    SectionHeader(
                        title = "Highlights",
                        subtitle = "Important information is clearer, faster to scan, and aligned with the rest of the app.",
                    )
                }
                items(rows) { (title, subtitle, icon) ->
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

    private fun openExternalUrl(url: String, failureMessageResId: Int? = null) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            failureMessageResId?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
