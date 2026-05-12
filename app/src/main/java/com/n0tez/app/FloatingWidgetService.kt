package com.n0tez.app

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.provider.Settings
import androidx.core.app.ServiceCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.n0tez.app.data.Note
import com.n0tez.app.data.NoteRepository
import com.n0tez.app.databinding.FloatingBubbleBinding
import com.n0tez.app.databinding.FloatingNotepadBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

class FloatingWidgetService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingBubbleView: View? = null
    private var floatingNotepadView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var notepadParams: WindowManager.LayoutParams? = null
    
    private lateinit var noteRepository: NoteRepository
    private var currentNote: Note? = null
    
    private val isNotepadExpanded = AtomicBoolean(false)
    private val isTransitioning = AtomicBoolean(false)
    
    private var transparencyLevel = 20
    private var notepadWidth = 380
    private var notepadHeight = 560
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var pendingCaptureAfterPermission = false
    private val textRecognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var autoSaveJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private val hideBubbleRunnable = Runnable {
        floatingBubbleView?.animate()?.alpha(0.3f)?.setDuration(300)?.start()
    }
    
    internal var testNotepadOpened = false

    companion object {
        const val ACTION_STOP = "com.n0tez.app.STOP_WIDGET"
        const val ACTION_SCREEN_CAPTURE_READY = "com.n0tez.app.SCREEN_CAPTURE_READY"
        const val EXTRA_MEDIA_PROJECTION_RESULT_CODE = "extra_media_projection_result_code"
        const val EXTRA_MEDIA_PROJECTION_DATA = "extra_media_projection_data"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "n0tez_widget_channel"
        private const val PREFS_NAME = "faceshot_buildingblock_prefs"
        private const val MIN_NOTEPAD_WIDTH_DP = 360
        private const val MIN_NOTEPAD_HEIGHT_DP = 440
        private const val MAX_NOTEPAD_WIDTH_DP = 860
        private const val MAX_NOTEPAD_HEIGHT_DP = 1200
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        noteRepository = NoteRepository(this)
        
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        transparencyLevel = prefs.getInt("widget_transparency", 20)
        notepadWidth = prefs.getInt("notepad_width", 380).coerceAtLeast(MIN_NOTEPAD_WIDTH_DP)
        notepadHeight = prefs.getInt("notepad_height", 560).coerceAtLeast(MIN_NOTEPAD_HEIGHT_DP)
        
        createNotificationChannel()
        startForegroundService()
        logInternal("service_start")
        readAndLogIconMetadata()
        createFloatingBubble()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Widget Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        logInternal("fgs_start_attempt")
        val stopIntent = Intent(this, FloatingWidgetService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FaceShot-BuildingBlock Active")
            .setContentText("Tap to open app, or use floating bubble")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_close, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            logInternal("fgs_started")
        } catch (e: SecurityException) {
            logInternal("fgs_security_exception", e)
            stopSelf()
        }
    }

    private fun promoteForegroundForScreenCapture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FaceShot-BuildingBlock Capture")
            .setContentText("Screen text capture is running only for your requested OCR action")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } catch (error: Exception) {
            logInternal("fgs_media_projection_promote_failed", error)
        }
    }

    private fun createFloatingBubble() {
        try {
            val binding = FloatingBubbleBinding.inflate(LayoutInflater.from(this))
            floatingBubbleView = binding.root
            logInternal("bubble_create_start")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                logInternal("overlay_permission_missing")
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                stopSelf()
                return
            }

            val bitmap = decodeOverlayIcon(R.drawable.ic_floating_bubble_original)
            binding.bubbleContainer.clipToOutline = true
            binding.bubbleIcon.clipToOutline = true
            binding.bubbleIcon.elevation = dpToPx(10).toFloat()
            if (bitmap != null) {
                binding.bubbleIcon.setImageBitmap(bitmap)
                logInternal("bubble_icon_set:${bitmap.width}x${bitmap.height}")
            } else {
                val fb = decodeOverlayIcon(R.drawable.ic_floating_bubble_large)
                if (fb != null) {
                    binding.bubbleIcon.setImageBitmap(fb)
                    logInternal("bubble_icon_fallback_set:${fb.width}x${fb.height}")
                } else {
                    binding.bubbleIcon.setImageResource(R.drawable.ic_notification)
                    logInternal("bubble_icon_fallback_vector")
                }
            }

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            bubbleParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 50
                y = 200
            }

            setupBubbleDragAndTap()
            try {
                windowManager?.addView(floatingBubbleView, bubbleParams)
                logInternal("bubble_view_added")
            } catch (e: WindowManager.BadTokenException) {
                logInternal("bad_token_add_bubble", e)
                throw e
            }
            mainHandler.postDelayed(hideBubbleRunnable, 5000)
        } catch (e: OutOfMemoryError) {
            logInternal("oom_create_bubble", e)
            Toast.makeText(this, "Low memory: Cannot show bubble", Toast.LENGTH_LONG).show()
        } catch (e: android.view.WindowManager.BadTokenException) {
             logInternal("bad_token_create_bubble", e)
        } catch (e: Exception) {
            logInternal("error_create_bubble", e)
            Toast.makeText(this, "Failed to create bubble", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBubbleDragAndTap() {
        val view = floatingBubbleView ?: return
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    mainHandler.removeCallbacks(hideBubbleRunnable)
                    view.animate().alpha(1.0f).setDuration(100).start()
                    
                    initialX = bubbleParams?.x ?: 0
                    initialY = bubbleParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_UP -> {
                    mainHandler.postDelayed(hideBubbleRunnable, 5000)
                    
                    if (!isDragging) {
                        toggleNotepad()
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    mainHandler.removeCallbacks(hideBubbleRunnable)
                    view.alpha = 1.0f
                    
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    
                    if (abs(deltaX) > touchSlop || abs(deltaY) > touchSlop) {
                        isDragging = true
                    }

                    if (isDragging) {
                        bubbleParams?.x = initialX + deltaX.toInt()
                        bubbleParams?.y = initialY + deltaY.toInt()
                        try {
                            windowManager?.updateViewLayout(floatingBubbleView, bubbleParams)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleNotepad() {
        if (!isTransitioning.compareAndSet(false, true)) return

        mainHandler.post {
            try {
                if (isNotepadExpanded.get()) {
                    closeNotepadInternal()
                } else {
                    logInternal("notepad_open_attempt")
                    openNotepadInternal()
                }
            } finally {
                mainHandler.postDelayed({ isTransitioning.set(false) }, 300)
            }
        }
    }

    private fun openNotepadInternal() {
        testNotepadOpened = true
        cleanupNotepadViewSafely()
        try {
            val binding = FloatingNotepadBinding.inflate(LayoutInflater.from(this))
            floatingNotepadView = binding.root
            // ... (rest of code)

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            notepadParams = WindowManager.LayoutParams(
                dpToPx(notepadWidth.coerceAtLeast(MIN_NOTEPAD_WIDTH_DP)),
                dpToPx(notepadHeight.coerceAtLeast(MIN_NOTEPAD_HEIGHT_DP)),
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (bubbleParams?.x ?: 50) + dpToPx(60)
                y = bubbleParams?.y ?: 200
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    logInternal("overlay_permission_missing_notepad")
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                    return
                }
                windowManager?.addView(floatingNotepadView, notepadParams)
                logInternal("notepad_view_added")
            } catch (e: WindowManager.BadTokenException) {
                logInternal("bad_token_add_notepad", e)
                throw e
            }
            isNotepadExpanded.set(true)
            
            setupNotepadUI(binding)
            
            floatingBubbleView?.findViewById<View>(R.id.bubble_icon)?.alpha = 0.5f
        } catch (e: Exception) {
            logInternal("error_open_notepad", e)
            Toast.makeText(this, "Failed to open notepad", Toast.LENGTH_SHORT).show()
            cleanupNotepadViewSafely()
        }
    }

    private fun cleanupNotepadViewSafely() {
        floatingNotepadView?.let { view ->
            try {
                val editText = view.findViewById<EditText>(R.id.notepad_edit_text)
                editText?.let { hideKeyboard(it) }
            } catch (e: Exception) {}
            
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {}
        }
        floatingNotepadView = null
        notepadParams = null
        isNotepadExpanded.set(false)
    }

    private fun setupNotepadUI(binding: FloatingNotepadBinding) {
        binding.apply {
            updateTransparency(transparencyLevel)
            transparencySeekbar.progress = transparencyLevel
            transparencyLabel.text = "${100 - transparencyLevel}%"

            // Load Note
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val note = noteRepository.getCurrentNote() ?: Note()
                    currentNote = note
                    withContext(Dispatchers.Main) {
                        notepadEditText.setText(note.content)
                        updatePinButton(btnPinNote)
                    }
                } catch (e: Exception) {
                    currentNote = Note()
                }
            }

            transparencySeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    transparencyLevel = progress
                    updateTransparency(progress)
                    transparencyLabel.text = "${100 - progress}%"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                        .putInt("widget_transparency", transparencyLevel).apply()
                }
            })

            notepadEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    autoSaveJob?.cancel()
                    autoSaveJob = serviceScope.launch {
                        delay(1000) // Debounce
                        saveCurrentNote()
                    }
                }
            })

            notepadEditText.setOnClickListener {
                it.requestFocus()
                showKeyboard(it as EditText)
            }

            btnCloseNotepad.setOnClickListener {
                saveCurrentNote()
                toggleNotepad()
            }

            btnSaveNote.setOnClickListener {
                saveCurrentNote()
                Toast.makeText(this@FloatingWidgetService, "Note saved", Toast.LENGTH_SHORT).show()
            }

            btnPinNote.setOnClickListener {
                currentNote?.let { note ->
                    note.isPinned = !note.isPinned
                    saveCurrentNote()
                    updatePinButton(btnPinNote)
                    val msg = if (note.isPinned) "Note pinned" else "Note unpinned"
                    Toast.makeText(this@FloatingWidgetService, msg, Toast.LENGTH_SHORT).show()
                }
            }

            btnNewNote.setOnClickListener {
                saveCurrentNote()
                currentNote = Note()
                notepadEditText.setText("")
                noteRepository.setCurrentNote(currentNote)
                updatePinButton(btnPinNote)
            }

            btnSelectNote.setOnClickListener {
                showSavedNotesPopup(notepadEditText, btnPinNote)
            }

            btnShareNote.setOnClickListener {
                shareCurrentNote()
            }

            btnCaptureText.setOnClickListener {
                updateCapturePermissionState(binding)
                captureVisibleTextIntoNote(notepadEditText)
            }
            updateCapturePermissionState(binding)

            btnDeleteNote.setOnClickListener {
                currentNote?.let { note ->
                    serviceScope.launch(Dispatchers.IO) {
                        noteRepository.deleteNote(note.id)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@FloatingWidgetService, "Note deleted", Toast.LENGTH_SHORT).show()
                            currentNote = Note()
                            notepadEditText.setText("")
                            noteRepository.setCurrentNote(currentNote)
                            updatePinButton(btnPinNote)
                        }
                    }
                }
            }

            btnShredNote.setOnClickListener {
                currentNote?.let { note ->
                    serviceScope.launch(Dispatchers.IO) {
                        noteRepository.shredNote(note.id)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@FloatingWidgetService, "Note securely shredded", Toast.LENGTH_SHORT).show()
                            currentNote = Note()
                            notepadEditText.setText("")
                            noteRepository.setCurrentNote(currentNote)
                            updatePinButton(btnPinNote)
                        }
                    }
                }
            }

            btnHome.setOnClickListener {
                saveCurrentNote()
                val intent = Intent(this@FloatingWidgetService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }

            setupNotepadDrag(headerBar)
            setupNotepadDrag(dragHandleChip)
            
            // Setup Resize
            btnResize?.let { setupResize(it) }
        }
    }

    private fun updateTransparency(level: Int) {
        val alpha = (100 - level) / 100f
        floatingNotepadView?.findViewById<View>(R.id.edit_text_background)?.alpha = alpha
        
        val container = floatingNotepadView?.findViewById<View>(R.id.notepad_container)
        container?.background?.alpha = ((100 - level) * 2.55f).toInt().coerceIn(0, 255)
    }

    private fun updatePinButton(btnPin: ImageButton) {
        val isPinned = currentNote?.isPinned == true
        btnPin.setColorFilter(if (isPinned) Color.parseColor("#FFD700") else Color.WHITE)
    }

    private fun updateCapturePermissionState(binding: FloatingNotepadBinding) {
        binding.btnCaptureText.alpha = 1.0f
        binding.btnCaptureText.setColorFilter(Color.parseColor("#FF00FFFF"))
        binding.captureStatusChip?.text = getString(R.string.screen_capture_ready_chip)
        binding.captureStatusChip?.setTextColor(Color.parseColor("#FF00FFFF"))
        binding.captureStatusChip?.contentDescription = getString(R.string.screen_capture_ready_description)
        binding.btnCaptureText.contentDescription = getString(R.string.screen_capture_text_description)
    }

    private fun setupNotepadDrag(dragView: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var pendingX = 0
        var pendingY = 0
        var updateScheduled = false
        var isDragging = false
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        fun schedulePositionUpdate() {
            if (updateScheduled) return
            updateScheduled = true
            mainHandler.post {
                updateScheduled = false
                val clamped = clampNotepadPosition(pendingX, pendingY, currentNotepadWidthPx(), currentNotepadHeightPx())
                notepadParams?.x = clamped.first
                notepadParams?.y = clamped.second
                try {
                    windowManager?.updateViewLayout(floatingNotepadView, notepadParams)
                } catch (e: Exception) {
                    logInternal("error_update_layout_notepad", e)
                }
            }
        }

        dragView.setOnTouchListener { touchedView, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = notepadParams?.x ?: 0
                    initialY = notepadParams?.y ?: 0
                    pendingX = initialX
                    pendingY = initialY
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    touchedView.animate().scaleX(1.01f).scaleY(1.01f).alpha(0.96f).setDuration(90).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    if (abs(deltaX) > touchSlop || abs(deltaY) > touchSlop) {
                        isDragging = true
                    }
                    pendingX = initialX + deltaX.toInt()
                    pendingY = initialY + deltaY.toInt()
                    schedulePositionUpdate()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    touchedView.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(120).start()
                    if (!isDragging) touchedView.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupResize(resizeButton: View) {
        var initialWidth = 0
        var initialHeight = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var pendingWidth = 0
        var pendingHeight = 0
        var updateScheduled = false
        var resized = false

        fun scheduleSizeUpdate() {
            if (updateScheduled) return
            updateScheduled = true
            mainHandler.post {
                updateScheduled = false
                notepadParams?.width = pendingWidth
                notepadParams?.height = pendingHeight
                try {
                    windowManager?.updateViewLayout(floatingNotepadView, notepadParams)
                } catch (e: Exception) {
                    logInternal("error_update_layout_resize", e)
                }
            }
        }

        resizeButton.setOnTouchListener { touchedView, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialWidth = currentNotepadWidthPx()
                    initialHeight = currentNotepadHeightPx()
                    pendingWidth = initialWidth
                    pendingHeight = initialHeight
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    resized = false
                    touchedView.animate().scaleX(1.12f).scaleY(1.12f).alpha(1.0f).setDuration(90).start()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    touchedView.animate().scaleX(1.0f).scaleY(1.0f).alpha(0.92f).setDuration(120).start()
                    if (resized) {
                        saveResizedNotepadDimensions()
                    } else {
                        touchedView.performClick()
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY

                    val maxWidth = maxNotepadWidthForCurrentPosition()
                    val maxHeight = maxNotepadHeightForCurrentPosition()
                    pendingWidth = (initialWidth + deltaX).toInt()
                        .coerceIn(dpToPx(MIN_NOTEPAD_WIDTH_DP), maxWidth)
                    pendingHeight = (initialHeight + deltaY).toInt()
                        .coerceIn(dpToPx(MIN_NOTEPAD_HEIGHT_DP), maxHeight)
                    resized = true
                    scheduleSizeUpdate()
                    true
                }
                else -> false
            }
        }
    }

    private fun currentNotepadWidthPx(): Int {
        val measuredWidth = floatingNotepadView?.width ?: 0
        val paramWidth = notepadParams?.width ?: 0
        return when {
            measuredWidth > 0 -> measuredWidth
            paramWidth > 0 -> paramWidth
            else -> dpToPx(notepadWidth.coerceAtLeast(MIN_NOTEPAD_WIDTH_DP))
        }
    }

    private fun currentNotepadHeightPx(): Int {
        val measuredHeight = floatingNotepadView?.height ?: 0
        val paramHeight = notepadParams?.height ?: 0
        return when {
            measuredHeight > 0 -> measuredHeight
            paramHeight > 0 -> paramHeight
            else -> dpToPx(MIN_NOTEPAD_HEIGHT_DP)
        }
    }

    private fun screenBoundsPx(): Rect {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager?.currentWindowMetrics?.bounds ?: Rect(0, 0, dpToPx(1080), dpToPx(1920))
        } else {
            val metrics = resources.displayMetrics
            Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
        }
    }

    private fun clampNotepadPosition(x: Int, y: Int, width: Int, height: Int): Pair<Int, Int> {
        val bounds = screenBoundsPx()
        val margin = dpToPx(12)
        val maxX = (bounds.width() - width - margin).coerceAtLeast(margin)
        val maxY = (bounds.height() - height - margin).coerceAtLeast(margin)
        return x.coerceIn(margin, maxX) to y.coerceIn(margin, maxY)
    }

    private fun maxNotepadWidthForCurrentPosition(): Int {
        val bounds = screenBoundsPx()
        val x = notepadParams?.x ?: dpToPx(24)
        val available = bounds.width() - x - dpToPx(12)
        return available.coerceIn(dpToPx(MIN_NOTEPAD_WIDTH_DP), dpToPx(MAX_NOTEPAD_WIDTH_DP))
    }

    private fun maxNotepadHeightForCurrentPosition(): Int {
        val bounds = screenBoundsPx()
        val y = notepadParams?.y ?: dpToPx(80)
        val available = bounds.height() - y - dpToPx(12)
        return available.coerceIn(dpToPx(MIN_NOTEPAD_HEIGHT_DP), dpToPx(MAX_NOTEPAD_HEIGHT_DP))
    }


    private fun saveResizedNotepadDimensions() {
        val widthPx = notepadParams?.width?.takeIf { it > 0 } ?: currentNotepadWidthPx()
        val heightPx = notepadParams?.height?.takeIf { it > 0 } ?: currentNotepadHeightPx()

        notepadWidth = pxToDp(widthPx).coerceAtLeast(MIN_NOTEPAD_WIDTH_DP)
        notepadHeight = pxToDp(heightPx).coerceAtLeast(MIN_NOTEPAD_HEIGHT_DP)

        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt("notepad_width", notepadWidth)
            .putInt("notepad_height", notepadHeight)
            .apply()
    }

    private fun saveCurrentNote() {
        try {
            val editText = floatingNotepadView?.findViewById<EditText>(R.id.notepad_edit_text) ?: return
            val content = editText.text.toString()
            
            currentNote?.let { note ->
                note.content = content
                if (content.isNotBlank()) {
                    noteRepository.saveNote(note)
                    noteRepository.setCurrentNote(note)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showSavedNotesPopup(editText: EditText, btnPin: ImageButton) {
        serviceScope.launch(Dispatchers.IO) {
            val notes = noteRepository.getActiveNotes()
            withContext(Dispatchers.Main) {
                if (notes.isEmpty()) {
                    Toast.makeText(this@FloatingWidgetService, "No saved notes", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                val noteNames = notes.map { note ->
                    val preview = note.getPreviewText()
                    if (preview.length > 30) "${preview.substring(0, 30)}..." else preview
                }

                val listView = ListView(this@FloatingWidgetService).apply {
                    adapter = ArrayAdapter(this@FloatingWidgetService, android.R.layout.simple_list_item_1, noteNames)
                    setBackgroundColor(Color.parseColor("#2A2D35"))
                }

                val popup = PopupWindow(
                    listView,
                    dpToPx(250),
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    true
                ).apply {
                    setBackgroundDrawable(ColorDrawable(Color.parseColor("#2A2D35")))
                    elevation = 10f
                }

                listView.setOnItemClickListener { _, _, position, _ ->
                    val selectedNote = notes[position]
                    currentNote = selectedNote
                    editText.setText(selectedNote.content)
                    noteRepository.setCurrentNote(selectedNote)
                    updatePinButton(btnPin)
                    Toast.makeText(this@FloatingWidgetService, "Note loaded", Toast.LENGTH_SHORT).show()
                    popup.dismiss()
                }

                popup.showAsDropDown(floatingNotepadView?.findViewById(R.id.btn_select_note))
            }
        }
    }

    private fun shareCurrentNote() {
        val editText = floatingNotepadView?.findViewById<EditText>(R.id.notepad_edit_text)
        val content = editText?.text?.toString() ?: ""
        
        if (content.isBlank()) {
            Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show()
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_SUBJECT, "Note from FaceShot-BuildingBlock")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        val chooserIntent = Intent.createChooser(shareIntent, "Share Note").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(chooserIntent)
    }

    private fun captureVisibleTextIntoNote(editText: EditText) {
        val captureBounds = buildCaptureBounds() ?: run {
            logInternal("capture_bounds_missing")
            Toast.makeText(this, R.string.screen_capture_no_text, Toast.LENGTH_SHORT).show()
            return
        }

        if (mediaProjection == null) {
            pendingCaptureAfterPermission = true
            Toast.makeText(this, R.string.screen_capture_permission_request, Toast.LENGTH_LONG).show()
            startActivity(
                Intent(this, ScreenCapturePermissionActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
            return
        }

        performScreenOcrCapture(editText, captureBounds)
    }

    private fun handleScreenCapturePermissionResult(intent: Intent) {
        val resultCode = intent.getIntExtra(EXTRA_MEDIA_PROJECTION_RESULT_CODE, Activity.RESULT_CANCELED)
        val resultData = intent.getParcelableExtra<Intent>(EXTRA_MEDIA_PROJECTION_DATA)
        if (resultCode != Activity.RESULT_OK || resultData == null) {
            pendingCaptureAfterPermission = false
            Toast.makeText(this, R.string.screen_capture_permission_denied, Toast.LENGTH_LONG).show()
            return
        }

        promoteForegroundForScreenCapture()
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = try {
            manager.getMediaProjection(resultCode, resultData).apply {
                registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() {
                        mediaProjection = null
                        releaseScreenCaptureResources()
                    }
                }, mainHandler)
            }
        } catch (error: Exception) {
            logInternal("media_projection_start_failed", error)
            Toast.makeText(this, R.string.screen_capture_failed, Toast.LENGTH_SHORT).show()
            null
        }

        if (pendingCaptureAfterPermission) {
            pendingCaptureAfterPermission = false
            val editText = floatingNotepadView?.findViewById<EditText>(R.id.notepad_edit_text)
            val bounds = buildCaptureBounds()
            if (editText != null && bounds != null) {
                performScreenOcrCapture(editText, bounds)
            }
        }
    }

    private fun performScreenOcrCapture(editText: EditText, captureBounds: Rect) {
        val projection = mediaProjection ?: run {
            Toast.makeText(this, R.string.screen_capture_permission_request, Toast.LENGTH_SHORT).show()
            return
        }
        val targetView = floatingNotepadView ?: return
        Toast.makeText(this, R.string.screen_capture_scanning, Toast.LENGTH_SHORT).show()
        targetView.visibility = View.INVISIBLE

        serviceScope.launch {
            try {
                val fullScreen = withContext(Dispatchers.Default) { captureScreenBitmap(projection) }
                val safeBounds = Rect(captureBounds).apply {
                    intersect(0, 0, fullScreen.width, fullScreen.height)
                }
                if (safeBounds.width() <= 4 || safeBounds.height() <= 4) {
                    fullScreen.recycle()
                    Toast.makeText(this@FloatingWidgetService, R.string.screen_capture_no_text, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val cropped = Bitmap.createBitmap(
                    fullScreen,
                    safeBounds.left,
                    safeBounds.top,
                    safeBounds.width(),
                    safeBounds.height(),
                )
                fullScreen.recycle()
                val capturedText = recognizeText(cropped).trim()
                cropped.recycle()
                if (capturedText.isBlank()) {
                    Toast.makeText(this@FloatingWidgetService, R.string.screen_capture_no_text, Toast.LENGTH_SHORT).show()
                    logInternal("screen_ocr_empty")
                    return@launch
                }
                insertCapturedText(editText, capturedText)
                saveCurrentNote()
                Toast.makeText(this@FloatingWidgetService, R.string.screen_capture_success, Toast.LENGTH_SHORT).show()
                logInternal("screen_ocr_success:${capturedText.length}")
            } catch (error: Exception) {
                logInternal("screen_ocr_failed", error)
                Toast.makeText(this@FloatingWidgetService, R.string.screen_capture_failed, Toast.LENGTH_SHORT).show()
                mediaProjection = null
            } finally {
                releaseScreenCaptureResources()
                targetView.visibility = View.VISIBLE
            }
        }
    }

    private suspend fun captureScreenBitmap(projection: MediaProjection): Bitmap {
        val bounds = screenBoundsPx()
        val width = bounds.width().coerceAtLeast(1)
        val height = bounds.height().coerceAtLeast(1)
        val densityDpi = resources.displayMetrics.densityDpi
        return withContext(Dispatchers.Main) {
            delay(180)
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            virtualDisplay = projection.createVirtualDisplay(
                "FaceShotTextCapture",
                width,
                height,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                mainHandler,
            )
            delay(320)
            val image = imageReader?.acquireLatestImage() ?: imageReader?.acquireNextImage()
            image?.use { imageToBitmap(it, width, height) }
                ?: throw IllegalStateException("No screen frame available")
        }
    }

    private fun imageToBitmap(image: Image, width: Int, height: Int): Bitmap {
        val plane = image.planes.first()
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width
        val paddedWidth = width + rowPadding / pixelStride
        val paddedBitmap = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
        paddedBitmap.copyPixelsFromBuffer(buffer)
        val bitmap = Bitmap.createBitmap(paddedBitmap, 0, 0, width, height)
        paddedBitmap.recycle()
        return bitmap
    }

    private suspend fun recognizeText(bitmap: Bitmap): String = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        textRecognizer.process(inputImage)
            .addOnSuccessListener { result ->
                if (continuation.isActive) continuation.resume(result.text)
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
    }

    private fun releaseScreenCaptureResources() {
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        try {
            imageReader?.close()
        } catch (_: Exception) {
        }
        virtualDisplay = null
        imageReader = null
    }

    private fun buildCaptureBounds(): Rect? {
        val bounds = Rect()
        val captureSurface = floatingNotepadView?.findViewById<View>(R.id.edit_text_background)
            ?: return null
        return if (captureSurface.getGlobalVisibleRect(bounds) && !bounds.isEmpty) {
            bounds
        } else {
            null
        }
    }

    private fun insertCapturedText(editText: EditText, capturedText: String) {
        val currentContent = editText.text ?: return
        val selectionStart = editText.selectionStart.coerceAtLeast(0)
        val selectionEnd = editText.selectionEnd.coerceAtLeast(0)
        val safeStart = minOf(selectionStart, selectionEnd)
        val safeEnd = maxOf(selectionStart, selectionEnd)
        val needsLeadingBreak = safeStart > 0 && currentContent.getOrNull(safeStart - 1) != '\n'
        val needsTrailingBreak = safeEnd < currentContent.length && currentContent.getOrNull(safeEnd) != '\n'
        val insertion = buildString {
            if (needsLeadingBreak) append('\n')
            append(capturedText)
            if (needsTrailingBreak) append('\n')
        }
        currentContent.replace(safeStart, safeEnd, insertion)
        val newSelection = (safeStart + insertion.length).coerceAtMost(currentContent.length)
        editText.setSelection(newSelection)
    }

    private fun closeNotepadInternal() {
        saveCurrentNote()
        cleanupNotepadViewSafely()
        try {
            floatingBubbleView?.findViewById<View>(R.id.bubble_icon)?.alpha = 1.0f
        } catch (e: Exception) {
            logInternal("error_close_notepad", e)
        }
    }

    private fun showKeyboard(editText: EditText) {
        editText.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun decodeOverlayIcon(resourceId: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options()
            bounds.inPreferredConfig = Bitmap.Config.ARGB_8888
            bounds.inJustDecodeBounds = true
            BitmapFactory.decodeResource(resources, resourceId, bounds)
            val target = 96
            val sample = computeSampleSize(bounds.outWidth, bounds.outHeight, target)
            val opts = BitmapFactory.Options()
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888
            opts.inSampleSize = sample
            var bm = BitmapFactory.decodeResource(resources, resourceId, opts)
            if (bm == null) return null
            if (bm.width != target || bm.height != target) {
                bm = Bitmap.createScaledBitmap(bm, target, target, true)
            }
            createCircularBitmap(bm)
        } catch (e: OutOfMemoryError) {
            logInternal("oom_decode_icon", e)
            null
        } catch (e: Exception) {
            logInternal("error_decode_icon", e)
            null
        }
    }

    private fun computeSampleSize(w: Int, h: Int, target: Int): Int {
        var sample = 1
        var width = w
        var height = h
        while (width / sample > target * 2 || height / sample > target * 2) {
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }

    private fun createCircularBitmap(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height)
        val xOffset = (source.width - size) / 2
        val yOffset = (source.height - size) / 2
        val squared = Bitmap.createBitmap(source, xOffset, yOffset, size, size)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)
        if (squared != source && !squared.isRecycled) {
            squared.recycle()
        }
        return output
    }

    private fun readAndLogIconMetadata() {
        try {
            val id = resources.getIdentifier("icon_metadata", "raw", packageName)
            if (id != 0) {
                val text = resources.openRawResource(id).bufferedReader().use { it.readText() }
                logInternal("icon_metadata:${text.length}")
            } else {
                logInternal("icon_metadata_missing")
            }
        } catch (e: Exception) {
            logInternal("icon_metadata_error", e)
        }
    }

    private fun logInternal(event: String, throwable: Throwable? = null) {
        try {
            val msg = if (throwable != null) "$event:${throwable.message ?: "err"}" else event
            android.util.Log.d("FloatingWidgetService", msg, throwable)
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existing = prefs.getString("service_logs", "") ?: ""
            val combined = (existing + "\n" + System.currentTimeMillis() + ":" + msg)
            val trimmed = if (combined.length > 10000) combined.takeLast(10000) else combined
            prefs.edit().putString("service_logs", trimmed).apply()
        } catch (_: Exception) {}
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun pxToDp(px: Int): Int {
        return (px / resources.displayMetrics.density).toInt()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SCREEN_CAPTURE_READY -> {
                handleScreenCapturePermissionResult(intent)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        saveCurrentNote()
        autoSaveJob?.cancel()
        releaseScreenCaptureResources()
        try {
            mediaProjection?.stop()
        } catch (_: Exception) {
        }
        textRecognizer.close()
        serviceScope.cancel()
        
        try {
            floatingNotepadView?.let { windowManager?.removeView(it) }
            floatingBubbleView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
