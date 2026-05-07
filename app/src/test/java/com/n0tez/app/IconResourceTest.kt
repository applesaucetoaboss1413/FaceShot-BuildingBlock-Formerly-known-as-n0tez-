package com.n0tez.app

import org.junit.Test
import org.junit.Assert.*
import java.io.File

class IconResourceTest {
    
    private val projectRoot = File(System.getProperty("user.dir") ?: ".")
    private val resDir = if (File(projectRoot, "app").exists()) {
        File(projectRoot, "app/src/main/res")
    } else {
        File(projectRoot, "src/main/res")
    }

    private fun getPngDimensions(file: File): Pair<Int, Int>? {
        if (!file.exists()) return null
        try {
            val bytes = file.inputStream().use { it.readBytes() }
            if (bytes.size < 24) return null
            
            // Check PNG signature
            val pngSignature = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte())
            for (i in pngSignature.indices) {
                if (bytes[i] != pngSignature[i]) return null
            }
            
            // Width: bytes 16-19 (Big Endian)
            val width = ((bytes[16].toInt() and 0xFF) shl 24) or
                        ((bytes[17].toInt() and 0xFF) shl 16) or
                        ((bytes[18].toInt() and 0xFF) shl 8) or
                        (bytes[19].toInt() and 0xFF)
            
            // Height: bytes 20-23 (Big Endian)
            val height = ((bytes[20].toInt() and 0xFF) shl 24) or
                         ((bytes[21].toInt() and 0xFF) shl 16) or
                         ((bytes[22].toInt() and 0xFF) shl 8) or
                         (bytes[23].toInt() and 0xFF)
                         
            return Pair(width, height)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getJpegDimensions(file: File): Pair<Int, Int>? {
        if (!file.exists()) return null
        return try {
            val bytes = file.inputStream().use { it.readBytes() }
            if (bytes.size < 4) return null
            if (bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) return null

            var i = 2
            while (i + 9 < bytes.size) {
                if (bytes[i] != 0xFF.toByte()) {
                    i++
                    continue
                }

                var marker = bytes[i + 1].toInt() and 0xFF
                while (marker == 0xFF && i + 2 < bytes.size) {
                    i++
                    marker = bytes[i + 1].toInt() and 0xFF
                }

                if (marker == 0xD9 || marker == 0xDA) return null

                val length = ((bytes[i + 2].toInt() and 0xFF) shl 8) or (bytes[i + 3].toInt() and 0xFF)
                if (length < 2) return null

                val isSofMarker = marker == 0xC0 || marker == 0xC1 || marker == 0xC2 || marker == 0xC3 ||
                    marker == 0xC5 || marker == 0xC6 || marker == 0xC7 || marker == 0xC9 ||
                    marker == 0xCA || marker == 0xCB || marker == 0xCD || marker == 0xCE || marker == 0xCF

                if (isSofMarker) {
                    val height = ((bytes[i + 5].toInt() and 0xFF) shl 8) or (bytes[i + 6].toInt() and 0xFF)
                    val width = ((bytes[i + 7].toInt() and 0xFF) shl 8) or (bytes[i + 8].toInt() and 0xFF)
                    return Pair(width, height)
                }

                i += 2 + length
            }

            null
        } catch (_: Exception) {
            null
        }
    }

    private fun getImageDimensions(file: File): Pair<Int, Int>? {
        return when (file.extension.lowercase()) {
            "png" -> getPngDimensions(file)
            "jpg", "jpeg" -> getJpegDimensions(file)
            else -> null
        }
    }

    @Test
    fun testBubbleIconExistenceAndDimensions() {
        val largePng = File(resDir, "drawable-nodpi/ic_floating_bubble_large.png")
        assertTrue("File should exist: ${largePng.path}", largePng.exists())
        val largeDims = getPngDimensions(largePng)
        assertNotNull("Should be a valid PNG", largeDims)
        assertEquals("Width should be 512", 512, largeDims?.first)
        assertEquals("Height should be 512", 512, largeDims?.second)

        val originalJpg = File(resDir, "drawable-nodpi/ic_floating_bubble_original.jpg")
        assertTrue("File should exist: ${originalJpg.path}", originalJpg.exists())
        val originalDims = getImageDimensions(originalJpg)
        assertNotNull("Should be a valid image", originalDims)
        assertTrue("Original icon width should be >= 96", (originalDims?.first ?: 0) >= 96)
        assertTrue("Original icon height should be >= 96", (originalDims?.second ?: 0) >= 96)
    }

    @Test
    fun testLauncherIconExistenceAndDimensions() {
        val densities = mapOf(
            "mdpi" to 48,
            "hdpi" to 72,
            "xhdpi" to 96,
            "xxhdpi" to 144,
            "xxxhdpi" to 192
        )

        densities.forEach { (density, size) ->
            val file = File(resDir, "mipmap-$density/ic_launcher.png")
            assertTrue("File should exist: ${file.path}", file.exists())
            
            val dims = getPngDimensions(file)
            assertNotNull("Should be a valid PNG", dims)
            assertEquals("Width should match for $density", size, dims?.first)
            assertEquals("Height should match for $density", size, dims?.second)
        }
    }
    
    @Test
    fun testMemoryLoadCheck() {
        // Verify that files are readable (simulated memory check)
        val densities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
        var count = 0
        
        densities.forEach { density ->
            val launcherFile = File(resDir, "mipmap-$density/ic_launcher.png")
            if (launcherFile.exists()) {
                val bytes = launcherFile.readBytes()
                assertTrue(bytes.isNotEmpty())
                count++
            }
        }

        val bubbleLargeFile = File(resDir, "drawable-nodpi/ic_floating_bubble_large.png")
        if (bubbleLargeFile.exists()) {
            val bytes = bubbleLargeFile.readBytes()
            assertTrue(bytes.isNotEmpty())
            count++
        }

        val bubbleOriginalFile = File(resDir, "drawable-nodpi/ic_floating_bubble_original.jpg")
        if (bubbleOriginalFile.exists()) {
            val bytes = bubbleOriginalFile.readBytes()
            assertTrue(bytes.isNotEmpty())
            count++
        }

        assertTrue("Should verify files", count > 0)
    }
}
