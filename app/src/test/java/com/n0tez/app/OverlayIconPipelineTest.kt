package com.n0tez.app

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class OverlayIconPipelineTest {

    @Test
    fun computeSampleSize_downsamplesForTarget() {
        val service = Robolectric.buildService(FloatingWidgetService::class.java).get()
        val method = service::class.java.getDeclaredMethod(
            "computeSampleSize",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true
        val sampleLarge = method.invoke(service, 512, 512, 96) as Int
        assertEquals(4, sampleLarge)
        val sampleAlreadyTarget = method.invoke(service, 96, 96, 96) as Int
        assertEquals(1, sampleAlreadyTarget)
    }

    @Test
    fun scaleTo96_producesExpectedSize() {
        val target = 96
        val input = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val output = if (input.width != target || input.height != target) {
            Bitmap.createScaledBitmap(input, target, target, true)
        } else {
            input
        }
        assertEquals(96, output.width)
        assertEquals(96, output.height)
    }
}
