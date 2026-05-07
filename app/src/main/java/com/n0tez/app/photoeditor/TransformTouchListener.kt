package com.n0tez.app.photoeditor

import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.hypot

class TransformTouchListener(
    private val onTransformChanged: (() -> Unit)? = null,
) : View.OnTouchListener {
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private val last = PointF()

    private var isTwoFinger = false
    private var lastSpan = 0f
    private var lastAngle = 0f
    private val mid = PointF()

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                last.set(event.x, event.y)
                isTwoFinger = false
                v.parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    isTwoFinger = true
                    val p0 = getPoint(event, 0)
                    val p1 = getPoint(event, 1)
                    mid.set((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
                    lastSpan = distance(p0, p1)
                    lastAngle = angle(p0, p1)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isTwoFinger && event.pointerCount >= 2) {
                    val p0 = getPoint(event, 0)
                    val p1 = getPoint(event, 1)
                    val newMid = PointF((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
                    val dx = newMid.x - mid.x
                    val dy = newMid.y - mid.y
                    v.translationX += dx
                    v.translationY += dy
                    mid.set(newMid.x, newMid.y)

                    val span = distance(p0, p1)
                    if (lastSpan > 0f) {
                        val scale = span / lastSpan
                        v.scaleX *= scale
                        v.scaleY *= scale
                    }
                    lastSpan = span

                    val ang = angle(p0, p1)
                    val delta = ang - lastAngle
                    v.rotation += delta
                    lastAngle = ang

                    onTransformChanged?.invoke()
                    return true
                }

                val index = event.findPointerIndex(activePointerId)
                if (index >= 0) {
                    val x = event.getX(index)
                    val y = event.getY(index)
                    val dx = x - last.x
                    val dy = y - last.y
                    v.translationX += dx
                    v.translationY += dy
                    last.set(x, y)
                    onTransformChanged?.invoke()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                isTwoFinger = false
                v.parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 2) {
                    isTwoFinger = false
                    val newIndex = if (event.actionIndex == 0) 1 else 0
                    if (newIndex < event.pointerCount) {
                        activePointerId = event.getPointerId(newIndex)
                        last.set(event.getX(newIndex), event.getY(newIndex))
                    } else {
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                    }
                }
                return true
            }
        }
        return false
    }

    private fun getPoint(e: MotionEvent, i: Int): PointF = PointF(e.getX(i), e.getY(i))

    private fun distance(a: PointF, b: PointF): Float = hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()

    private fun angle(a: PointF, b: PointF): Float = (atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble()) * 180.0 / Math.PI).toFloat()
}
