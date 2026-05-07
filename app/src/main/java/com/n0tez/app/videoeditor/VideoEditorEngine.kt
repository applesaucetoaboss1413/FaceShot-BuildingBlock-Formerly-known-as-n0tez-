package com.n0tez.app.videoeditor

import android.graphics.Color
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class VideoEditorEngine(
    private val workingDir: File,
) {
    private val logTag = "VideoEditorEngine"

    fun newTimelineFromVideo(path: String): VideoTimeline {
        val clip = VideoClip(sourcePath = path)
        val track = VideoTrack(clips = mutableListOf(clip))
        return VideoTimeline(videoTracks = mutableListOf(track))
    }

    fun newTimelineFromVideos(paths: List<String>): VideoTimeline {
        val clips = paths.map { VideoClip(sourcePath = it) }.toMutableList()
        val transitions = mutableListOf<VideoTransition>()
        if (clips.size > 1) {
            repeat(clips.size - 1) {
                transitions.add(VideoTransition())
            }
        }
        val track = VideoTrack(clips = clips, transitions = transitions)
        return VideoTimeline(videoTracks = mutableListOf(track))
    }

    suspend fun renderPreview(timeline: VideoTimeline, options: PreviewOptions): VideoEditResult {
        return withContext(Dispatchers.IO) {
            if (timeline.videoTracks.isEmpty()) {
                return@withContext VideoEditResult.Failure("No video tracks available")
            }
            options.file.parentFile?.mkdirs()
            val cmd = buildCommandForPreview(timeline, options)
            executeCommand(cmd, options.file)
        }
    }

    suspend fun export(timeline: VideoTimeline, options: ExportOptions): VideoEditResult {
        return withContext(Dispatchers.IO) {
            if (timeline.videoTracks.isEmpty()) {
                return@withContext VideoEditResult.Failure("No video tracks available")
            }
            options.file.parentFile?.mkdirs()
            val cmd = buildCommandForExport(timeline, options)
            executeCommand(cmd, options.file)
        }
    }

    private fun executeCommand(command: String, outputFile: File): VideoEditResult {
        Log.d(logTag, "ffmpeg: $command")
        val session = FFmpegKit.execute(command)
        val returnCode = session.returnCode
        if (ReturnCode.isSuccess(returnCode)) {
            val durationMs = outputFile.length().coerceAtLeast(0L)
            return VideoEditResult.Success(outputFile, durationMs, "Success")
        }
        val failStack = session.failStackTrace
        val message = session.output
        Log.e(logTag, "ffmpeg failed: $message")
        return VideoEditResult.Failure("Export failed", failStack ?: message)
    }

    private fun buildCommandForPreview(timeline: VideoTimeline, options: PreviewOptions): String {
        val base = buildGraph(timeline)
        val scale = "scale=${options.width}:${options.height}:flags=lanczos"
        val fps = "fps=${options.fps}"
        val previewFilter = listOf(base.filterComplex, "[${base.videoOut}]$scale,$fps[previewv]").joinToString(";")
        val trimMax = options.maxDurationMs.coerceAtLeast(1L)
        val output = options.file.absolutePath
        val args = mutableListOf<String>()
        args.addAll(base.inputArgs)
        args.add("-filter_complex")
        args.add(quote(previewFilter))
        args.add("-map")
        args.add("[previewv]")
        if (base.audioOut != null) {
            args.add("-map")
            args.add("[${base.audioOut}]")
        } else {
            args.add("-an")
        }
        args.addAll(listOf("-t", (trimMax / 1000f).toString()))
        args.addAll(listOf("-preset", "ultrafast", "-b:v", "1200k", "-movflags", "+faststart"))
        args.add(quote(output))
        return args.joinToString(" ")
    }

    private fun buildCommandForExport(timeline: VideoTimeline, options: ExportOptions): String {
        val base = buildGraph(timeline)
        val scale = if (options.width != null && options.height != null) {
            "scale=${options.width}:${options.height}:flags=lanczos"
        } else {
            null
        }
        val fps = options.fps?.let { "fps=$it" }
        val finalVideoLabel = "vout"
        val filterParts = mutableListOf<String>()
        filterParts.add(base.filterComplex)
        val chain = listOfNotNull(
            "[${base.videoOut}]",
            scale,
            fps,
            "format=yuv420p",
            "[$finalVideoLabel]",
        ).filterNotNull().joinToString(",")
        filterParts.add(chain)
        val filterComplex = filterParts.joinToString(";")
        val output = options.file.absolutePath
        val args = mutableListOf<String>()
        args.addAll(base.inputArgs)
        args.add("-filter_complex")
        args.add(quote(filterComplex))
        args.add("-map")
        args.add("[$finalVideoLabel]")
        if (options.includeAudio && base.audioOut != null) {
            args.add("-map")
            args.add("[${base.audioOut}]")
        } else {
            args.add("-an")
        }
        args.addAll(listOf("-preset", "veryfast", "-movflags", "+faststart"))
        options.videoBitrate?.let { args.addAll(listOf("-b:v", it.toString())) }
        options.audioBitrate?.let { args.addAll(listOf("-b:a", it.toString())) }
        args.addAll(listOf("-c:v", "libx264", "-c:a", "aac"))
        args.add(quote(output))
        return args.joinToString(" ")
    }

    private data class GraphBuild(
        val inputArgs: List<String>,
        val filterComplex: String,
        val videoOut: String,
        val audioOut: String?,
    )

    private fun buildGraph(timeline: VideoTimeline): GraphBuild {
        val inputArgs = mutableListOf<String>()
        val inputIndex = linkedMapOf<String, Int>()
        fun ensureInput(path: String): Int {
            val existing = inputIndex[path]
            if (existing != null) return existing
            val idx = inputIndex.size
            inputIndex[path] = idx
            inputArgs.addAll(listOf("-i", quote(path)))
            return idx
        }

        val filterParts = mutableListOf<String>()
        val baseTrack = timeline.videoTracks.first()
        val baseTrackResult = buildVideoTrack(baseTrack, ::ensureInput, filterParts)
        var videoOut = baseTrackResult.videoLabel
        var baseDuration = baseTrackResult.durationSec

        timeline.videoTracks.drop(1).forEachIndexed { index, track ->
            val trackResult = buildVideoTrack(track, ::ensureInput, filterParts)
            val overlayLabel = "v_overlay_$index"
            val enable = "between(t,0,$baseDuration)"
            filterParts.add("[$videoOut][${trackResult.videoLabel}]overlay=enable='$enable'[$overlayLabel]")
            videoOut = overlayLabel
            baseDuration = max(baseDuration, trackResult.durationSec)
        }

        if (timeline.textOverlays.isNotEmpty()) {
            val textOut = "v_text"
            val drawFilters = timeline.textOverlays.fold("[$videoOut]") { acc, overlay ->
                val color = formatColor(overlay.color)
                val alpha = ((overlay.color ushr 24) and 0xFF) / 255f
                val text = escapeDrawText(overlay.text)
                val startSec = overlay.startMs / 1000f
                val endSec = (overlay.endMs ?: (baseDuration * 1000).toLong()) / 1000f
                val x = overlay.x
                val y = overlay.y
                val draw = "drawtext=fontfile=/system/fonts/Roboto-Regular.ttf:text='$text':x=w*$x:y=h*$y:fontsize=${overlay.fontSize}:fontcolor=$color@${alpha}:enable='between(t,$startSec,$endSec)'"
                "$acc,$draw"
            }
            filterParts.add("$drawFilters[$textOut]")
            videoOut = textOut
        }

        val audioOut = buildAudioMix(timeline, ::ensureInput, filterParts, baseDuration)

        val filterComplex = filterParts.joinToString(";")
        return GraphBuild(inputArgs, filterComplex, videoOut, audioOut)
    }

    private data class TrackResult(
        val videoLabel: String,
        val durationSec: Float,
        val audioLabel: String?,
    )

    private fun buildVideoTrack(
        track: VideoTrack,
        ensureInput: (String) -> Int,
        filterParts: MutableList<String>,
    ): TrackResult {
        val clips = track.clips
        val clipResults = clips.map { clip ->
            val index = ensureInput(clip.sourcePath)
            val vLabel = "v_${clip.id.take(8)}"
            val startSec = clip.startMs / 1000f
            val endSec = (clip.endMs ?: 0L).takeIf { it > 0 }?.div(1000f)
            val trim = if (endSec != null) "trim=start=$startSec:end=$endSec" else "trim=start=$startSec"
            val setpts = "setpts=PTS-STARTPTS"
            val speed = if (clip.speed != 1f) "setpts=PTS/${clip.speed}" else null
            val crop = clip.crop?.let { cropExpr(it) }
            val rotate = rotateExpr(clip.rotationDegrees)
            val filter = filterExpr(clip.filter)
            val effects = clip.effects.mapNotNull { effectExpr(it) }
            val chain = listOfNotNull(
                "[$index:v]",
                trim,
                setpts,
                speed,
                crop,
                rotate,
                filter,
            ).plus(effects).joinToString(",")
            val offset = clip.startAtMs
            val offsetExpr = if (offset > 0) "setpts=PTS+${offset / 1000f}/TB" else null
            val fullChain = listOfNotNull(chain, offsetExpr, "[$vLabel]").joinToString(",")
            filterParts.add(fullChain)
            val durationSec = computeDurationSec(clip)
            val audioLabel = if (clip.includeAudio) {
                val aLabel = "a_${clip.id.take(8)}"
                val audioChain = buildAudioFromVideo(index, clip, aLabel)
                filterParts.add(audioChain)
                aLabel
            } else null
            ClipResult(vLabel, durationSec, audioLabel)
        }

        val transitions = track.transitions
        return if (clips.size > 1 && transitions.isNotEmpty()) {
            val transitionType = transitions.first().type
            val durationSec = transitions.first().durationMs / 1000f
            var currentLabel = clipResults.first().videoLabel
            var currentDuration = clipResults.first().durationSec
            clipResults.drop(1).forEachIndexed { idx, result ->
                val outLabel = "v_xfade_${track.id.take(6)}_$idx"
                val offset = max(0f, currentDuration - durationSec)
                filterParts.add("[$currentLabel][${result.videoLabel}]xfade=transition=${transitionType.ffmpegName}:duration=$durationSec:offset=$offset[$outLabel]")
                currentLabel = outLabel
                currentDuration = currentDuration + result.durationSec - durationSec
            }
            val audioLabel = buildAudioSequence(clipResults, transitions, filterParts, track.id)
            TrackResult(currentLabel, currentDuration, audioLabel)
        } else if (clips.size > 1) {
            val concatLabel = "v_concat_${track.id.take(6)}"
            val inputs = clipResults.joinToString("") { "[${it.videoLabel}]" }
            filterParts.add("${inputs}concat=n=${clipResults.size}:v=1:a=0[$concatLabel]")
            val total = clipResults.sumOf { it.durationSec.toDouble() }.toFloat()
            val audioLabel = buildAudioConcat(clipResults, filterParts, track.id)
            TrackResult(concatLabel, total, audioLabel)
        } else {
            TrackResult(clipResults.first().videoLabel, clipResults.first().durationSec, clipResults.first().audioLabel)
        }
    }

    private data class ClipResult(
        val videoLabel: String,
        val durationSec: Float,
        val audioLabel: String?,
    )

    private fun buildAudioMix(
        timeline: VideoTimeline,
        ensureInput: (String) -> Int,
        filterParts: MutableList<String>,
        baseDurationSec: Float,
    ): String? {
        val audioLabels = mutableListOf<String>()
        timeline.videoTracks.forEach { track ->
            val trackAudio = buildAudioTrackFromVideo(track, filterParts)
            if (trackAudio != null) audioLabels.add(trackAudio)
        }
        timeline.audioTracks.forEach { track ->
            track.clips.forEach { clip ->
                val idx = ensureInput(clip.sourcePath)
                val label = "a_ext_${clip.id.take(8)}"
                val startSec = clip.startMs / 1000f
                val endSec = clip.endMs?.takeIf { it > 0 }?.div(1000f)
                val trim = if (endSec != null) "atrim=start=$startSec:end=$endSec" else "atrim=start=$startSec"
                val setpts = "asetpts=PTS-STARTPTS"
                val tempo = atempoExpr(clip.speed)
                val volume = if (abs(clip.volume - 1f) > 0.001f) "volume=${clip.volume}" else null
                val delayMs = clip.startAtMs
                val delay = if (delayMs > 0) "adelay=${delayMs}|${delayMs}" else null
                val chain = listOfNotNull("[$idx:a]", trim, setpts, tempo, volume, delay, "[$label]").joinToString(",")
                filterParts.add(chain)
                audioLabels.add(label)
            }
        }
        if (audioLabels.isEmpty()) return null
        if (audioLabels.size == 1) return audioLabels.first()
        val mixLabel = "a_mix"
        val inputs = audioLabels.joinToString("") { "[$it]" }
        filterParts.add("${inputs}amix=inputs=${audioLabels.size}:duration=longest:dropout_transition=0[$mixLabel]")
        return mixLabel
    }

    private fun buildAudioTrackFromVideo(
        track: VideoTrack,
        filterParts: MutableList<String>,
    ): String? {
        val clipResults = track.clips.map { clip ->
            if (!clip.includeAudio) return@map null
            val aLabel = "a_${clip.id.take(8)}"
            aLabel
        }.filterNotNull()
        if (clipResults.isEmpty()) return null
        return if (clipResults.size == 1) {
            clipResults.first()
        } else {
            val concatLabel = "a_concat_${track.id.take(6)}"
            val inputs = clipResults.joinToString("") { "[$it]" }
            filterParts.add("${inputs}concat=n=${clipResults.size}:v=0:a=1[$concatLabel]")
            concatLabel
        }
    }

    private fun buildAudioSequence(
        clipResults: List<ClipResult>,
        transitions: List<VideoTransition>,
        filterParts: MutableList<String>,
        trackId: String,
    ): String? {
        val audioLabels = clipResults.mapNotNull { it.audioLabel }
        if (audioLabels.isEmpty()) return null
        if (audioLabels.size == 1) return audioLabels.first()
        var currentLabel = audioLabels.first()
        val durationSec = transitions.first().durationMs / 1000f
        audioLabels.drop(1).forEachIndexed { idx, label ->
            val outLabel = "a_xfade_${trackId.take(6)}_$idx"
            filterParts.add("[$currentLabel][$label]acrossfade=d=$durationSec[$outLabel]")
            currentLabel = outLabel
        }
        return currentLabel
    }

    private fun buildAudioConcat(
        clipResults: List<ClipResult>,
        filterParts: MutableList<String>,
        trackId: String,
    ): String? {
        val audioLabels = clipResults.mapNotNull { it.audioLabel }
        if (audioLabels.isEmpty()) return null
        val concatLabel = "a_concat_${trackId.take(6)}"
        val inputs = audioLabels.joinToString("") { "[$it]" }
        filterParts.add("${inputs}concat=n=${audioLabels.size}:v=0:a=1[$concatLabel]")
        return concatLabel
    }

    private fun buildAudioFromVideo(index: Int, clip: VideoClip, label: String): String {
        val startSec = clip.startMs / 1000f
        val endSec = clip.endMs?.takeIf { it > 0 }?.div(1000f)
        val trim = if (endSec != null) "atrim=start=$startSec:end=$endSec" else "atrim=start=$startSec"
        val setpts = "asetpts=PTS-STARTPTS"
        val tempo = atempoExpr(clip.speed)
        val volume = if (abs(clip.volume - 1f) > 0.001f) "volume=${clip.volume}" else null
        val delayMs = clip.startAtMs
        val delay = if (delayMs > 0) "adelay=${delayMs}|${delayMs}" else null
        return listOfNotNull("[$index:a]", trim, setpts, tempo, volume, delay, "[$label]").joinToString(",")
    }

    private fun atempoExpr(speed: Float): String? {
        if (speed == 1f) return null
        val parts = mutableListOf<Float>()
        var s = speed
        while (s > 2f) {
            parts.add(2f)
            s /= 2f
        }
        while (s < 0.5f) {
            parts.add(0.5f)
            s *= 2f
        }
        parts.add(s)
        return parts.joinToString(",") { "atempo=$it" }
    }

    private fun computeDurationSec(clip: VideoClip): Float {
        val durationMs = (clip.endMs ?: clip.startMs).let { end ->
            if (end <= clip.startMs) 0L else end - clip.startMs
        }
        val speed = if (clip.speed == 0f) 1f else clip.speed
        return (durationMs / 1000f) / speed
    }

    private fun cropExpr(crop: CropSpec): String {
        val ratio = max(0.1f, crop.ratio)
        val w = "min(iw,ih*$ratio)"
        val h = "min(ih,iw/$ratio)"
        return "crop=$w:$h:(iw-$w)/2:(ih-$h)/2"
    }

    private fun rotateExpr(deg: Int): String? {
        val d = ((deg % 360) + 360) % 360
        return when (d) {
            90 -> "transpose=1"
            180 -> "transpose=1,transpose=1"
            270 -> "transpose=2"
            0 -> null
            else -> "rotate=${Math.toRadians(d.toDouble())}:fillcolor=black"
        }
    }

    private fun filterExpr(filter: VideoFilter): String? {
        return when (filter) {
            VideoFilter.None -> null
            VideoFilter.Grayscale -> "format=gray"
            VideoFilter.Sepia -> "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131"
            VideoFilter.Vignette -> "vignette=PI/4"
            VideoFilter.ContrastBoost -> "eq=contrast=1.3:saturation=1.1"
            VideoFilter.Warm -> "colorbalance=rs=.05:gs=.02:bs=-.02"
            VideoFilter.Cool -> "colorbalance=rs=-.02:gs=.02:bs=.05"
            VideoFilter.Bright -> "eq=brightness=0.05:saturation=1.1"
        }
    }

    private fun effectExpr(effect: VideoEffect): String? {
        return when (effect) {
            VideoEffect.None -> null
            VideoEffect.ZoomIn -> "zoompan=z='min(zoom+0.002,1.5)':d=1"
            VideoEffect.ZoomOut -> "zoompan=z='max(zoom-0.002,1.0)':d=1"
            VideoEffect.Shake -> "tblend=all_mode=average,framestep=2"
            VideoEffect.Glow -> "gblur=sigma=5"
            VideoEffect.Blur -> "gblur=sigma=2"
        }
    }

    private fun escapeDrawText(text: String): String {
        return text.replace("\\", "\\\\").replace(":", "\\:").replace("'", "\\'")
    }

    private fun formatColor(color: Int): String {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return String.format("0x%02X%02X%02X", r, g, b)
    }

    private fun quote(value: String): String = "\"$value\""
}
