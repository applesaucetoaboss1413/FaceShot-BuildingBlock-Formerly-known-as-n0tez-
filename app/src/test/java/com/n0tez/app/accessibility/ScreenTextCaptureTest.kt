package com.n0tez.app.accessibility

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class ScreenTextCaptureTest {

    @Test
    fun `extractText returns visible intersecting text in reading order`() {
        val nodes = listOf(
            ScreenTextNode(
                text = "Second line",
                bounds = Rect(10, 80, 250, 120),
                packageName = "com.example.reader",
                isVisibleToUser = true,
            ),
            ScreenTextNode(
                text = "First line",
                bounds = Rect(10, 20, 250, 60),
                packageName = "com.example.reader",
                isVisibleToUser = true,
            ),
            ScreenTextNode(
                text = "Outside",
                bounds = Rect(400, 400, 520, 480),
                packageName = "com.example.reader",
                isVisibleToUser = true,
            ),
        )

        val result = ScreenTextCapture.extractText(
            nodes = nodes,
            targetBounds = Rect(0, 0, 300, 200),
            excludedPackageName = "com.n0tez.app",
        )

        assertEquals("First line\nSecond line", result)
    }

    @Test
    fun `extractText skips invisible duplicate and self-package nodes`() {
        val nodes = listOf(
            ScreenTextNode(
                text = "Capture me",
                bounds = Rect(0, 0, 200, 60),
                packageName = "com.example.reader",
                isVisibleToUser = true,
            ),
            ScreenTextNode(
                text = "Capture me",
                bounds = Rect(0, 0, 200, 60),
                packageName = "com.example.reader",
                isVisibleToUser = true,
            ),
            ScreenTextNode(
                text = "Ignore me",
                bounds = Rect(0, 0, 200, 60),
                packageName = "com.n0tez.app",
                isVisibleToUser = true,
            ),
            ScreenTextNode(
                text = "Hidden",
                bounds = Rect(0, 0, 200, 60),
                packageName = "com.example.reader",
                isVisibleToUser = false,
            ),
        )

        val result = ScreenTextCapture.extractText(
            nodes = nodes,
            targetBounds = Rect(0, 0, 300, 200),
            excludedPackageName = "com.n0tez.app",
        )

        assertEquals("Capture me", result)
    }
}
