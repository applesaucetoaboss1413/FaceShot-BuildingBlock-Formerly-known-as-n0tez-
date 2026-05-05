package com.n0tez.app

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.n0tez.app.ui.compose.FaceshotTheme
import com.n0tez.app.ui.compose.FuturisticScreen
import com.n0tez.app.ui.compose.GlassPanel
import com.n0tez.app.ui.compose.HeroCard
import com.n0tez.app.ui.compose.PrimaryButton
import com.n0tez.app.ui.compose.SecondaryButton
import com.n0tez.app.ui.compose.SectionHeader
import com.n0tez.app.ui.compose.StatusPillRow
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

class VoiceRecorderActivity : AppCompatActivity() {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording by mutableStateOf(false)
    private var isPaused by mutableStateOf(false)
    private var recordingStartTime by mutableLongStateOf(0L)
    private var recordingDuration by mutableLongStateOf(0L)
    private var elapsedTime by mutableLongStateOf(0L)
    private var timerDisplay by mutableStateOf("00:00.00")
    private var waveformSeed by mutableFloatStateOf(0f)
    private var audioFilePath: String? = null
    private val handler = Handler(Looper.getMainLooper())

    private val audioPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(this, "Audio recording permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private val updateTimerRunnable = object : Runnable {
        override fun run() {
            if (isRecording && !isPaused) {
                val elapsed = currentElapsedMs()
                elapsedTime = elapsed
                updateTimerDisplay(elapsed)
                waveformSeed += 0.08f
                handler.postDelayed(this, 100L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FaceshotTheme {
                VoiceRecorderScreen()
            }
        }
    }

    private fun checkPermissionAndRecord() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        try {
            val audioDir = File(filesDir, "media/audio")
            if (!audioDir.exists()) audioDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val audioFile = File(audioDir, "recording_$timestamp.m4a")
            audioFilePath = audioFile.absolutePath

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }

            isRecording = true
            isPaused = false
            recordingStartTime = System.currentTimeMillis()
            recordingDuration = 0L
            elapsedTime = 0L
            timerDisplay = "00:00.00"
            handler.post(updateTimerRunnable)
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Failed to start recording: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePauseRecording() {
        try {
            if (isPaused) {
                mediaRecorder?.resume()
                recordingStartTime = System.currentTimeMillis()
                isPaused = false
                handler.post(updateTimerRunnable)
                Toast.makeText(this, "Recording resumed", Toast.LENGTH_SHORT).show()
            } else {
                mediaRecorder?.pause()
                recordingDuration += System.currentTimeMillis() - recordingStartTime
                elapsedTime = recordingDuration
                isPaused = true
                updateTimerDisplay(elapsedTime)
                Toast.makeText(this, "Recording paused", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to pause/resume: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            recordingDuration = currentElapsedMs()
            elapsedTime = recordingDuration
            isRecording = false
            isPaused = false
            handler.removeCallbacks(updateTimerRunnable)
            updateTimerDisplay(elapsedTime)
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to stop recording: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveRecording() {
        if (audioFilePath != null && File(audioFilePath!!).exists()) {
            Toast.makeText(this, "Recording saved: ${File(audioFilePath!!).name}", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "No recording to save", Toast.LENGTH_SHORT).show()
        }
    }

    private fun discardRecording() {
        if (audioFilePath != null) {
            val file = File(audioFilePath!!)
            if (file.exists()) file.delete()
        }
        Toast.makeText(this, "Recording discarded", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun currentElapsedMs(): Long {
        return if (isRecording && !isPaused) {
            (System.currentTimeMillis() - recordingStartTime) + recordingDuration
        } else {
            recordingDuration
        }
    }

    private fun updateTimerDisplay(milliseconds: Long) {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        val millis = (milliseconds % 1000) / 10
        timerDisplay = String.format("%02d:%02d.%02d", minutes, remainingSeconds, millis)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            stopRecording()
        }
        handler.removeCallbacks(updateTimerRunnable)
    }

    @Composable
    private fun VoiceRecorderScreen() {
        val hasSavedClip = audioFilePath != null
        val recordingState = when {
            isRecording && !isPaused -> "Recording"
            isRecording -> "Paused"
            hasSavedClip -> "Ready to save"
            else -> "Idle"
        }

        FuturisticScreen(
            title = "Voice Recorder",
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
                        eyebrow = "Audio Capture",
                        title = "A cleaner recording console with premium visual hierarchy.",
                        description = "Timer, capture controls, and save actions now sit in one polished shell for faster recording and fewer mis-taps.",
                    )
                }
                item {
                    StatusPillRow(
                        statuses = listOf(
                            "Mic Ready" to hasRecordPermission(),
                            recordingState to (isRecording || hasSavedClip),
                            "Live Waveform" to (isRecording && !isPaused),
                        ),
                    )
                }
                item {
                    GlassPanel {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            SectionHeader(
                                title = "Recorder Status",
                                subtitle = "Capture clear audio, pause safely, then save or discard with confidence.",
                            )
                            Text(
                                text = timerDisplay,
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = recordingState,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            WaveformPreview(
                                active = isRecording && !isPaused,
                                progressSeed = waveformSeed + (elapsedTime / 1000f),
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PrimaryButton(
                            text = if (isRecording) "Stop" else "Record",
                            onClick = {
                                if (isRecording) {
                                    stopRecording()
                                } else {
                                    checkPermissionAndRecord()
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            text = if (isPaused) "Resume" else "Pause",
                            onClick = ::togglePauseRecording,
                            enabled = isRecording,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PrimaryButton(
                            text = "Save",
                            onClick = ::saveRecording,
                            enabled = !isRecording && hasSavedClip,
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            text = "Discard",
                            onClick = ::discardRecording,
                            enabled = !isRecording && hasSavedClip,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun WaveformPreview(active: Boolean, progressSeed: Float) {
        val bars = remember(progressSeed) {
            List(28) { index ->
                val wave = sin(progressSeed + (index * 0.38f))
                0.24f + ((wave + 1f) / 2f) * 0.74f
            }
        }
        val waveColors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.86f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.82f),
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
        ) {
            val gap = 6f
            val barCount = bars.size
            val barWidth = ((size.width - (barCount - 1) * gap) / barCount).coerceAtLeast(4f)
            bars.forEachIndexed { index, value ->
                val left = index * (barWidth + gap)
                val ratio = if (active) value else 0.18f
                val barHeight = size.height * ratio
                val top = (size.height - barHeight) / 2f
                drawRoundRect(
                    brush = Brush.verticalGradient(waveColors),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }
        }
    }

    private fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
