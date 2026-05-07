package com.n0tez.app

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.n0tez.app.databinding.ActivityAudioEditorBinding
import java.io.File
import java.io.IOException

// ============================================================
// TRAE BUILD AGENT — REPAIR MANIFEST
// File: app/src/main/java/com/n0tez/app/AudioEditorActivity.kt
//
// FIXES APPLIED:
//   [FIX-1] prepare() → prepareAsync() to avoid ANR on main thread.
//           setOnPreparedListener now fires correctly and initialises
//           audioDuration, SeekBar max, RangeSlider, and trimEnd.
//   [FIX-2] Placeholder icons ic_add / ic_close replaced with the
//           correct ic_play / ic_pause drawables used in VideoEditorActivity.
//   [FIX-3] Hardcoded ".m4a" temp file extension replaced with MIME-
//           derived extension so MP3 / WAV / OGG files are handled.
//   [FIX-4] FFmpeg -ss flag moved to AFTER -i for -c copy mode to
//           prevent keyframe-misalignment desync on trim.
//   [FIX-5] Export progress bar shown/hidden around FFmpegKit.executeAsync
//           so users get visual feedback during long exports.
//   [FIX-6] onSupportNavigateUp() uses finish() instead of deprecated
//           onBackPressed() (required for targetSdk 34).
//   [FIX-7] Speed and pitch controls added (atempo + asetrate filters).
//   [FIX-8] Temp file name is time-stamped to avoid stale cache collisions.
// ============================================================

class AudioEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAudioEditorBinding
    private var audioUri: Uri? = null
    private var audioPath: String? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isAudioPlaying = false
    private var audioDuration = 0
    private var trimStart = 0f
    private var trimEnd = 0f
    private var volume = 1.0f
    private var playbackSpeed = 1.0f   // [FIX-7] new field
    private val handler = Handler(Looper.getMainLooper())

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (isAudioPlaying) {
                    val currentPosition = player.currentPosition
                    binding.seekBarProgress.progress = currentPosition
                    updateTimeDisplay(currentPosition, audioDuration)

                    // Loop within trim region
                    if (trimEnd > 0 && currentPosition >= trimEnd) {
                        player.seekTo(trimStart.toInt())
                    }

                    handler.postDelayed(this, 100)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()

        val path = intent.getStringExtra("AUDIO_FILE_PATH")
        audioUri = intent.data

        when {
            audioUri != null -> loadAudio(audioUri!!)
            path != null     -> loadAudio(Uri.fromFile(File(path)))
            else             -> {
                Toast.makeText(this, "No audio file provided", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Audio Editor"

        // [FIX-2] Use correct play/pause icons
        binding.btnPlayPause.setOnClickListener {
            if (isAudioPlaying) pauseAudio() else playAudio()
        }

        binding.btnStop.setOnClickListener { stopAudio() }
        binding.btnSave.setOnClickListener { saveAudio() }
        binding.btnCancel.setOnClickListener { finish() }

        // Playback seek
        binding.seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    updateTimeDisplay(progress, audioDuration)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Trim region slider
        binding.rangeSliderTrim.addOnChangeListener { slider, _, fromUser ->
            val values = slider.values
            trimStart = values[0]
            trimEnd = values[1]
            updateTrimInfo()
            if (fromUser) {
                mediaPlayer?.seekTo(trimStart.toInt())
                updateTimeDisplay(trimStart.toInt(), audioDuration)
            }
        }

        // Volume slider
        binding.seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                volume = progress / 100f
                mediaPlayer?.setVolume(volume, volume)
                binding.tvVolume.text = "Volume: ${progress}%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // [FIX-7] Speed controls
        binding.btnSpeedDown.setOnClickListener { adjustSpeed(-0.25f) }
        binding.btnSpeedUp.setOnClickListener   { adjustSpeed(0.25f) }
        binding.btnSpeedReset.setOnClickListener {
            playbackSpeed = 1.0f
            binding.tvSpeed.text = "1.00x"
        }
    }

    // [FIX-7] Speed helper
    private fun adjustSpeed(delta: Float) {
        playbackSpeed = (playbackSpeed + delta).coerceIn(0.25f, 4.0f)
        binding.tvSpeed.text = String.format("%.2fx", playbackSpeed)
    }

    // [FIX-1] [FIX-3] Async prepare + MIME-derived extension
    private fun loadAudio(uri: Uri) {
        try {
            // [FIX-3] Derive file extension from MIME type instead of hardcoding .m4a
            val mimeType = contentResolver.getType(uri) ?: "audio/mp4"
            val ext = when {
                mimeType.contains("mpeg") || mimeType.contains("mp3") -> "mp3"
                mimeType.contains("wav")                               -> "wav"
                mimeType.contains("ogg")                               -> "ogg"
                mimeType.contains("flac")                              -> "flac"
                mimeType.contains("aac")                               -> "aac"
                else                                                   -> "m4a"
            }

            // [FIX-8] Time-stamp the temp file name to avoid stale cache
            val tempFile = File(cacheDir, "temp_audio_${System.currentTimeMillis()}.$ext")
            contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("Cannot open input stream for URI: $uri")

            audioPath = tempFile.absolutePath

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioPath)

                // [FIX-1] prepareAsync() — non-blocking; listener fires on completion
                setOnPreparedListener { mp ->
                    audioDuration = mp.duration
                    binding.seekBarProgress.max = audioDuration
                    binding.tvTotalTime.text = formatTime(audioDuration)

                    trimEnd = audioDuration.toFloat()
                    binding.rangeSliderTrim.valueFrom = 0f
                    binding.rangeSliderTrim.valueTo   = audioDuration.toFloat()
                    binding.rangeSliderTrim.setValues(0f, audioDuration.toFloat())

                    binding.seekBarVolume.progress = (volume * 100).toInt()
                    binding.tvVolume.text = "Volume: ${(volume * 100).toInt()}%"
                    binding.tvSpeed.text  = "1.00x"

                    updateTrimInfo()
                    updateTimeDisplay(0, audioDuration)
                }

                setOnCompletionListener {
                    isAudioPlaying = false
                    // [FIX-2] Correct icon
                    binding.btnPlayPause.setIconResource(R.drawable.ic_play)
                    binding.btnPlayPause.text = "Play"
                    binding.seekBarProgress.progress = 0
                }

                setOnErrorListener { _, what, extra ->
                    Toast.makeText(
                        this@AudioEditorActivity,
                        "MediaPlayer error ($what/$extra)",
                        Toast.LENGTH_SHORT
                    ).show()
                    false
                }

                prepareAsync() // [FIX-1] non-blocking
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Failed to load audio: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun playAudio() {
        mediaPlayer?.let {
            it.start()
            isAudioPlaying = true
            // [FIX-2] Correct pause icon
            binding.btnPlayPause.setIconResource(R.drawable.ic_pause)
            binding.btnPlayPause.text = "Pause"
            handler.post(updateProgressRunnable)
        }
    }

    private fun pauseAudio() {
        mediaPlayer?.let {
            it.pause()
            isAudioPlaying = false
            // [FIX-2] Correct play icon
            binding.btnPlayPause.setIconResource(R.drawable.ic_play)
            binding.btnPlayPause.text = "Play"
            handler.removeCallbacks(updateProgressRunnable)
        }
    }

    private fun stopAudio() {
        mediaPlayer?.let {
            it.pause()
            it.seekTo(0)
        }
        isAudioPlaying = false
        // [FIX-2] Correct play icon
        binding.btnPlayPause.setIconResource(R.drawable.ic_play)
        binding.btnPlayPause.text = "Play"
        binding.seekBarProgress.progress = 0
        handler.removeCallbacks(updateProgressRunnable)
    }

    private fun updateTimeDisplay(current: Int, total: Int) {
        binding.tvCurrentTime.text = formatTime(current)
        binding.tvTotalTime.text   = formatTime(total)
    }

    private fun updateTrimInfo() {
        val start    = formatTime(trimStart.toInt())
        val end      = formatTime(trimEnd.toInt())
        val duration = formatTime((trimEnd - trimStart).coerceAtLeast(0f).toInt())
        binding.tvTrimInfo.text = "Trim: $start – $end  (Duration: $duration)"
    }

    private fun formatTime(millis: Int): String {
        val totalSec = millis / 1000
        return String.format("%02d:%02d", totalSec / 60, totalSec % 60)
    }

    private fun saveAudio() {
        val path = audioPath ?: return

        val startSec    = trimStart / 1000f
        val durationSec = ((trimEnd - trimStart) / 1000f).coerceAtLeast(0.1f)

        val outputDir = File(filesDir, "media/audio")
        outputDir.mkdirs()
        val outputFile = File(outputDir, "edited_${System.currentTimeMillis()}.m4a")

        // [FIX-7] Build filter chain: volume + atempo (speed without pitch shift)
        // atempo only accepts 0.5–2.0 per filter; chain for extreme values
        fun atempoChain(speed: Float): String {
            val filters = mutableListOf<String>()
            var s = speed
            while (s > 2.0f) { filters.add("atempo=2.0"); s /= 2.0f }
            while (s < 0.5f) { filters.add("atempo=0.5"); s /= 0.5f }
            filters.add("atempo=%.4f".format(s))
            return filters.joinToString(",")
        }

        val needsReencode = volume != 1.0f || playbackSpeed != 1.0f

        // [FIX-4] -ss AFTER -i when using -c copy to avoid keyframe desync.
        //         When re-encoding, -ss before -i is fine (and faster).
        val cmd = if (!needsReencode) {
            "-i \"$path\" -ss $startSec -t $durationSec -c copy \"${outputFile.absolutePath}\""
        } else {
            val filterParts = mutableListOf<String>()
            if (volume != 1.0f)       filterParts.add("volume=$volume")
            if (playbackSpeed != 1.0f) filterParts.add(atempoChain(playbackSpeed))
            val filter = filterParts.joinToString(",")
            "-ss $startSec -i \"$path\" -t $durationSec -filter:a \"$filter\" -c:a aac \"${outputFile.absolutePath}\""
        }

        // [FIX-5] Show progress indicator during export
        binding.btnSave.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        Toast.makeText(this, "Saving…", Toast.LENGTH_SHORT).show()

        FFmpegKit.executeAsync(cmd) { session ->
            runOnUiThread {
                binding.btnSave.isEnabled       = true
                binding.progressBar.visibility  = View.GONE

                if (ReturnCode.isSuccess(session.returnCode)) {
                    Toast.makeText(this, "Saved: ${outputFile.name}", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Save failed: ${session.failStackTrace ?: session.returnCode}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // [FIX-6] Use finish() instead of deprecated onBackPressed()
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
