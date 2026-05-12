package com.n0tez.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FuturisticBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            Color(0xFF071321),
            MaterialTheme.colorScheme.surfaceVariant,
            Color(0xFF03070F)
        )
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        CyberGridOverlay(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        )
        GlowOrb(
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.TopEnd)
                .padding(top = 0.dp, end = 0.dp),
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                Color.Transparent
            )
        )
        GlowOrb(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopStart)
                .padding(top = 140.dp, start = (-24).dp),
            colors = listOf(
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                Color.Transparent
            )
        )
        GlowOrb(
            modifier = Modifier
                .size(420.dp)
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = (-48).dp),
            colors = listOf(
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                Color.Transparent
            )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )
        content()
    }
}

@Composable
private fun CyberGridOverlay(modifier: Modifier = Modifier) {
    val gridAccent = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    Canvas(modifier = modifier) {
        val verticalStep = 36.dp.toPx()
        val horizontalStep = 42.dp.toPx()
        val gridColor = Color.White.copy(alpha = 0.035f)
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, size.height),
                strokeWidth = 1.dp.toPx()
            )
            x += verticalStep
        }
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
            y += horizontalStep
        }
        drawRoundRect(
            color = gridAccent,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.06f, size.height * 0.12f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.88f, size.height * 0.72f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(44.dp.toPx(), 44.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun GlowOrb(
    modifier: Modifier,
    colors: List<Color>
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Brush.radialGradient(colors = colors))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    FuturisticBackground(modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    tonalElevation = 10.dp,
                    shadowElevation = 10.dp
                ) {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                            actionIconContentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (subtitle != null) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            if (navigationIcon != null && onNavigationClick != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                                    tonalElevation = 6.dp
                                ) {
                                    IconButton(onClick = onNavigationClick) {
                                        Icon(
                                            imageVector = navigationIcon,
                                            contentDescription = "Back"
                                        )
                                    }
                                }
                            }
                        },
                        actions = actions
                    )
                }
            }
        ) { innerPadding ->
            content(innerPadding)
        }
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                shape = RoundedCornerShape(30.dp)
            ),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        tonalElevation = 18.dp,
        shadowElevation = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeading(
    eyebrow: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MetricChip(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        tonalElevation = 10.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroPanel(
    eyebrow: String,
    title: String,
    description: String,
    metrics: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null
) {
    GlassPanel(modifier = modifier) {
        SectionHeading(
            eyebrow = eyebrow,
            title = title,
            description = description
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            metrics()
        }
        if (footer != null) {
            Spacer(modifier = Modifier.height(2.dp))
            footer()
        }
    }
}

@Composable
fun ActionCard(
    eyebrow: String,
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    footerText: String = "Open"
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = accent.copy(alpha = 0.16f)
                ) {
                    Icon(
                        modifier = Modifier.padding(14.dp),
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.86f)
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        text = eyebrow.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = accent.copy(alpha = 0.12f)
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    text = footerText,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
            }
        }
    }
}
