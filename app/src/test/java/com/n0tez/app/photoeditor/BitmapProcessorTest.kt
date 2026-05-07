package com.n0tez.app.photoeditor

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BitmapProcessorTest {

    @Test
    fun renderFinalBitmap_brightnessChangesPixels() = runBlocking {
        val contentResolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        val src = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
            setPixel(0, 0, 0xFF202020.toInt())
            setPixel(1, 0, 0xFF808080.toInt())
            setPixel(0, 1, 0xFF000000.toInt())
            setPixel(1, 1, 0xFFFFFFFF.toInt())
        }

        val out = BitmapProcessor.renderFinalBitmap(
            contentResolver = contentResolver,
            source = src,
            state = PhotoEditorState(adjustments = Adjustments(brightness = 0.2f)),
        )

        assertNotEquals(src.getPixel(0, 0), out.getPixel(0, 0))
        assertNotEquals(src.getPixel(1, 0), out.getPixel(1, 0))
        out.recycle()
        src.recycle()
    }

    @Test
    fun renderFinalBitmap_grainDeterministicWithSeed() = runBlocking {
        val contentResolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        val src = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply {
            eraseColor(0xFF7F7F7F.toInt())
        }

        val state = PhotoEditorState(adjustments = Adjustments(grain = 1f))
        val out1 = BitmapProcessor.renderFinalBitmap(contentResolver, src, state, seed = 123)
        val out2 = BitmapProcessor.renderFinalBitmap(contentResolver, src, state, seed = 123)
        val out3 = BitmapProcessor.renderFinalBitmap(contentResolver, src, state, seed = 124)

        assertEquals(out1.getPixel(3, 3), out2.getPixel(3, 3))
        assertNotEquals(out1.getPixel(3, 3), out3.getPixel(3, 3))

        out1.recycle()
        out2.recycle()
        out3.recycle()
        src.recycle()
    }
}

