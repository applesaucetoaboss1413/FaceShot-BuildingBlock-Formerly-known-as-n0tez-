package com.n0tez.app.photoeditor

import android.graphics.Bitmap
import android.util.Log
import com.camerasideas.collagemaker.photoproc.removal.ObjectRemoval

object NativePhotoEditor {
    private const val logTag = "NativePhotoEditor"
    private val isLibLoaded: Boolean = loadLibraries()

    fun isAvailable(): Boolean = isLibLoaded

    fun inpaintWithMask(base: Bitmap, mask: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
        val result = ObjectRemoval.runObjectRemoval(base, mask, output)
        if (result < 0) {
            throw IllegalStateException("Native inpaint failed: $result")
        }
        return output
    }

    private fun loadLibraries(): Boolean {
        val libs = listOf(
            "c++_shared",
            "cjson",
            "cvautils",
            "cvalgo",
            "tiny-aes",
            "core_util",
            "nms",
            "itcore",
            "MNN",
            "mnncore",
            "segCore",
            "matting",
            "objectremoval",
        )
        return try {
            for (lib in libs) {
                System.loadLibrary(lib)
            }
            true
        } catch (t: Throwable) {
            Log.w(logTag, "Native photo libs unavailable: ${t.message}")
            false
        }
    }
}
