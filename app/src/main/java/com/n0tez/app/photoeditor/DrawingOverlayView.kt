package com.n0tez.app.photoeditor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import kotlin.math.max

class DrawingOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    var imageView: ImageView? = null
        set(value) {
            field = value
            mapper = value?.let { ImageViewBitmapMapper(it) }
        }

    var brushColor: Int = 0xFFFFFFFF.toInt()
        set(value) {
            field = value
            invalidate()
        }

    var brushWidthPx: Float = 14f
        set(value) {
            field = max(1f, value)
            invalidate()
        }

    var isDrawingEnabled: Boolean = false
        set(value) {
            field = value
            if (!value) currentStrokeId = null
        }

    private var mapper: ImageViewBitmapMapper? = null
    private val strokePoints = ArrayList<PointF>(2048)
    private var currentStrokeId: String? = null
    private val strokesInternal = ArrayList<Stroke>()

    val strokes: List<Stroke> get() = strokesInternal.toList()

    fun clear() {
        strokesInternal.clear()
        currentStrokeId = null
        strokePoints.clear()
        invalidate()
    }

    fun undo() {
        if (strokesInternal.isNotEmpty()) {
            strokesInternal.removeAt(strokesInternal.lastIndex)
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDrawingEnabled) return false
        val map = mapper ?: return false
        val p = map.viewToBitmap(event.x, event.y) ?: return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                currentStrokeId = BitmapProcessor.newId()
                strokePoints.clear()
                strokePoints.add(p)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (currentStrokeId == null) return false
                val history = event.historySize
                for (i in 0 until history) {
                    val hp = map.viewToBitmap(event.getHistoricalX(i), event.getHistoricalY(i))
                    if (hp != null) strokePoints.add(hp)
                }
                strokePoints.add(p)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val id = currentStrokeId ?: return false
                if (strokePoints.size >= 2) {
                    strokesInternal.add(
                        Stroke(
                            id = id,
                            color = brushColor,
                            widthPx = brushWidthPx,
                            points = strokePoints.toList(),
                        ),
                    )
                }
                currentStrokeId = null
                strokePoints.clear()
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
            else -> return false
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val map = mapper ?: return

        for (stroke in strokesInternal) {
            drawStroke(canvas, map, stroke)
        }
        currentStrokeId?.let {
            if (strokePoints.size >= 2) {
                drawStroke(
                    canvas,
                    map,
                    Stroke(
                        id = it,
                        color = brushColor,
                        widthPx = brushWidthPx,
                        points = strokePoints,
                    ),
                )
            }
        }
    }

    private fun drawStroke(canvas: Canvas, map: ImageViewBitmapMapper, stroke: Stroke) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = stroke.widthPx
            color = stroke.color
            alpha = (stroke.alpha * 255f).toInt().coerceIn(0, 255)
        }
        val path = Path()
        val first = stroke.points.firstOrNull() ?: return
        val fp = map.bitmapToView(first.x, first.y) ?: return
        path.moveTo(fp.x, fp.y)
        for (pt in stroke.points.drop(1)) {
            val vp = map.bitmapToView(pt.x, pt.y) ?: continue
            path.lineTo(vp.x, vp.y)
        }
        canvas.drawPath(path, paint)
    }
}
