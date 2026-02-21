package com.n0tez.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.n0tez.app.databinding.ActivityPhotoEditorBinding
import com.n0tez.app.photoeditor.Adjustments
import com.n0tez.app.photoeditor.BitmapProcessor
import com.n0tez.app.photoeditor.FilterPreset
import com.n0tez.app.photoeditor.ImageViewBitmapMapper
import com.n0tez.app.photoeditor.NativePhotoEditor
import com.n0tez.app.photoeditor.PhotoEditorState
import com.n0tez.app.photoeditor.StickerOverlay
import com.n0tez.app.photoeditor.TextOverlay
import com.n0tez.app.photoeditor.TransformTouchListener
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class PhotoEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoEditorBinding
    private val logTag = "PhotoEditorActivity"
    private var originalBitmap: Bitmap? = null
    private var editedBitmap: Bitmap? = null
    private var imageUri: Uri? = null
    private var quality = 90
    private var rotation = 0f
    private var previewJob: Job? = null
    private var previewBitmap: Bitmap? = null
    private var adjustments = Adjustments()
    private var filterPreset: FilterPreset = FilterPreset.None
    private val transformTouchListener = TransformTouchListener()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = it
            loadImage(it)
        }
    }

    private val pickStickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { addStickerOverlay(it) }
    }

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                loadImage(it)
                Toast.makeText(this, "Image cropped successfully", Toast.LENGTH_SHORT).show()
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(this, "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()

        val imagePath = intent.getStringExtra("IMAGE_FILE_PATH")
        imageUri = intent.data

        if (imageUri != null) {
            loadImage(imageUri!!)
        } else if (imagePath != null) {
            loadImageFromPath(imagePath)
        } else {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Photo Editor"

        binding.btnSelectImage.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnRotateLeft.setOnClickListener { rotateImage(-90f) }
        binding.btnRotateRight.setOnClickListener { rotateImage(90f) }
        binding.btnFlipHorizontal.setOnClickListener { flipImage(true) }
        binding.btnFlipVertical.setOnClickListener { flipImage(false) }
        binding.btnCrop.setOnClickListener { cropImage() }

        binding.drawingView.imageView = binding.imageView
        binding.btnAdjust.setOnClickListener { showAdjustmentsDialog() }
        binding.btnFilters.setOnClickListener { showFiltersDialog() }
        binding.btnDraw.setOnClickListener { toggleDrawMode() }
        binding.btnUndo.setOnClickListener { undoLastEdit() }
        binding.btnAddText.setOnClickListener { showAddTextDialog() }
        binding.btnAddSticker.setOnClickListener { pickStickerLauncher.launch("image/*") }
        binding.btnAiCutout.setOnClickListener { runAiCutout() }
        binding.btnRemove.setOnClickListener { runObjectRemoval() }
        
        binding.seekBarQuality.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                quality = progress
                binding.tvQuality.text = "Quality: $quality%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnSave.setOnClickListener { saveEditedImage() }
        binding.btnShare.setOnClickListener { shareEditedImage() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.tvImageInfo.setOnClickListener { showResolutionDialog() }
        
        binding.tvQuality.text = "Quality: $quality%"
    }

    private fun loadImage(uri: Uri) {
        try {
            // Check mime type
            val mimeType = contentResolver.getType(uri)
            if (mimeType != null && !mimeType.startsWith("image/")) {
                Toast.makeText(this, "Unsupported file type: $mimeType", Toast.LENGTH_SHORT).show()
                return
            }

            // First decode with inJustDecodeBounds=true to check dimensions
            var options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it, null, options)
            }

            // Calculate inSampleSize
            options.inJustDecodeBounds = false
            options.inSampleSize = calculateInSampleSize(options, 2048, 2048)

            // Decode bitmap with inSampleSize
            contentResolver.openInputStream(uri)?.use { inputStream ->
                releaseBitmaps()
                originalBitmap = BitmapFactory.decodeStream(inputStream, null, options)
                if (originalBitmap == null) throw IOException("Failed to decode bitmap")
                
                editedBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                imageUri = uri
                clearOverlays()
                rotation = 0f
                displayImage()
                Toast.makeText(this, "Image loaded", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            Toast.makeText(this, "Out of memory! Try a smaller image.", Toast.LENGTH_LONG).show()
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun loadImageFromPath(path: String) {
        try {
            val file = File(path)
            if (!file.exists()) {
                 Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
                 return
            }
            
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, options)
            
            options.inJustDecodeBounds = false
            options.inSampleSize = calculateInSampleSize(options, 2048, 2048)
            
            releaseBitmaps()
            originalBitmap = BitmapFactory.decodeFile(path, options)
            if (originalBitmap == null) throw IOException("Failed to decode bitmap")
            
            editedBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)
            imageUri = Uri.fromFile(file)
            clearOverlays()
            rotation = 0f
            displayImage()
            Toast.makeText(this, "Image loaded", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            Toast.makeText(this, "Out of memory!", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun displayImage() {
        editedBitmap?.let {
            binding.imageView.setImageBitmap(it)
            val sizeKB = it.byteCount / 1024
            binding.tvImageInfo.text = "📐 ${it.width}x${it.height} | ~$sizeKB KB (Tap to resize)"
            applyPreviewDebounced()
        }
    }

    private fun rotateImage(degrees: Float) {
        editedBitmap?.let {
            rotation = (rotation + degrees) % 360
            val matrix = Matrix()
            matrix.postRotate(degrees)
            editedBitmap = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
            displayImage()
        }
    }

    private fun flipImage(horizontal: Boolean) {
        editedBitmap?.let {
            val matrix = Matrix()
            if (horizontal) {
                matrix.postScale(-1f, 1f, it.width / 2f, it.height / 2f)
            } else {
                matrix.postScale(1f, -1f, it.width / 2f, it.height / 2f)
            }
            editedBitmap = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
            displayImage()
        }
    }

    private fun cropImage() {
        editedBitmap?.let {
            try {
                val tempFile = File(cacheDir, "temp_crop_${System.currentTimeMillis()}.jpg")
                val outputFile = File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
                
                FileOutputStream(tempFile).use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
                
                val options = UCrop.Options().apply {
                    setFreeStyleCropEnabled(true)
                    setShowCropGrid(true)
                    setCompressionQuality(100)
                    // Set colors if possible, skipped for brevity
                }
                
                val uCropIntent = UCrop.of(Uri.fromFile(tempFile), Uri.fromFile(outputFile))
                    .withOptions(options)
                    .withMaxResultSize(4000, 4000)
                    .getIntent(this)
                
                cropLauncher.launch(uCropIntent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to start crop: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Please load an image first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResolutionDialog() {
        editedBitmap?.let { bitmap ->
            val resolutions = arrayOf(
                "Keep Original (${bitmap.width}x${bitmap.height})",
                "1920x1080 (Full HD)",
                "1280x720 (HD)",
                "854x480 (SD)",
                "640x480 (VGA)",
                "Custom Resolution"
            )
            
            AlertDialog.Builder(this)
                .setTitle("Change Image Resolution")
                .setItems(resolutions) { _, which ->
                    when (which) {
                        0 -> Toast.makeText(this, "Keeping original resolution", Toast.LENGTH_SHORT).show()
                        1 -> resizeImage(1920, 1080)
                        2 -> resizeImage(1280, 720)
                        3 -> resizeImage(854, 480)
                        4 -> resizeImage(640, 480)
                        5 -> showCustomResolutionDialog()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } ?: run {
            Toast.makeText(this, "Please load an image first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCustomResolutionDialog() {
        editedBitmap?.let { bitmap ->
            val widthInput = EditText(this).apply {
                hint = "Width (px)"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText(bitmap.width.toString())
            }
            val heightInput = EditText(this).apply {
                hint = "Height (px)"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText(bitmap.height.toString())
            }
            
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 20, 50, 20)
                addView(widthInput)
                addView(heightInput)
            }
            
            AlertDialog.Builder(this)
                .setTitle("Custom Resolution")
                .setView(layout)
                .setPositiveButton("Apply") { _, _ ->
                    val width = widthInput.text.toString().toIntOrNull() ?: bitmap.width
                    val height = heightInput.text.toString().toIntOrNull() ?: bitmap.height
                    resizeImage(width, height)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun resizeImage(newWidth: Int, newHeight: Int) {
        editedBitmap?.let {
            try {
                editedBitmap = Bitmap.createScaledBitmap(it, newWidth, newHeight, true)
                displayImage()
                Toast.makeText(this, "Image resized to ${newWidth}x$newHeight", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(logTag, "Resize failed: ${e.message}", e)
                Toast.makeText(this, "Failed to resize: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveEditedImage() {
        val base = editedBitmap
        if (base == null) {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val state = PhotoEditorState(
                    adjustments = adjustments,
                    filter = filterPreset,
                    overlays = collectOverlaysFromViews(),
                    strokes = binding.drawingView.strokes,
                )
                val rendered = BitmapProcessor.renderFinalBitmap(contentResolver, base, state)
                saveBitmapToAppStorage(rendered)
                if (rendered != base) rendered.recycle()
                Toast.makeText(this@PhotoEditorActivity, "Image saved", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Log.e(logTag, "Failed to save edited image", e)
                Toast.makeText(this@PhotoEditorActivity, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareEditedImage() {
        val base = editedBitmap
        if (base == null) {
            Toast.makeText(this, "No image to share", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val state = PhotoEditorState(
                    adjustments = adjustments,
                    filter = filterPreset,
                    overlays = collectOverlaysFromViews(),
                    strokes = binding.drawingView.strokes,
                )
                val rendered = BitmapProcessor.renderFinalBitmap(contentResolver, base, state)
                shareBitmap(rendered)
                if (rendered != base) rendered.recycle()
            } catch (e: Exception) {
                Log.e(logTag, "Failed to share edited image", e)
                Toast.makeText(this@PhotoEditorActivity, "Failed to share image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveBitmapToAppStorage(bitmap: Bitmap) {
        val imagesDir = File(filesDir, "media/images")
        if (!imagesDir.exists()) imagesDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFile = File(imagesDir, "edited_$timestamp.jpg")

        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
    }

    private fun shareBitmap(bitmap: Bitmap) {
        val cachePath = File(cacheDir, "shared_images")
        cachePath.mkdirs()
        val file = File(cachePath, "shared_image_${System.currentTimeMillis()}.jpg")

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }

        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Image"))
    }

    private fun applyPreviewDebounced() {
        val base = editedBitmap ?: return
        previewJob?.cancel()
        previewJob = lifecycleScope.launch {
            delay(80)
            try {
                val state = PhotoEditorState(adjustments = adjustments, filter = filterPreset)
                val rendered = BitmapProcessor.renderFinalBitmap(
                    contentResolver = contentResolver,
                    source = base,
                    state = state,
                    outputMaxSize = 1280,
                    seed = 0,
                )
                val previous = previewBitmap
                previewBitmap = rendered
                binding.imageView.setImageBitmap(rendered)
                if (previous != null && previous != base && previous != rendered) {
                    previous.recycle()
                }
            } catch (e: Exception) {
                Log.e(logTag, "Preview failed: ${e.message}", e)
                binding.imageView.setImageBitmap(base)
            }
        }
    }

    private fun toggleDrawMode() {
        val enabled = !binding.drawingView.isDrawingEnabled
        binding.drawingView.isDrawingEnabled = enabled
        if (enabled) showDrawSettingsDialog()
        Toast.makeText(this, if (enabled) "Draw enabled" else "Draw disabled", Toast.LENGTH_SHORT).show()
    }

    private fun showDrawSettingsDialog() {
        val sizes = arrayOf("Small", "Medium", "Large")
        val sizeValues = floatArrayOf(8f, 14f, 22f)
        AlertDialog.Builder(this)
            .setTitle("Brush Size")
            .setItems(sizes) { _, which ->
                binding.drawingView.brushWidthPx = sizeValues.getOrElse(which) { 14f }
                showBrushColorDialog()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBrushColorDialog() {
        val labels = arrayOf("White", "Black", "Red", "Green", "Blue", "Yellow")
        val colors = intArrayOf(
            0xFFFFFFFF.toInt(),
            0xFF000000.toInt(),
            0xFFFF1744.toInt(),
            0xFF00C853.toInt(),
            0xFF2979FF.toInt(),
            0xFFFFD600.toInt(),
        )
        AlertDialog.Builder(this)
            .setTitle("Brush Color")
            .setItems(labels) { _, which ->
                binding.drawingView.brushColor = colors.getOrElse(which) { 0xFFFFFFFF.toInt() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun undoLastEdit() {
        if (binding.drawingView.strokes.isNotEmpty()) {
            binding.drawingView.undo()
            return
        }
        val container = binding.editorContainer
        val lastOverlay = (0 until container.childCount)
            .map { container.getChildAt(it) }
            .filterNot { it.id == binding.imageView.id || it.id == binding.drawingView.id }
            .lastOrNull()
        if (lastOverlay != null) {
            container.removeView(lastOverlay)
            return
        }
        Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show()
    }

    private fun runAiCutout() {
        val base = editedBitmap
        if (base == null) {
            Toast.makeText(this, "Load an image first", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val mask = createSimpleMask(base)
                val cutout = if (NativePhotoEditor.isAvailable()) {
                    NativePhotoEditor.inpaintWithMask(base, mask)
                } else {
                    Log.w(logTag, "Native cutout unavailable, using fallback")
                    BitmapProcessor.inpaintWithMask(base, mask)
                }
                updateEditedBitmap(cutout)
            } catch (e: Exception) {
                Toast.makeText(this@PhotoEditorActivity, "AI cutout failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun runObjectRemoval() {
        val base = editedBitmap
        if (base == null) {
            Toast.makeText(this, "Load an image first", Toast.LENGTH_SHORT).show()
            return
        }
        if (binding.drawingView.strokes.isEmpty()) {
            Toast.makeText(this, "Draw over the area to remove", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val mask = createStrokeMask(base)
                val removed = if (NativePhotoEditor.isAvailable()) {
                    NativePhotoEditor.inpaintWithMask(base, mask)
                } else {
                    Log.w(logTag, "Native removal unavailable, using fallback")
                    BitmapProcessor.inpaintWithMask(base, mask)
                }
                binding.drawingView.clear()
                updateEditedBitmap(removed)
            } catch (e: Exception) {
                Toast.makeText(this@PhotoEditorActivity, "Removal failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateEditedBitmap(bitmap: Bitmap) {
        val previous = editedBitmap
        editedBitmap = bitmap
        previewBitmap?.let { if (it != previous && it != bitmap) it.recycle() }
        previewBitmap = null
        binding.imageView.setImageBitmap(bitmap)
    }

    private fun releaseBitmaps() {
        previewBitmap?.let { if (it != editedBitmap) it.recycle() }
        previewBitmap = null
        editedBitmap?.recycle()
        originalBitmap?.recycle()
        editedBitmap = null
        originalBitmap = null
    }

    private fun createStrokeMask(base: Bitmap): Bitmap {
        val mask = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(mask)
        canvas.drawColor(0x00000000)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            color = 0xFFFFFFFF.toInt()
        }
        val mapper = ImageViewBitmapMapper(binding.imageView)
        for (stroke in binding.drawingView.strokes) {
            paint.strokeWidth = stroke.widthPx * 2f
            val path = android.graphics.Path()
            val first = stroke.points.firstOrNull() ?: continue
            val fp = mapper.bitmapToView(first.x, first.y) ?: continue
            path.moveTo(fp.x, fp.y)
            for (pt in stroke.points.drop(1)) {
                val vp = mapper.bitmapToView(pt.x, pt.y) ?: continue
                path.lineTo(vp.x, vp.y)
            }
            canvas.drawPath(path, paint)
        }
        return mask
    }

    private fun createSimpleMask(base: Bitmap): Bitmap {
        val mask = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(mask)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
        }
        val cx = base.width / 2f
        val cy = base.height / 2f
        val r = (minOf(base.width, base.height) * 0.25f)
        canvas.drawCircle(cx, cy, r, paint)
        return mask
    }
    private fun showAddTextDialog() {
        val input = EditText(this).apply { hint = "Enter text" }
        val colors = arrayOf("White", "Black", "Red", "Green", "Blue", "Yellow")
        val colorValues = intArrayOf(
            0xFFFFFFFF.toInt(),
            0xFF000000.toInt(),
            0xFFFF1744.toInt(),
            0xFF00C853.toInt(),
            0xFF2979FF.toInt(),
            0xFFFFD600.toInt(),
        )
        AlertDialog.Builder(this)
            .setTitle("Add Text")
            .setView(input)
            .setPositiveButton("Next") { _, _ ->
                val text = input.text?.toString()?.trim().orEmpty()
                if (text.isBlank()) {
                    Toast.makeText(this, "Text cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                AlertDialog.Builder(this)
                    .setTitle("Text Color")
                    .setItems(colors) { _, which ->
                        addTextOverlay(text, colorValues.getOrElse(which) { 0xFFFFFFFF.toInt() })
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addTextOverlay(text: String, color: Int) {
        val tv = TextView(this).apply {
            this.text = text
            setTextColor(color)
            textSize = 24f
            tag = BitmapProcessor.newId()
        }
        tv.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        )
        binding.editorContainer.addView(tv)
        tv.post {
            val (cx, cy) = centerPointInContainer()
            tv.x = cx - tv.width / 2f
            tv.y = cy - tv.height / 2f
            tv.setOnTouchListener(transformTouchListener)
        }
    }

    private fun addStickerOverlay(uri: Uri) {
        val iv = ImageView(this).apply {
            setImageURI(uri)
            scaleType = ImageView.ScaleType.FIT_CENTER
            tag = uri.toString()
        }
        val sizePx = (resources.displayMetrics.density * 120f).toInt()
        iv.layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
        binding.editorContainer.addView(iv)
        iv.post {
            val (cx, cy) = centerPointInContainer()
            iv.x = cx - iv.width / 2f
            iv.y = cy - iv.height / 2f
            iv.setOnTouchListener(transformTouchListener)
        }
    }

    private fun centerPointInContainer(): Pair<Float, Float> {
        val c = binding.editorContainer
        return Pair(c.width / 2f, c.height / 2f)
    }

    private fun showAdjustmentsDialog() {
        val items = arrayOf("Brightness", "Contrast", "Saturation", "Warmth", "Tint", "Vignette", "Grain", "Sharpen", "Blur", "Reset")
        AlertDialog.Builder(this)
            .setTitle("Adjustments")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showSliderDialog("Brightness", adjustments.brightness, -1f, 1f) { v -> adjustments = adjustments.copy(brightness = v); applyPreviewDebounced() }
                    1 -> showSliderDialog("Contrast", adjustments.contrast, 0f, 2f) { v -> adjustments = adjustments.copy(contrast = v); applyPreviewDebounced() }
                    2 -> showSliderDialog("Saturation", adjustments.saturation, 0f, 2f) { v -> adjustments = adjustments.copy(saturation = v); applyPreviewDebounced() }
                    3 -> showSliderDialog("Warmth", adjustments.warmth, -1f, 1f) { v -> adjustments = adjustments.copy(warmth = v); applyPreviewDebounced() }
                    4 -> showSliderDialog("Tint", adjustments.tint, -1f, 1f) { v -> adjustments = adjustments.copy(tint = v); applyPreviewDebounced() }
                    5 -> showSliderDialog("Vignette", adjustments.vignette, 0f, 1f) { v -> adjustments = adjustments.copy(vignette = v); applyPreviewDebounced() }
                    6 -> showSliderDialog("Grain", adjustments.grain, 0f, 1f) { v -> adjustments = adjustments.copy(grain = v); applyPreviewDebounced() }
                    7 -> showSliderDialog("Sharpen", adjustments.sharpen, 0f, 1f) { v -> adjustments = adjustments.copy(sharpen = v); applyPreviewDebounced() }
                    8 -> showSliderDialog("Blur", adjustments.blur, 0f, 1f) { v -> adjustments = adjustments.copy(blur = v); applyPreviewDebounced() }
                    9 -> {
                        adjustments = Adjustments()
                        filterPreset = FilterPreset.None
                        applyPreviewDebounced()
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showSliderDialog(title: String, current: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
        val seekBar = SeekBar(this).apply { this.max = 1000 }
        val label = TextView(this).apply { text = "$title: ${"%.2f".format(current)}" }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
            addView(label)
            addView(seekBar)
        }
        fun progressToValue(p: Int): Float {
            val t = p / 1000f
            return (min + (max - min) * t)
        }
        fun valueToProgress(v: Float): Int {
            val t = ((v - min) / (max - min)).coerceIn(0f, 1f)
            return (t * 1000f).toInt()
        }
        seekBar.progress = valueToProgress(current)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val v = progressToValue(progress)
                label.text = "$title: ${"%.2f".format(v)}"
                if (fromUser) onChange(v)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton("Done", null)
            .show()
    }

    private fun showFiltersDialog() {
        val items = arrayOf("None", "Vintage", "Warm", "Cool", "B&W", "Sepia")
        AlertDialog.Builder(this)
            .setTitle("Filters")
            .setItems(items) { _, which ->
                filterPreset = when (which) {
                    1 -> FilterPreset.Vintage
                    2 -> FilterPreset.Warm
                    3 -> FilterPreset.Cool
                    4 -> FilterPreset.BlackAndWhite
                    5 -> FilterPreset.Sepia
                    else -> FilterPreset.None
                }
                applyPreviewDebounced()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun clearOverlays() {
        binding.drawingView.clear()
        val container = binding.editorContainer
        val toRemove = (0 until container.childCount)
            .map { container.getChildAt(it) }
            .filterNot { it.id == binding.imageView.id || it.id == binding.drawingView.id }
        for (v in toRemove) container.removeView(v)
        adjustments = Adjustments()
        filterPreset = FilterPreset.None
    }

    private fun collectOverlaysFromViews(): List<com.n0tez.app.photoeditor.OverlayElement> {
        val container = binding.editorContainer
        val mapper = ImageViewBitmapMapper(binding.imageView)
        val overlays = ArrayList<com.n0tez.app.photoeditor.OverlayElement>()

        for (i in 0 until container.childCount) {
            val v = container.getChildAt(i)
            if (v.id == binding.imageView.id || v.id == binding.drawingView.id) continue
            val centerX = v.x + (v.width * v.scaleX) / 2f
            val centerY = v.y + (v.height * v.scaleY) / 2f
            val center = mapper.viewToBitmap(centerX, centerY) ?: continue

            when (v) {
                is TextView -> {
                    val pxPerViewPx = computeBitmapScaleX(mapper, centerX, centerY)
                    val textSizePx = v.textSize * pxPerViewPx
                    overlays.add(
                        TextOverlay(
                            id = (v.tag as? String) ?: BitmapProcessor.newId(),
                            centerX = center.x,
                            centerY = center.y,
                            rotationDegrees = v.rotation,
                            scale = v.scaleX,
                            alpha = v.alpha,
                            text = v.text?.toString().orEmpty(),
                            color = v.currentTextColor,
                            textSizePx = textSizePx,
                        ),
                    )
                }
                is ImageView -> {
                    val uri = runCatching { Uri.parse(v.tag?.toString().orEmpty()) }.getOrNull() ?: continue
                    val intrinsic = v.drawable?.intrinsicWidth?.takeIf { it > 0 } ?: 1
                    val widthInBitmap = computeBitmapWidth(mapper, v)
                    val scale = (widthInBitmap / intrinsic.toFloat()).coerceAtLeast(0.001f)
                    overlays.add(
                        StickerOverlay(
                            id = BitmapProcessor.newId(),
                            centerX = center.x,
                            centerY = center.y,
                            rotationDegrees = v.rotation,
                            scale = scale,
                            alpha = v.alpha,
                            uri = uri,
                        ),
                    )
                }
            }
        }

        return overlays
    }

    private fun computeBitmapScaleX(mapper: ImageViewBitmapMapper, x: Float, y: Float): Float {
        val p0 = mapper.viewToBitmap(x, y) ?: return 1f
        val p1 = mapper.viewToBitmap(x + 1f, y) ?: return 1f
        val dx = kotlin.math.abs(p1.x - p0.x)
        return if (dx <= 0f) 1f else dx
    }

    private fun computeBitmapWidth(mapper: ImageViewBitmapMapper, v: View): Float {
        val centerY = v.y + (v.height * v.scaleY) / 2f
        val leftX = v.x + v.width / 2f - (v.width * v.scaleX) / 2f
        val rightX = v.x + v.width / 2f + (v.width * v.scaleX) / 2f
        val l = mapper.viewToBitmap(leftX, centerY) ?: return v.width.toFloat()
        val r = mapper.viewToBitmap(rightX, centerY) ?: return v.width.toFloat()
        return kotlin.math.abs(r.x - l.x).coerceAtLeast(1f)
    }

    override fun onDestroy() {
        super.onDestroy()
        previewJob?.cancel()
        releaseBitmaps()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
