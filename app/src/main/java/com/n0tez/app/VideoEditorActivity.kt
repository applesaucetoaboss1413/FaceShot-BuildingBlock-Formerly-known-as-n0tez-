package com.n0tez.app

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.AdapterView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.android.video.util.VideoEditorUtil
import com.n0tez.app.databinding.ActivityVideoEditorBinding
import com.n0tez.app.videoeditor.CropSpec
import com.n0tez.app.videoeditor.ExportOptions
import com.n0tez.app.videoeditor.PreviewOptions
import com.n0tez.app.videoeditor.VideoClip
import com.n0tez.app.videoeditor.VideoEditResult
import com.n0tez.app.videoeditor.VideoEditorEngine
import com.n0tez.app.videoeditor.VideoTimeline
import com.n0tez.app.videoeditor.VideoTrack
import com.n0tez.app.videoeditor.VideoTransition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*
// ============================================================
// TRAE BUILD AGENT — REPAIR MANIFEST
// File: app/src/main/java/com/n0tez/app/VideoEditorActivity.kt
//
// FIXES APPLIED:
//   [FIX-1] Preview URI swap no longer resets trimEnd/mediaPlayer.
//           Added `isPreviewLoad: Boolean` guard so setOnPreparedListener
//           skips full reinitialisation when a rendered preview is loaded.
//   [FIX-2] onSupportNavigateUp() uses finish() instead of deprecated
//           onBackPressed() (targetSdk 34 requirement).
//   [FIX-3] exportVideo() now inserts the exported file into MediaStore
//           (Pictures/Movies) so it appears in the device gallery.
//           Requires READ_MEDIA_VIDEO / WRITE_EXTERNAL_STORAGE permission
//           (already declared or added in AndroidManifest by this task).
//   [FIX-4] Export button text reset even on failure path (was left as
//           "Exporting…" on error).
//   [FIX-5] previewDir created if missing before engine init to prevent
//           FileNotFoundException on first cold start.
// ============================================================

class VideoEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoEditorBinding
    private var videoUri: Uri? = null
    private var videoPath: String? = null
    private var previewPath: String? = null

    // [FIX-5] Ensure preview directory exists before engine references it
    private val engine by lazy {
        val dir = File(cacheDir, "video_preview").also { it.mkdirs() }
        VideoEditorEngine(dir)
    }

    private val logTag = "VideoEditorActivity"
    private var isPlaying = false
    private var videoDuration = 0
    private var trimStart = 0f
    private var trimEnd = 0f
    private val handler = Handler(Looper.getMainLooper())
    private var playbackSpeed = 1f
    private var cropRatio: Float? = null
    private val cutPoints = mutableListOf<Long>()
    private var selectedQualityIndex = 0
    private var previewJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isSeeking = false

    // [FIX-1] Guard flag: true while a rendered preview is being loaded
    //         into VideoView so onPreparedListener does not reset state.
    private var isPreviewLoad = false

    private val pickVideoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null) {
                Toast.makeText(this, "No video selected", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            videoUri = uri
            videoPath = null
            isPreviewLoad = false
            loadVideo(uri)
        }

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            if (isPlaying) {
                val pos = binding.videoView.currentPosition
                updateTimeDisplay(pos, videoDuration)
                if (!isSeeking) binding.seekBarProgress.progress = pos
                if (trimEnd > 0 && pos >= trimEnd) binding.videoView.seekTo(trimStart.toInt())
                handler.postDelayed(this, 100)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()

        videoPath = intent.getStringExtra("VIDEO_FILE_PATH")
        videoUri  = intent.data

        when {
            videoUri  != null -> loadVideo(videoUri!!)
            videoPath != null -> loadVideo(Uri.fromFile(File(videoPath!!)))
            else              -> binding.tvVideoInfo.text = "Select a video to start"
        }
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Video Editor"

        binding.btnSelectVideo.setOnClickListener { pickVideoLauncher.launch("video/*") }

        binding.btnPlayPause.setOnClickListener {
            if (isPlaying) pauseVideo() else playVideo()
        }

        binding.btnStop.setOnClickListener {
            try { binding.videoView.stopPlayback() } catch (_: Throwable) {}
            isPlaying = false
            handler.removeCallbacks(updateProgressRunnable)
            binding.btnPlayPause.setIconResource(R.drawable.ic_play)
        }

        binding.btnSave.setOnClickListener   { exportVideo() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnExtractFrame.setOnClickListener { extractFrame() }

        binding.btnSpeedDown.setOnClickListener  { adjustSpeed(-0.25f) }
        binding.btnSpeedUp.setOnClickListener    { adjustSpeed(0.25f) }
        binding.btnSpeedReset.setOnClickListener { resetSpeed() }

        binding.btnCrop.setOnClickListener       { showCropDialog() }
        binding.btnCut.setOnClickListener        { addCutPoint() }
        binding.btnClearCuts.setOnClickListener  { clearCutPoints() }

        binding.seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.videoView.seekTo(progress)
                    updateTimeDisplay(progress, videoDuration)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { isSeeking = true }
            override fun onStopTrackingTouch(seekBar: SeekBar?)  { isSeeking = false }
        })

        binding.spinnerQuality.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: android.view.View?,
                    position: Int, id: Long
                ) { selectedQualityIndex = position }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedQualityIndex = 0
                }
            }

        binding.rangeSliderTrim.addOnChangeListener { slider, _, fromUser ->
            val values = slider.values
            trimStart = values[0]
            trimEnd   = values[1]
            if (fromUser) {
                binding.videoView.seekTo(trimStart.toInt())
                updateTimeDisplay(trimStart.toInt(), videoDuration)
                updateTrimInfo()
                applyPreviewDebounced()
            }
        }

        // [FIX-1] onPreparedListener skips reset when loading a preview clip
        binding.videoView.setOnPreparedListener { mp ->
            if (isPreviewLoad) {
                isPreviewLoad = false
                return@setOnPreparedListener
            }
            mediaPlayer  = mp
            videoDuration = mp.duration
            trimEnd       = videoDuration.toFloat()
            binding.rangeSliderTrim.valueFrom = 0f
            binding.rangeSliderTrim.valueTo   = videoDuration.toFloat()
            binding.rangeSliderTrim.setValues(0f, videoDuration.toFloat())
            binding.seekBarProgress.max       = videoDuration
            updateTimeDisplay(0, videoDuration)
            updateTrimInfo()
            applyPlaybackSpeed()
        }

        binding.videoView.setOnCompletionListener {
            isPlaying = false
            binding.btnPlayPause.setIconResource(R.drawable.ic_play)
        }

        binding.videoView.setOnErrorListener { _, _, _ ->
            isPlaying = false
            handler.removeCallbacks(updateProgressRunnable)
            binding.btnPlayPause.setIconResource(R.drawable.ic_play)
            Toast.makeText(this, "Video playback failed", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun loadVideo(uri: Uri) {
        try {
            binding.videoView.setVideoURI(uri)
            binding.tvVideoInfo.text = "Loading…"
        } catch (_: Throwable) {
            Toast.makeText(this, "Failed to load video", Toast.LENGTH_SHORT).show()
            return
        }

        videoPath?.let { updateNativeVideoInfo(it) }
        if (videoPath != null) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val tempFile = File(cacheDir, "video_${System.currentTimeMillis()}.mp4")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Could not open selected video")
                videoPath = tempFile.absolutePath
                withContext(Dispatchers.Main) { updateNativeVideoInfo(tempFile.absolutePath) }
            } catch (_: Throwable) {
                withContext(Dispatchers.Main) {
                    binding.tvVideoInfo.text = "Failed to load video file"
                    Toast.makeText(this@VideoEditorActivity, "Failed to load video file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateNativeVideoInfo(path: String) {
        if (!VideoEditorUtil.isAvailable()) {
            binding.tvVideoInfo.text = "Native video engine unavailable"
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val info = VideoEditorUtil.nativeGetVideoInfo(this@VideoEditorActivity, path)
                val resolved = if (info.isNullOrBlank()) "Native info unavailable" else info
                withContext(Dispatchers.Main) { binding.tvVideoInfo.text = resolved }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    binding.tvVideoInfo.text = "Native info error: ${e.message}"
                }
            }
        }
    }

    private fun playVideo() {
        binding.videoView.start()
        isPlaying = true
        binding.btnPlayPause.setIconResource(R.drawable.ic_pause)
        handler.post(updateProgressRunnable)
    }

    private fun pauseVideo() {
        binding.videoView.pause()
        isPlaying = false
        binding.btnPlayPause.setIconResource(R.drawable.ic_play)
        handler.removeCallbacks(updateProgressRunnable)
    }

    private fun updateTimeDisplay(current: Int, total: Int) {
        binding.tvCurrentTime.text = formatTime(current)
        binding.tvTotalTime.text   = formatTime(total)
    }

    private fun updateTrimInfo() {
        val start    = trimStart.toInt()
        val end      = trimEnd.toInt()
        val duration = (end - start).coerceAtLeast(0)
        binding.tvTrimInfo.text =
            "Trim: ${formatTime(start)} – ${formatTime(end)}  (Duration: ${formatTime(duration)})"
    }

    private fun formatTime(millis: Int): String {
        val s = millis / 1000
        return String.format("%02d:%02d", s / 60, s % 60)
    }

    private fun adjustSpeed(delta: Float) {
        playbackSpeed = (playbackSpeed + delta).coerceIn(0.25f, 4f)
        binding.tvSpeed.text = String.format(Locale.US, "%.2fx", playbackSpeed)
        applyPlaybackSpeed()
        applyPreviewDebounced()
    }

    private fun resetSpeed() {
        playbackSpeed = 1f
        binding.tvSpeed.text = "1.00x"
        applyPlaybackSpeed()
        applyPreviewDebounced()
    }

    private fun applyPlaybackSpeed() {
        val mp = mediaPlayer ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                val params = mp.playbackParams
                params.speed = playbackSpeed
                mp.playbackParams = params
            } catch (e: Exception) {
                Log.w(logTag, "Failed to set playback speed: ${e.message}")
            }
        }
    }

    private fun showCropDialog() {
        val options = arrayOf("None", "1:1", "4:5", "16:9", "9:16")
        AlertDialog.Builder(this)
            .setTitle("Crop Ratio")
            .setItems(options) { _, which ->
                cropRatio = when (which) {
                    1 -> 1f; 2 -> 4f / 5f; 3 -> 16f / 9f; 4 -> 9f / 16f; else -> null
                }
                binding.tvCropRatio.text = "Crop: ${options.getOrNull(which) ?: "None"}"
                applyPreviewDebounced()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addCutPoint() {
        val position = binding.videoView.currentPosition.toLong()
        if (position <= 0) {
            Toast.makeText(this, "Seek to a position to cut", Toast.LENGTH_SHORT).show()
            return
        }
        if (cutPoints.contains(position)) {
            Toast.makeText(this, "Cut point already added", Toast.LENGTH_SHORT).show()
            return
        }
        cutPoints.add(position)
        cutPoints.sort()
        updateCutPointsLabel()
        applyPreviewDebounced()
    }

    private fun clearCutPoints() {
        cutPoints.clear()
        updateCutPointsLabel()
        applyPreviewDebounced()
    }

    private fun updateCutPointsLabel() {
        binding.tvCutPoints.text = if (cutPoints.isEmpty()) "No cut points"
        else "Cuts: ${cutPoints.joinToString(", ") { formatTime(it.toInt()) }}"
    }

    private fun extractFrame() {
        val path = videoPath ?: run {
            Toast.makeText(this, "No video loaded", Toast.LENGTH_SHORT).show()
            return
        }
        if (!VideoEditorUtil.isAvailable()) {
            Toast.makeText(this, "Native video engine unavailable", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            try {
                val w = if (binding.videoView.width  > 0) binding.videoView.width  else 640
                val h = if (binding.videoView.height > 0) binding.videoView.height else 360
                bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                if (VideoEditorUtil.nativeOpenVideoFile(path, 0) < 0)
                    throw IllegalStateException("Native open failed")
                VideoEditorUtil.nativeSeekTo(binding.videoView.currentPosition.toLong())
                if (VideoEditorUtil.nativeGetNextFrame(bitmap) < 0)
                    throw IllegalStateException("Frame extraction failed")
                val outputFile = File(cacheDir, "frame_${System.currentTimeMillis()}.png")
                FileOutputStream(outputFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@VideoEditorActivity,
                        "Frame saved: ${outputFile.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@VideoEditorActivity,
                        "Frame extract failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                VideoEditorUtil.nativeRelease()
                bitmap?.recycle()
            }
        }
    }

    private fun applyPreviewDebounced() {
        val path = videoPath ?: return
        previewJob?.cancel()
        previewJob = lifecycleScope.launch {
            delay(120)
            val output = File(
                cacheDir,
                "video_preview/preview_${System.currentTimeMillis()}.mp4"
            )
            val previewOptions = PreviewOptions(
                width = 640, height = 360, fps = 24, maxDurationMs = 6_000, file = output
            )
            val timeline = buildTimeline(path)
            when (val result = engine.renderPreview(timeline, previewOptions)) {
                is VideoEditResult.Success -> {
                    previewPath   = result.file.absolutePath
                    // [FIX-1] Signal that the next onPrepared is a preview load
                    isPreviewLoad = true
                    binding.videoView.setVideoURI(Uri.fromFile(result.file))
                    binding.videoView.seekTo(0)
                }
                is VideoEditResult.Failure -> {
                    Log.w(logTag, "Preview failed: ${result.message} ${result.details.orEmpty()}")
                }
            }
        }
    }

    private fun buildTimeline(path: String): VideoTimeline {
        val baseClip = VideoClip(
            sourcePath = path,
            startMs    = trimStart.toLong(),
            endMs      = trimEnd.toLong().takeIf { it > 0 },
            speed      = playbackSpeed,
            crop       = cropRatio?.let { CropSpec(it) },
        )
        val clips = if (cutPoints.isEmpty()) {
            mutableListOf(baseClip)
        } else {
            val points   = listOf(trimStart.toLong()) + cutPoints + listOf(trimEnd.toLong())
            val segments = mutableListOf<VideoClip>()
            points.windowed(2).forEach { (start, end) ->
                if (end > start) segments.add(
                    baseClip.copy(id = UUID.randomUUID().toString(), startMs = start, endMs = end)
                )
            }
            segments
        }
        val transitions =
            if (clips.size > 1) MutableList(clips.size - 1) { VideoTransition() }
            else mutableListOf()
        return VideoTimeline(videoTracks = mutableListOf(VideoTrack(clips = clips, transitions = transitions)))
    }

    private fun exportVideo() {
        val path = videoPath ?: run {
            Toast.makeText(this, "No video loaded", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val outputDir  = File(filesDir, "media/video").also { it.mkdirs() }
            val outputFile = File(outputDir, "export_${System.currentTimeMillis()}.mp4")
            val (width, height, bitrate) = when (selectedQualityIndex) {
                1    -> Triple(1920, 1080, 6_000_000)
                2    -> Triple(1280, 720,  3_500_000)
                3    -> Triple(854,  480,  2_000_000)
                else -> Triple(null, null, null)
            }
            val options  = ExportOptions(width = width, height = height, fps = 30, videoBitrate = bitrate, file = outputFile)
            val timeline = buildTimeline(path)

            binding.btnSave.isEnabled = false
            binding.btnSave.text      = "Exporting…"

            val result = withContext(Dispatchers.IO) { engine.export(timeline, options) }

            // [FIX-4] Always restore button — even on failure
            binding.btnSave.isEnabled = true
            binding.btnSave.text      = "Export"

            when (result) {
                is VideoEditResult.Success -> {
                    // [FIX-3] Insert into MediaStore so file is visible in gallery
                    insertVideoIntoMediaStore(result.file)
                    Toast.makeText(
                        this@VideoEditorActivity,
                        "Exported: ${result.file.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                    shareVideo(result.file)
                }
                is VideoEditResult.Failure -> {
                    Toast.makeText(
                        this@VideoEditorActivity,
                        "Export failed: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e(logTag, "Export failure: ${result.details.orEmpty()}")
                }
            }
        }
    }

    // [FIX-3] MediaStore insertion for gallery visibility
    private fun insertVideoIntoMediaStore(file: File) {
        try {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/FaceShot")
                put(android.provider.MediaStore.Video.Media.IS_PENDING, 0)
            }
            val collection = android.provider.MediaStore.Video.Media
                .getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = contentResolver.insert(collection, values) ?: return
            contentResolver.openOutputStream(itemUri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            }
            Log.d(logTag, "Video inserted into MediaStore: $itemUri")
        } catch (e: Exception) {
            Log.w(logTag, "MediaStore insert failed (non-fatal): ${e.message}")
        }
    }

    private fun shareVideo(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(share, "Share video"))
        } catch (e: Exception) {
            Toast.makeText(this, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // [FIX-2] Use finish() — onBackPressed() is deprecated at targetSdk 34
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressRunnable)
        previewJob?.cancel()
        mediaPlayer = null
    }
}
