package com.n0tez.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SampleSizeTest {

    // Logic mirroring PhotoEditorActivity
    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    @Test
    fun testSampleSizeCalculation() {
        // Test 1: Image fits exact requirement
        assertEquals(1, calculateInSampleSize(1000, 1000, 1000, 1000))

        // Test 2: Image is exactly 2x larger
        // 2000x2000 -> 1000x1000
        // half = 1000. 1000/1 >= 1000. sample=2.
        assertEquals(2, calculateInSampleSize(2000, 2000, 1000, 1000))
        
        // Test 3: Image is 4x larger
        // 4000x4000 -> 1000x1000
        // half = 2000. 
        // Loop 1: 2000/1 >= 1000. sample=2.
        // Loop 2: 2000/2 = 1000 >= 1000. sample=4.
        assertEquals(4, calculateInSampleSize(4000, 4000, 1000, 1000))
        
        // Test 4: Aspect Ratio difference
        // 4000x2000 -> 1000x1000
        // half = 2000x1000.
        // Loop 1: 2000/1 >= 1000 && 1000/1 >= 1000. sample=2.
        // Loop 2: 2000/2 = 1000 >= 1000 && 1000/2 = 500 >= 1000 (False).
        assertEquals(2, calculateInSampleSize(4000, 2000, 1000, 1000))
    }
}
