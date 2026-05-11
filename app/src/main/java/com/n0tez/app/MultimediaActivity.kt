package com.n0tez.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.n0tez.app.ui.compose.ActionGrid
import com.n0tez.app.ui.compose.DashboardAction
import com.n0tez.app.ui.compose.FaceshotTheme
import com.n0tez.app.ui.compose.FeatureRow
import com.n0tez.app.ui.compose.FuturisticScreen
import com.n0tez.app.ui.compose.HeroCard
import com.n0tez.app.ui.compose.MetricCard
import com.n0tez.app.ui.compose.SectionHeader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.VideoCameraBack
import androidx.compose.material.icons.outlined.Waves

class MultimediaActivity : AppCompatActivity() {

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            startActivity(Intent(this, PhotoEditorActivity::class.java).apply { data = uri })
        }
    }

    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            startActivity(Intent(this, VideoEditorActivity::class.java).apply { data = uri })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FaceshotTheme {
                MultimediaScreen()
            }
        }
    }

    @Composable
    private fun MultimediaScreen() {
        val actions = listOf(
            DashboardAction(
                title = "Voice Recorder",
                subtitle = "Capture spoken notes and rapid ideas with less visual noise.",
                iconRes = R.drawable.ic_mic,
                accent = MaterialTheme.colorScheme.primary,
                onClick = { startActivity(Intent(this, VoiceRecorderActivity::class.java)) },
            ),
            DashboardAction(
                title = "Photo Editor",
                subtitle = "Pick an image and open the enhanced editing workflow instantly.",
                iconRes = R.drawable.ic_crop,
                accent = MaterialTheme.colorScheme.secondary,
                onClick = { pickImageLauncher.launch("image/*") },
            ),
            DashboardAction(
                title = "Video Editor",
                subtitle = "Choose a clip and move directly into the video toolchain.",
                iconRes = R.drawable.ic_play,
                accent = MaterialTheme.colorScheme.tertiary,
                onClick = { pickVideoLauncher.launch("video/*") },
            ),
            DashboardAction(
                title = "Media Gallery",
                subtitle = "Review saved assets in a cleaner media access point.",
                iconRes = R.drawable.ic_home,
                accent = Color(0xFFFFB86C),
                onClick = { startActivity(Intent(this, MediaGalleryActivity::class.java)) },
            ),
        )

        val strengths = listOf(
            Triple("Photo pipeline", "Cleaner visual entry into image adjustments and exports.", Icons.Outlined.PhotoCamera),
            Triple("Video pipeline", "Faster access to trim, preview, and publishing flows.", Icons.Outlined.VideoCameraBack),
            Triple("Audio workflow", "Recording stays one tap away from the main creative shell.", Icons.Outlined.Waves),
        )

        FuturisticScreen(
            title = getString(R.string.multimedia_title),
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
                        eyebrow = "Studio",
                        title = "One visual shell for photo, video, voice, and gallery workflows.",
                        description = "The media section now feels like a deliberate premium workspace instead of a list of disconnected entry points.",
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        MetricCard(
                            label = "Tools",
                            value = actions.size.toString(),
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        MetricCard(
                            label = "Mode",
                            value = "Create",
                            accent = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    SectionHeader(
                        title = "Creative Modules",
                        subtitle = "Choose the exact production tool you need without hunting through old cards.",
                    )
                }
                item {
                    ActionGrid(actions = actions)
                }
                item {
                    SectionHeader(
                        title = "Studio Advantages",
                        subtitle = "These upgrades make the product feel more credible and easier to navigate.",
                    )
                }
                items(strengths) { (title, subtitle, icon) ->
                    FeatureRow(
                        title = title,
                        subtitle = subtitle,
                        icon = icon,
                        accent = MaterialTheme.colorScheme.secondary,
                        onClick = {},
                    )
                }
            }
        }
    }
}
