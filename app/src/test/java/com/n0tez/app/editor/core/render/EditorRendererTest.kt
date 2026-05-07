package com.n0tez.app.editor.core.render

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.n0tez.app.photoeditor.Adjustments
import com.n0tez.app.photoeditor.PhotoEditorState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditorRendererTest {
    @Test
    fun renderFinalBitmap_delegatesAndIsDeterministicWithSeed() = runBlocking {
        val contentResolver =
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver

        val src = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply {
            eraseColor(0xFF7F7F7F.toInt())
        }

        val state = PhotoEditorState(adjustments = Adjustments(grain = 1f))
        val out1 = EditorRenderer.renderFinalBitmap(contentResolver, src, state, seed = 7)
        val out2 = EditorRenderer.renderFinalBitmap(contentResolver, src, state, seed = 7)
        val out3 = EditorRenderer.renderFinalBitmap(contentResolver, src, state, seed = 8)

        assertEquals(out1.getPixel(4, 4), out2.getPixel(4, 4))
        assertNotEquals(out1.getPixel(4, 4), out3.getPixel(4, 4))

        out1.recycle()
        out2.recycle()
        out3.recycle()
        src.recycle()
    }
}

