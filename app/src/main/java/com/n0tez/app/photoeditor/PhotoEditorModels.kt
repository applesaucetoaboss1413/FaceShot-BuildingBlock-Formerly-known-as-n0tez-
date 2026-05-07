package com.n0tez.app.photoeditor

import android.graphics.Color
import android.net.Uri

data class PhotoEditorState(
    val adjustments: Adjustments = Adjustments(),
    val filter: FilterPreset = FilterPreset.None,
    val lut: LutFilter? = null,
    val overlays: List<OverlayElement> = emptyList(),
    val strokes: List<Stroke> = emptyList(),
)

data class Adjustments(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    val sharpen: Float = 0f,
    val blur: Float = 0f,
)

sealed interface FilterPreset {
    data object None : FilterPreset
    data object Vintage : FilterPreset
    data object Warm : FilterPreset
    data object Cool : FilterPreset
    data object BlackAndWhite : FilterPreset
    data object Sepia : FilterPreset
}

data class LutFilter(
    val uri: Uri,
    val intensity: Float = 1f,
)

sealed interface OverlayElement {
    val id: String
    val centerX: Float
    val centerY: Float
    val rotationDegrees: Float
    val scale: Float
    val alpha: Float
}

data class TextOverlay(
    override val id: String,
    override val centerX: Float,
    override val centerY: Float,
    override val rotationDegrees: Float = 0f,
    override val scale: Float = 1f,
    override val alpha: Float = 1f,
    val text: String,
    val color: Int = Color.WHITE,
    val textSizePx: Float,
    val typefaceName: String? = null,
) : OverlayElement

data class StickerOverlay(
    override val id: String,
    override val centerX: Float,
    override val centerY: Float,
    override val rotationDegrees: Float = 0f,
    override val scale: Float = 1f,
    override val alpha: Float = 1f,
    val uri: Uri,
) : OverlayElement

data class Stroke(
    val id: String,
    val color: Int,
    val widthPx: Float,
    val points: List<PointF>,
    val alpha: Float = 1f,
)

data class PointF(
    val x: Float,
    val y: Float,
)
