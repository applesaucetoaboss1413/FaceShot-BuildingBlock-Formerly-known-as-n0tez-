package com.n0tez.app.editor.core.render

import android.content.ContentResolver
import android.graphics.Bitmap
import com.n0tez.app.editor.core.EditorState
import com.n0tez.app.photoeditor.BitmapProcessor

/**
 * Renders editor state into a final bitmap for export/share.
 *
 * This is intentionally a thin wrapper over the existing clean-room renderer to keep behavior
 * consistent while the module structure is stabilized.
 */
object EditorRenderer {
    suspend fun renderFinalBitmap(
        contentResolver: ContentResolver,
        source: Bitmap,
        state: EditorState,
        outputMaxSize: Int? = null,
        seed: Int = 0,
    ): Bitmap {
        return BitmapProcessor.renderFinalBitmap(
            contentResolver = contentResolver,
            source = source,
            state = state,
            outputMaxSize = outputMaxSize,
            seed = seed,
        )
    }
}

