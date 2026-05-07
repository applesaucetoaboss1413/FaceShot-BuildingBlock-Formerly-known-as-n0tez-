package com.n0tez.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FileValidationTest {

    // Logic mirroring MediaGalleryActivity
    private fun isImageFile(name: String): Boolean {
        val lower = name.toLowerCase(Locale.ROOT)
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".bmp")
    }

    @Test
    fun testSupportedImageFormats() {
        assertTrue("JPG should be supported", isImageFile("photo.jpg"))
        assertTrue("JPEG should be supported", isImageFile("image.jpeg"))
        assertTrue("PNG should be supported", isImageFile("image.png"))
        assertTrue("WEBP should be supported", isImageFile("sticker.webp"))
        assertTrue("GIF should be supported", isImageFile("animation.gif"))
        assertTrue("BMP should be supported", isImageFile("icon.bmp"))
    }

    @Test
    fun testUnsupportedFormats() {
        assertFalse("PDF should not be supported", isImageFile("document.pdf"))
        assertFalse("SH should not be supported", isImageFile("script.sh"))
        assertFalse("EXE should not be supported", isImageFile("virus.exe"))
        assertFalse("Unknown should not be supported", isImageFile("file.xyz"))
    }
    
    @Test
    fun testCaseInsensitivity() {
        assertTrue("Uppercase JPG should be supported", isImageFile("PHOTO.JPG"))
        assertTrue("Mixed case Png should be supported", isImageFile("Image.Png"))
    }
}
