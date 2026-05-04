package com.n0tez.app

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
import android.graphics.Rect
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
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
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
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
    private var notepadWidth = 320
    private var notepadHeight = WindowManager.LayoutParams.WRAP_CONTENT
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var autoSaveJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private val hideBubbleRunnable = Runnable {
        floatingBubbleView?.animate()?.alpha(0.3f)?.setDuration(300)?.start()
    }
    
    internal var testNotepadOpened = false

    companion object {
        const val ACTION_STOP = "com.n0tez.app.STOP_WIDGET"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "n0tez_widget_channel"
        private const val PREFS_NAME = "faceshot_buildingblock_prefs"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        noteRepository = NoteRepository(this)
        
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        transparencyLevel = prefs.getInt("widget_transparency", 20)
        notepadWidth = prefs.getInt("notepad_width", 320)
        notepadHeight = prefs.getInt("notepad_height", WindowManager.LayoutParams.WRAP_CONTENT)
        
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
                dpToPx(notepadWidth),
                if (notepadHeight == WindowManager.LayoutParams.WRAP_CONTENT) WindowManager.LayoutParams.WRAP_CONTENT else dpToPx(notepadHeight),
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
                captureVisibleTextIntoNote(notepadEditText)
            }

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

    private fun setupNotepadDrag(dragView: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        dragView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = notepadParams?.x ?: 0
                    initialY = notepadParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    notepadParams?.x = initialX + (event.rawX - initialTouchX).toInt()
                    notepadParams?.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager?.updateViewLayout(floatingNotepadView, notepadParams)
                    } catch (e: Exception) {
                        logInternal("error_update_layout_notepad", e)
                    }
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
        var resized = false

        resizeButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialWidth = currentNotepadWidthPx()
                    initialHeight = currentNotepadHeightPx()
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    resized = false
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (resized) {
                        saveResizedNotepadDimensions()
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    
                    val newWidth = (initialWidth + deltaX).toInt().coerceIn(dpToPx(200), dpToPx(800))
                    val newHeight = (initialHeight + deltaY).toInt().coerceIn(dpToPx(200), dpToPx(1200))

                    notepadParams?.width = newWidth
                    notepadParams?.height = newHeight
                    resized = true
                    
                    try {
                        windowManager?.updateViewLayout(floatingNotepadView, notepadParams)
                    } catch (e: Exception) {
                        logInternal("error_update_layout_resize", e)
                    }
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
            else -> dpToPx(notepadWidth.coerceAtLeast(200))
        }
    }

    private fun currentNotepadHeightPx(): Int {
        val measuredHeight = floatingNotepadView?.height ?: 0
        val paramHeight = notepadParams?.height ?: 0
        return when {
            measuredHeight > 0 -> measuredHeight
            paramHeight > 0 -> paramHeight
            else -> dpToPx(320)
        }
    }

    private fun saveResizedNotepadDimensions() {
        val widthPx = notepadParams?.width?.takeIf { it > 0 } ?: currentNotepadWidthPx()
        val heightPx = notepadParams?.height?.takeIf { it > 0 } ?: currentNotepadHeightPx()

        notepadWidth = pxToDp(widthPx).coerceAtLeast(200)
        notepadHeight = pxToDp(heightPx).coerceAtLeast(200)

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
        if (!TextCaptureAccessibilityService.isEnabled(this)) {
            openAccessibilitySettings()
            Toast.makeText(
                this,
                getString(R.string.accessibility_permission_required),
                Toast.LENGTH_LONG,
            ).show()
            return
        }

        val captureBounds = buildCaptureBounds() ?: run {
            logInternal("capture_bounds_missing")
            Toast.makeText(this, R.string.accessibility_capture_no_text, Toast.LENGTH_SHORT).show()
            return
        }

        val capturedText = TextCaptureAccessibilityService.captureTextInRegion(
            targetBounds = captureBounds,
            excludedPackageName = packageName,
        )

        if (capturedText.isBlank()) {
            logInternal(
                "capture_empty:${TextCaptureAccessibilityService.latestObservedPackageName().orEmpty()}",
            )
            Toast.makeText(this, R.string.accessibility_capture_no_text, Toast.LENGTH_SHORT).show()
            return
        }

        insertCapturedText(editText, capturedText)
        saveCurrentNote()
        logInternal("capture_success:${capturedText.length}")
        Toast.makeText(this, R.string.accessibility_capture_success, Toast.LENGTH_SHORT).show()
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

    private fun openAccessibilitySettings() {
        Toast.makeText(this, R.string.accessibility_enable_instructions, Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
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
            bm
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
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        saveCurrentNote()
        autoSaveJob?.cancel()
        serviceScope.cancel()
        
        try {
            floatingNotepadView?.let { windowManager?.removeView(it) }
            floatingBubbleView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
