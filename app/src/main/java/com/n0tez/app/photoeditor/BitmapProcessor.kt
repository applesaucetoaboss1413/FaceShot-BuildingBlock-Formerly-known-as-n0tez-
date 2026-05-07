package com.n0tez.app.photoeditor

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

object BitmapProcessor {
    suspend fun renderFinalBitmap(
        contentResolver: ContentResolver,
        source: Bitmap,
        state: PhotoEditorState,
        outputMaxSize: Int? = null,
        seed: Int = 0,
    ): Bitmap = withContext(Dispatchers.Default) {
        val base = if (outputMaxSize != null) {
            downscaleToMax(source, outputMaxSize)
        } else {
            if (source.config == Bitmap.Config.ARGB_8888) source.copy(Bitmap.Config.ARGB_8888, true)
            else source.copy(Bitmap.Config.ARGB_8888, true)
        }

        var working = base

        working = applyPreset(working, state.filter)
        working = applyAdjustments(working, state.adjustments, seed)
        state.lut?.let { lut ->
            val lutBitmap = decodeBitmap(contentResolver, lut.uri, 2048) ?: throw IOException("Failed to decode LUT bitmap")
            working = applyLut2d(working, lutBitmap, lut.intensity)
            lutBitmap.recycle()
        }

        if (state.strokes.isNotEmpty() || state.overlays.isNotEmpty()) {
            val canvas = Canvas(working)
            drawStrokes(canvas, state.strokes)
            drawOverlays(contentResolver, canvas, state.overlays)
        }

        working
    }

    fun newId(): String = UUID.randomUUID().toString()

    fun inpaintWithMask(base: Bitmap, mask: Bitmap): Bitmap {
        val w = base.width
        val h = base.height
        val basePixels = IntArray(w * h)
        val maskPixels = IntArray(w * h)
        base.getPixels(basePixels, 0, w, 0, 0, w, h)
        mask.getPixels(maskPixels, 0, w, 0, 0, w, h)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        for (i in basePixels.indices) {
            val mp = maskPixels[i]
            val maskAlpha = (mp ushr 24) and 0xFF
            if (maskAlpha > 24) {
                out.setPixel(i % w, i / w, 0x00000000)
            } else {
                out.setPixel(i % w, i / w, basePixels[i])
            }
        }
        return out
    }
    private fun downscaleToMax(source: Bitmap, maxSize: Int): Bitmap {
        val longest = max(source.width, source.height)
        if (longest <= maxSize) return source.copy(Bitmap.Config.ARGB_8888, true)
        val scale = maxSize.toFloat() / longest.toFloat()
        val w = max(1, (source.width * scale).roundToInt())
        val h = max(1, (source.height * scale).roundToInt())
        return Bitmap.createScaledBitmap(source, w, h, true).copy(Bitmap.Config.ARGB_8888, true)
    }

    private fun decodeBitmap(contentResolver: ContentResolver, uri: Uri, maxSize: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxSize, maxSize)
        val opts = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    private fun applyPreset(bitmap: Bitmap, preset: FilterPreset): Bitmap {
        return when (preset) {
            FilterPreset.None -> bitmap
            FilterPreset.BlackAndWhite -> applyAdjustments(bitmap, Adjustments(saturation = 0f), seed = 0)
            FilterPreset.Sepia -> applyColorMatrix(bitmap) { r, g, b ->
                val tr = (0.393f * r + 0.769f * g + 0.189f * b)
                val tg = (0.349f * r + 0.686f * g + 0.168f * b)
                val tb = (0.272f * r + 0.534f * g + 0.131f * b)
                floatArrayOf(tr, tg, tb)
            }
            FilterPreset.Vintage -> applyVintage(bitmap)
            FilterPreset.Warm -> applyAdjustments(bitmap, Adjustments(warmth = 0.25f, contrast = 1.05f, saturation = 1.05f), seed = 0)
            FilterPreset.Cool -> applyAdjustments(bitmap, Adjustments(warmth = -0.25f, contrast = 1.05f, saturation = 1.03f), seed = 0)
        }
    }

    private fun applyVintage(bitmap: Bitmap): Bitmap {
        val adjusted = applyAdjustments(bitmap, Adjustments(contrast = 1.06f, saturation = 0.95f, brightness = 0.02f), seed = 0)
        return applyColorMatrix(adjusted) { r, g, b ->
            val faded = 0.92f
            floatArrayOf(r * faded + 0.02f, g * faded + 0.015f, b * faded + 0.01f)
        }
    }

    private fun applyAdjustments(bitmap: Bitmap, adjustments: Adjustments, seed: Int): Bitmap {
        val hasMatrix = adjustments.brightness != 0f ||
            adjustments.contrast != 1f ||
            adjustments.saturation != 1f ||
            adjustments.warmth != 0f ||
            adjustments.tint != 0f
        val hasVignette = adjustments.vignette > 0f
        val hasGrain = adjustments.grain > 0f
        val hasSharpen = adjustments.sharpen > 0f
        val hasBlur = adjustments.blur > 0f

        var working = bitmap

        if (hasMatrix) {
            working = applyColorAdjustments(working, adjustments)
        }
        if (hasVignette) {
            working = applyVignette(working, adjustments.vignette)
        }
        if (hasGrain) {
            working = applyGrain(working, adjustments.grain, seed)
        }
        if (hasSharpen) {
            working = applySharpen(working, adjustments.sharpen)
        }
        if (hasBlur) {
            working = applyBlur(working, adjustments.blur)
        }

        return working
    }

    private fun applyColorAdjustments(bitmap: Bitmap, a: Adjustments): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val brightness = a.brightness.coerceIn(-1f, 1f)
        val contrast = a.contrast.coerceIn(0f, 3f)
        val saturation = a.saturation.coerceIn(0f, 3f)
        val warmth = a.warmth.coerceIn(-1f, 1f)
        val tint = a.tint.coerceIn(-1f, 1f)

        for (i in pixels.indices) {
            val c = pixels[i]
            val alpha = (c ushr 24) and 0xFF
            var r = ((c ushr 16) and 0xFF) / 255f
            var g = ((c ushr 8) and 0xFF) / 255f
            var b = (c and 0xFF) / 255f

            r = (r + brightness)
            g = (g + brightness)
            b = (b + brightness)

            r = (r - 0.5f) * contrast + 0.5f
            g = (g - 0.5f) * contrast + 0.5f
            b = (b - 0.5f) * contrast + 0.5f

            val luma = (0.2126f * r + 0.7152f * g + 0.0722f * b)
            r = luma + (r - luma) * saturation
            g = luma + (g - luma) * saturation
            b = luma + (b - luma) * saturation

            val warm = warmth * 0.1f
            r += warm
            b -= warm

            val magenta = tint * 0.1f
            g -= magenta
            r += magenta * 0.5f
            b += magenta * 0.5f

            r = r.coerceIn(0f, 1f)
            g = g.coerceIn(0f, 1f)
            b = b.coerceIn(0f, 1f)

            pixels[i] = (alpha shl 24) or
                ((r * 255f).roundToInt().coerceIn(0, 255) shl 16) or
                ((g * 255f).roundToInt().coerceIn(0, 255) shl 8) or
                ((b * 255f).roundToInt().coerceIn(0, 255))
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun applyVignette(bitmap: Bitmap, strength: Float): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val cx = w / 2f
        val cy = h / 2f
        val maxDist = sqrt(cx * cx + cy * cy)
        val s = strength.coerceIn(0f, 1f)

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val c = pixels[idx]
                val a = (c ushr 24) and 0xFF
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF

                val dx = x - cx
                val dy = y - cy
                val dist = sqrt(dx * dx + dy * dy)
                val t = (dist / maxDist).coerceIn(0f, 1f)
                val factor = (1f - s * t.pow(2.2f)).coerceIn(0f, 1f)
                pixels[idx] = (a shl 24) or
                    ((r * factor).roundToInt().coerceIn(0, 255) shl 16) or
                    ((g * factor).roundToInt().coerceIn(0, 255) shl 8) or
                    ((b * factor).roundToInt().coerceIn(0, 255))
            }
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun applyGrain(bitmap: Bitmap, strength: Float, seed: Int): Bitmap {
        val s = strength.coerceIn(0f, 1f)
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val rng = Random(seed)
        val amp = (s * 24f)
        for (i in pixels.indices) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            val n = (rng.nextFloat() * 2f - 1f) * amp
            pixels[i] = (a shl 24) or
                ((r + n).roundToInt().coerceIn(0, 255) shl 16) or
                ((g + n).roundToInt().coerceIn(0, 255) shl 8) or
                ((b + n).roundToInt().coerceIn(0, 255))
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun applyBlur(bitmap: Bitmap, strength: Float): Bitmap {
        val s = strength.coerceIn(0f, 1f)
        if (s <= 0f) return bitmap
        val radius = max(1, (s * 12f).roundToInt())
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        val tmp = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)

        val kernelSize = radius * 2 + 1
        for (y in 0 until h) {
            var rSum = 0
            var gSum = 0
            var bSum = 0
            var aSum = 0
            for (kx in -radius..radius) {
                val x = kx.coerceIn(0, w - 1)
                val c = src[y * w + x]
                aSum += (c ushr 24) and 0xFF
                rSum += (c ushr 16) and 0xFF
                gSum += (c ushr 8) and 0xFF
                bSum += c and 0xFF
            }
            for (x in 0 until w) {
                val idx = y * w + x
                tmp[idx] = ((aSum / kernelSize) shl 24) or
                    ((rSum / kernelSize) shl 16) or
                    ((gSum / kernelSize) shl 8) or
                    (bSum / kernelSize)

                val removeX = (x - radius).coerceIn(0, w - 1)
                val addX = (x + radius + 1).coerceIn(0, w - 1)
                val cRemove = src[y * w + removeX]
                val cAdd = src[y * w + addX]
                aSum += ((cAdd ushr 24) and 0xFF) - ((cRemove ushr 24) and 0xFF)
                rSum += ((cAdd ushr 16) and 0xFF) - ((cRemove ushr 16) and 0xFF)
                gSum += ((cAdd ushr 8) and 0xFF) - ((cRemove ushr 8) and 0xFF)
                bSum += (cAdd and 0xFF) - (cRemove and 0xFF)
            }
        }

        val out = IntArray(w * h)
        for (x in 0 until w) {
            var rSum = 0
            var gSum = 0
            var bSum = 0
            var aSum = 0
            for (ky in -radius..radius) {
                val y = ky.coerceIn(0, h - 1)
                val c = tmp[y * w + x]
                aSum += (c ushr 24) and 0xFF
                rSum += (c ushr 16) and 0xFF
                gSum += (c ushr 8) and 0xFF
                bSum += c and 0xFF
            }
            for (y in 0 until h) {
                val idx = y * w + x
                out[idx] = ((aSum / kernelSize) shl 24) or
                    ((rSum / kernelSize) shl 16) or
                    ((gSum / kernelSize) shl 8) or
                    (bSum / kernelSize)

                val removeY = (y - radius).coerceIn(0, h - 1)
                val addY = (y + radius + 1).coerceIn(0, h - 1)
                val cRemove = tmp[removeY * w + x]
                val cAdd = tmp[addY * w + x]
                aSum += ((cAdd ushr 24) and 0xFF) - ((cRemove ushr 24) and 0xFF)
                rSum += ((cAdd ushr 16) and 0xFF) - ((cRemove ushr 16) and 0xFF)
                gSum += ((cAdd ushr 8) and 0xFF) - ((cRemove ushr 8) and 0xFF)
                bSum += (cAdd and 0xFF) - (cRemove and 0xFF)
            }
        }

        val blurred = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        blurred.setPixels(out, 0, w, 0, 0, w, h)
        return blurred
    }
    private fun applySharpen(bitmap: Bitmap, strength: Float): Bitmap {
        val s = strength.coerceIn(0f, 1f)
        if (s == 0f) return bitmap
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        val dst = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        val center = 1f + 4f * s
        val edge = -s
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val c = src[idx]
                val a = (c ushr 24) and 0xFF

                fun sample(px: Int, py: Int): Int {
                    val sx = px.coerceIn(0, w - 1)
                    val sy = py.coerceIn(0, h - 1)
                    return src[sy * w + sx]
                }

                val c0 = sample(x, y)
                val c1 = sample(x - 1, y)
                val c2 = sample(x + 1, y)
                val c3 = sample(x, y - 1)
                val c4 = sample(x, y + 1)

                val r = clampToByte(
                    ((c0 ushr 16) and 0xFF) * center +
                        ((c1 ushr 16) and 0xFF) * edge +
                        ((c2 ushr 16) and 0xFF) * edge +
                        ((c3 ushr 16) and 0xFF) * edge +
                        ((c4 ushr 16) and 0xFF) * edge,
                )
                val g = clampToByte(
                    ((c0 ushr 8) and 0xFF) * center +
                        ((c1 ushr 8) and 0xFF) * edge +
                        ((c2 ushr 8) and 0xFF) * edge +
                        ((c3 ushr 8) and 0xFF) * edge +
                        ((c4 ushr 8) and 0xFF) * edge,
                )
                val b = clampToByte(
                    (c0 and 0xFF) * center +
                        (c1 and 0xFF) * edge +
                        (c2 and 0xFF) * edge +
                        (c3 and 0xFF) * edge +
                        (c4 and 0xFF) * edge,
                )
                dst[idx] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(dst, 0, w, 0, 0, w, h)
        return out
    }

    private fun clampToByte(v: Float): Int = v.roundToInt().coerceIn(0, 255)

    private fun applyColorMatrix(bitmap: Bitmap, transform: (r: Float, g: Float, b: Float) -> FloatArray): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF
            val r = ((c ushr 16) and 0xFF) / 255f
            val g = ((c ushr 8) and 0xFF) / 255f
            val b = (c and 0xFF) / 255f
            val out = transform(r, g, b)
            pixels[i] = (a shl 24) or
                ((out[0].coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255) shl 16) or
                ((out[1].coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255) shl 8) or
                ((out[2].coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255))
        }
        val outBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return outBitmap
    }

    private fun applyLut2d(source: Bitmap, lut: Bitmap, intensity: Float): Bitmap {
        val w = source.width
        val h = source.height
        val src = IntArray(w * h)
        source.getPixels(src, 0, w, 0, 0, w, h)

        val lutSize = lut.height
        if (lutSize <= 0) return source
        val lutWidth = lut.width
        val tiles = lutWidth / lutSize
        if (tiles <= 0) return source
        val lutPixels = IntArray(lutWidth * lutSize)
        lut.getPixels(lutPixels, 0, lutWidth, 0, 0, lutWidth, lutSize)

        val t = intensity.coerceIn(0f, 1f)
        for (i in src.indices) {
            val c = src[i]
            val a = (c ushr 24) and 0xFF
            val r = ((c ushr 16) and 0xFF) / 255f
            val g = ((c ushr 8) and 0xFF) / 255f
            val b = (c and 0xFF) / 255f

            val mapped = sampleLut2d(lutPixels, lutWidth, lutSize, tiles, r, g, b)
            val mr = (mapped[0] * 255f).roundToInt().coerceIn(0, 255)
            val mg = (mapped[1] * 255f).roundToInt().coerceIn(0, 255)
            val mb = (mapped[2] * 255f).roundToInt().coerceIn(0, 255)

            val rr = lerp((r * 255f).roundToInt(), mr, t)
            val gg = lerp((g * 255f).roundToInt(), mg, t)
            val bb = lerp((b * 255f).roundToInt(), mb, t)
            src[i] = (a shl 24) or (rr shl 16) or (gg shl 8) or bb
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(src, 0, w, 0, 0, w, h)
        return out
    }

    private fun lerp(a: Int, b: Int, t: Float): Int {
        return (a + (b - a) * t).roundToInt().coerceIn(0, 255)
    }

    private fun sampleLut2d(
        lutPixels: IntArray,
        lutWidth: Int,
        lutSize: Int,
        tiles: Int,
        r: Float,
        g: Float,
        b: Float,
    ): FloatArray {
        val rf = r.coerceIn(0f, 1f) * (lutSize - 1)
        val gf = g.coerceIn(0f, 1f) * (lutSize - 1)
        val bf = b.coerceIn(0f, 1f) * (lutSize - 1)

        val b0 = floor(bf).toInt().coerceIn(0, lutSize - 1)
        val b1 = min(lutSize - 1, b0 + 1)
        val bt = (bf - b0).coerceIn(0f, 1f)

        val c0 = sampleLutSlice(lutPixels, lutWidth, lutSize, tiles, rf, gf, b0)
        val c1 = sampleLutSlice(lutPixels, lutWidth, lutSize, tiles, rf, gf, b1)

        return floatArrayOf(
            c0[0] + (c1[0] - c0[0]) * bt,
            c0[1] + (c1[1] - c0[1]) * bt,
            c0[2] + (c1[2] - c0[2]) * bt,
        )
    }

    private fun sampleLutSlice(
        lutPixels: IntArray,
        lutWidth: Int,
        lutSize: Int,
        tiles: Int,
        rf: Float,
        gf: Float,
        slice: Int,
    ): FloatArray {
        val tileX = slice % tiles
        val tileY = slice / tiles
        val x0 = floor(rf).toInt().coerceIn(0, lutSize - 1)
        val y0 = floor(gf).toInt().coerceIn(0, lutSize - 1)
        val x1 = min(lutSize - 1, x0 + 1)
        val y1 = min(lutSize - 1, y0 + 1)
        val tx = (rf - x0).coerceIn(0f, 1f)
        val ty = (gf - y0).coerceIn(0f, 1f)

        fun lutAt(x: Int, y: Int): Int {
            val px = tileX * lutSize + x
            val py = tileY * lutSize + y
            val idx = py * lutWidth + px
            return if (idx in lutPixels.indices) lutPixels[idx] else 0
        }

        val c00 = lutAt(x0, y0)
        val c10 = lutAt(x1, y0)
        val c01 = lutAt(x0, y1)
        val c11 = lutAt(x1, y1)

        val r00 = ((c00 ushr 16) and 0xFF) / 255f
        val g00 = ((c00 ushr 8) and 0xFF) / 255f
        val b00 = (c00 and 0xFF) / 255f
        val r10 = ((c10 ushr 16) and 0xFF) / 255f
        val g10 = ((c10 ushr 8) and 0xFF) / 255f
        val b10 = (c10 and 0xFF) / 255f
        val r01 = ((c01 ushr 16) and 0xFF) / 255f
        val g01 = ((c01 ushr 8) and 0xFF) / 255f
        val b01 = (c01 and 0xFF) / 255f
        val r11 = ((c11 ushr 16) and 0xFF) / 255f
        val g11 = ((c11 ushr 8) and 0xFF) / 255f
        val b11 = (c11 and 0xFF) / 255f

        val r0 = r00 + (r10 - r00) * tx
        val g0 = g00 + (g10 - g00) * tx
        val b0 = b00 + (b10 - b00) * tx
        val r1 = r01 + (r11 - r01) * tx
        val g1 = g01 + (g11 - g01) * tx
        val b1 = b01 + (b11 - b01) * tx

        return floatArrayOf(
            r0 + (r1 - r0) * ty,
            g0 + (g1 - g0) * ty,
            b0 + (b1 - b0) * ty,
        )
    }

    private fun drawStrokes(canvas: Canvas, strokes: List<Stroke>) {
        for (stroke in strokes) {
            if (stroke.points.size < 2) continue
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                strokeWidth = max(1f, stroke.widthPx)
                color = stroke.color
                alpha = (stroke.alpha.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
            }
            val path = Path()
            val first = stroke.points.first()
            path.moveTo(first.x, first.y)
            for (p in stroke.points.drop(1)) {
                path.lineTo(p.x, p.y)
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawOverlays(contentResolver: ContentResolver, canvas: Canvas, overlays: List<OverlayElement>) {
        for (overlay in overlays) {
            when (overlay) {
                is TextOverlay -> drawTextOverlay(canvas, overlay)
                is StickerOverlay -> drawStickerOverlay(contentResolver, canvas, overlay)
            }
        }
    }

    private fun drawTextOverlay(canvas: Canvas, overlay: TextOverlay) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = overlay.color
            textSize = max(1f, overlay.textSizePx * overlay.scale)
            alpha = (overlay.alpha.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
            typeface = overlay.typefaceName?.let { Typeface.create(it, Typeface.NORMAL) } ?: Typeface.DEFAULT
        }
        val text = overlay.text
        if (text.isBlank()) return
        val width = paint.measureText(text)
        val fm = paint.fontMetrics
        val height = fm.descent - fm.ascent

        canvas.save()
        canvas.translate(overlay.centerX, overlay.centerY)
        canvas.rotate(overlay.rotationDegrees)
        canvas.drawText(text, -width / 2f, height / 2f - fm.descent, paint)
        canvas.restore()
    }

    private fun drawStickerOverlay(contentResolver: ContentResolver, canvas: Canvas, overlay: StickerOverlay) {
        val sticker = decodeBitmap(contentResolver, overlay.uri, 2048) ?: return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = (overlay.alpha.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
        }
        val m = Matrix()
        m.postTranslate(-sticker.width / 2f, -sticker.height / 2f)
        m.postScale(overlay.scale, overlay.scale)
        m.postRotate(overlay.rotationDegrees)
        m.postTranslate(overlay.centerX, overlay.centerY)
        canvas.drawBitmap(sticker, m, paint)
        sticker.recycle()
    }
}
