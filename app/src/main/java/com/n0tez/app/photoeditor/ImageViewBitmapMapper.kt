package com.n0tez.app.photoeditor

import android.graphics.Matrix
import android.graphics.RectF
import android.widget.ImageView

class ImageViewBitmapMapper(
    private val imageView: ImageView,
) {
    fun bitmapRectInView(): RectF? {
        val d = imageView.drawable ?: return null
        val imageRect = RectF(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
        val out = RectF()
        imageView.imageMatrix.mapRect(out, imageRect)
        out.offset(imageView.paddingLeft.toFloat(), imageView.paddingTop.toFloat())
        return out
    }

    fun viewToBitmap(x: Float, y: Float): PointF? {
        val d = imageView.drawable ?: return null
        val m = Matrix(imageView.imageMatrix)
        val inv = Matrix()
        if (!m.invert(inv)) return null
        val pts = floatArrayOf(x - imageView.paddingLeft, y - imageView.paddingTop)
        inv.mapPoints(pts)
        val bx = pts[0].coerceIn(0f, (d.intrinsicWidth - 1).toFloat())
        val by = pts[1].coerceIn(0f, (d.intrinsicHeight - 1).toFloat())
        return PointF(bx, by)
    }

    fun bitmapToView(x: Float, y: Float): PointF? {
        val d = imageView.drawable ?: return null
        val pts = floatArrayOf(
            x.coerceIn(0f, (d.intrinsicWidth - 1).toFloat()),
            y.coerceIn(0f, (d.intrinsicHeight - 1).toFloat()),
        )
        val m = Matrix(imageView.imageMatrix)
        m.mapPoints(pts)
        return PointF(pts[0] + imageView.paddingLeft, pts[1] + imageView.paddingTop)
    }
}
