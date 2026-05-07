package com.n0tez.app.editor.ui.canvas

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.n0tez.app.photoeditor.DrawingOverlayView

/**
 * A reusable canvas container for image preview + overlay drawing.
 *
 * This is the UI foundation for the editor refactor, decoupling the canvas from any specific
 * Activity layout.
 */
class EditorCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    val imageView: ImageView = ImageView(context).apply {
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        scaleType = ImageView.ScaleType.FIT_CENTER
        adjustViewBounds = true
    }

    val drawingOverlayView: DrawingOverlayView = DrawingOverlayView(context).apply {
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        imageView = this@EditorCanvasView.imageView
    }

    init {
        clipToPadding = false
        addView(imageView)
        addView(drawingOverlayView)
    }

    fun setPreviewBitmap(bitmap: Bitmap?) {
        imageView.setImageBitmap(bitmap)
    }
}

