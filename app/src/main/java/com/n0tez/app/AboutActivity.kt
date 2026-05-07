package com.n0tez.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.n0tez.app.ui.components.ActionCard
import com.n0tez.app.ui.components.AppScreen
import com.n0tez.app.ui.components.HeroPanel
import com.n0tez.app.ui.components.MetricChip
import com.n0tez.app.ui.theme.N0tezTheme

class AboutActivity : AppCompatActivity() {
    private var versionName by mutableStateOf("1.0.0")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            versionName = pInfo.versionName ?: versionName
        } catch (_: Exception) {
        }

        setContent {
            N0tezTheme {
                AboutScreen(
                    versionName = versionName,
                    onBack = ::finish,
                    onVisitSite = {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://faceshot-chopshop-1.onrender.com")
                            )
                        )
                    }
                )
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

private data class AboutHighlight(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
private fun AboutScreen(
    versionName: String,
    onBack: () -> Unit,
    onVisitSite: () -> Unit
) {
    val highlights = listOf(
        AboutHighlight(
            title = "Polished shell",
            description = "The UI is shifting toward a cleaner, Compose-first Material 3 presentation.",
            icon = Icons.Rounded.Star
        ),
        AboutHighlight(
            title = "Private by design",
            description = "PIN protection and local note controls stay easy to access.",
            icon = Icons.Rounded.Shield
        ),
        AboutHighlight(
            title = "Connected tools",
            description = "Notes, media, overlays, and editors now feel part of one suite.",
            icon = Icons.Rounded.Language
        )
    )

    AppScreen(
        title = "About",
        subtitle = "Product details",
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
                    eyebrow = "Identity",
                    title = "FaceShot-BuildingBlock",
                    description = "A utility-focused app combining private notes, overlays, and multimedia editing under a more premium visual language.",
                    metrics = {
                        MetricChip("Version", versionName, Icons.Rounded.Star)
                        MetricChip("Suite", "Multi-tool", Icons.Rounded.RocketLaunch)
                        MetricChip("Privacy", "Local first", Icons.Rounded.Shield)
                    },
                    footer = {
                        Button(onClick = onVisitSite, modifier = Modifier.fillMaxWidth()) {
                            androidx.compose.material3.Text("Visit project site")
                        }
                    }
                )
            }
            items(highlights.size) { index ->
                val item = highlights[index]
                ActionCard(
                    eyebrow = "Detail",
                    title = item.title,
                    description = item.description,
                    icon = item.icon,
                    accent = MaterialTheme.colorScheme.primary,
                    footerText = "Learn more",
                    onClick = onVisitSite
                )
            }
        }
    }
}
