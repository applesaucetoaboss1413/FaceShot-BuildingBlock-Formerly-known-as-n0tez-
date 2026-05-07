package com.n0tez.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.PhotoCameraBack
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoSettings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.n0tez.app.ui.components.ActionCard
import com.n0tez.app.ui.components.AppScreen
import com.n0tez.app.ui.components.GlassPanel
import com.n0tez.app.ui.components.HeroPanel
import com.n0tez.app.ui.components.MetricChip
import com.n0tez.app.ui.components.SectionHeading
import com.n0tez.app.ui.theme.N0tezTheme

class MultimediaActivity : AppCompatActivity() {

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val intent = Intent(this, PhotoEditorActivity::class.java)
            intent.data = uri
            startActivity(intent)
        }
    }

    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val intent = Intent(this, VideoEditorActivity::class.java)
            intent.data = uri
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            N0tezTheme {
                MultimediaScreen(
                    onBack = ::finish,
                    onVoiceRecorder = {
                        startActivity(Intent(this, VoiceRecorderActivity::class.java))
                    },
                    onPhotoEditor = {
                        pickImageLauncher.launch("image/*")
                    },
                    onVideoEditor = {
                        pickVideoLauncher.launch("video/*")
                    },
                    onGallery = {
                        startActivity(Intent(this, MediaGalleryActivity::class.java))
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

private data class StudioTool(
    val eyebrow: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: androidx.compose.ui.graphics.Color,
    val footerText: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultimediaScreen(
    onBack: () -> Unit,
    onVoiceRecorder: () -> Unit,
    onPhotoEditor: () -> Unit,
    onVideoEditor: () -> Unit,
    onGallery: () -> Unit
) {
    val tools = listOf(
        StudioTool(
            eyebrow = "Photo",
            title = "Photo Editor",
            description = "Select an image and open the enhancement, filter, crop, and AI toolchain in a more cinematic workspace.",
            icon = Icons.Rounded.PhotoCameraBack,
            accent = MaterialTheme.colorScheme.primary,
            footerText = "Edit stills",
            onClick = onPhotoEditor
        ),
        StudioTool(
            eyebrow = "Video",
            title = "Video Editor",
            description = "Trim, crop, adjust speed, and export sharper video edits with cleaner control grouping.",
            icon = Icons.Rounded.VideoSettings,
            accent = MaterialTheme.colorScheme.secondary,
            footerText = "Cut motion",
            onClick = onVideoEditor
        ),
        StudioTool(
            eyebrow = "Audio",
            title = "Voice Recorder",
            description = "Capture quick voice notes with a focused recording surface and clearer actions.",
            icon = Icons.Rounded.GraphicEq,
            accent = MaterialTheme.colorScheme.tertiary,
            footerText = "Record audio",
            onClick = onVoiceRecorder
        ),
        StudioTool(
            eyebrow = "Library",
            title = "Media Gallery",
            description = "Browse saved outputs and move quickly between edits, exports, and reusable assets.",
            icon = Icons.Rounded.Collections,
            accent = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
            footerText = "Open library",
            onClick = onGallery
        )
    )

    AppScreen(
        title = "Media Studio",
        subtitle = "Creative tools, simplified",
        navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
        onNavigationClick = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GlassPanel {
                    SectionHeading(
                        eyebrow = "Studio Deck",
                        title = "Create in a darker, sharper media workspace.",
                        description = "The studio now opens as a focused creative bay with dedicated lanes for stills, motion, audio, and asset review."
                    )
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricChip("Visual feel", "Cinematic", Icons.Rounded.MovieFilter)
                        MetricChip("Editing flow", "Focused", Icons.Rounded.Tune)
                        MetricChip("Output level", "High quality", Icons.Rounded.HighQuality)
                    }
                }
            }
            item {
                HeroPanel(
                    eyebrow = "Studio",
                    title = "High-end media controls in one place.",
                    description = "Photo, video, audio, and gallery tools now read like one premium creation suite instead of separate utility pages.",
                    metrics = {
                        MetricChip("Creative lanes", "4 tools", Icons.Rounded.RocketLaunch)
                        MetricChip("Output feel", "Pro grade", Icons.Rounded.HighQuality)
                        MetricChip("Asset flow", "Gallery linked", Icons.Rounded.Collections)
                    }
                )
            }
            items(tools.size) { index ->
                val tool = tools[index]
                ActionCard(
                    eyebrow = tool.eyebrow,
                    title = tool.title,
                    description = tool.description,
                    icon = tool.icon,
                    accent = tool.accent,
                    footerText = tool.footerText,
                    onClick = tool.onClick
                )
            }
        }
    }
}
