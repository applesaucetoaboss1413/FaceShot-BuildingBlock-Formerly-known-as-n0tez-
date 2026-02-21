package com.n0tez.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PhotoEditorLayoutTest {
    @Test
    fun toolButtons_useTopIcons() {
        val candidates = listOf(
            File("app/src/main/res/layout/activity_photo_editor.xml"),
            File("src/main/res/layout/activity_photo_editor.xml"),
        )
        val layoutFile = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Could not find activity_photo_editor.xml from test working directory.")

        val xml = layoutFile.readText()

        val ids = listOf(
            "btnAdjust",
            "btnFilters",
            "btnDraw",
            "btnAddText",
            "btnAddSticker",
            "btnUndo",
        )
        for (id in ids) {
            val hasTopIconGravity = Regex(
                pattern =
                    "android:id=\\\"@\\+id/$id\\\"[\\s\\S]*?app:iconGravity=\\\"top\\\"",
            ).containsMatchIn(xml)

            assertTrue("Expected app:iconGravity=\\\"top\\\" for $id", hasTopIconGravity)
        }
    }
}
