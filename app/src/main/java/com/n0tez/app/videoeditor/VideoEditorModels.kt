package com.n0tez.app.videoeditor

import android.graphics.Color
import java.io.File
import java.util.UUID

data class VideoTimeline(
    val videoTracks: MutableList<VideoTrack> = mutableListOf(),
    val audioTracks: MutableList<AudioTrack> = mutableListOf(),
    val textOverlays: MutableList<TextOverlay> = mutableListOf(),
)

data class VideoTrack(
    val id: String = UUID.randomUUID().toString(),
    val clips: MutableList<VideoClip> = mutableListOf(),
    val transitions: MutableList<VideoTransition> = mutableListOf(),
)

data class AudioTrack(
    val id: String = UUID.randomUUID().toString(),
    val clips: MutableList<AudioClip> = mutableListOf(),
)

data class VideoClip(
    val id: String = UUID.randomUUID().toString(),
    val sourcePath: String,
    val startMs: Long = 0L,
    val endMs: Long? = null,
    val startAtMs: Long = 0L,
    val speed: Float = 1f,
    val crop: CropSpec? = null,
    val rotationDegrees: Int = 0,
    val filter: VideoFilter = VideoFilter.None,
    val effects: List<VideoEffect> = emptyList(),
    val includeAudio: Boolean = true,
    val volume: Float = 1f,
)

data class AudioClip(
    val id: String = UUID.randomUUID().toString(),
    val sourcePath: String,
    val startMs: Long = 0L,
    val endMs: Long? = null,
    val startAtMs: Long = 0L,
    val speed: Float = 1f,
    val volume: Float = 1f,
)

data class TextOverlay(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val startMs: Long = 0L,
    val endMs: Long? = null,
    val x: Float = 0.1f,
    val y: Float = 0.1f,
    val fontSize: Int = 36,
    val color: Int = Color.WHITE,
)

data class CropSpec(
    val ratio: Float,
)

data class VideoTransition(
    val type: VideoTransitionType = VideoTransitionType.CrossFade,
    val durationMs: Long = 350L,
)

enum class VideoTransitionType(val ffmpegName: String) {
    CrossFade("fade"),
    WipeLeft("wipeleft"),
    WipeRight("wiperight"),
    WipeUp("wipeup"),
    WipeDown("wipedown"),
    SlideLeft("slideleft"),
    SlideRight("slideright"),
    FadeBlack("fadeblack"),
}

enum class VideoFilter {
    None,
    Grayscale,
    Sepia,
    Vignette,
    ContrastBoost,
    Warm,
    Cool,
    Bright,
}

enum class VideoEffect {
    None,
    ZoomIn,
    ZoomOut,
    Shake,
    Glow,
    Blur,
}

data class ExportOptions(
    val format: VideoFormat = VideoFormat.Mp4,
    val width: Int? = null,
    val height: Int? = null,
    val fps: Int? = 30,
    val videoBitrate: Int? = null,
    val audioBitrate: Int? = 192_000,
    val includeAudio: Boolean = true,
    val file: File,
)

data class PreviewOptions(
    val width: Int = 640,
    val height: Int = 360,
    val fps: Int = 24,
    val maxDurationMs: Long = 10_000,
    val file: File,
)

enum class VideoFormat(val extension: String, val mimeType: String) {
    Mp4("mp4", "video/mp4"),
    Mov("mov", "video/quicktime"),
    Mkv("mkv", "video/x-matroska"),
}

sealed class VideoEditResult {
    data class Success(val file: File, val durationMs: Long, val message: String) : VideoEditResult()
    data class Failure(val message: String, val details: String? = null) : VideoEditResult()
}
