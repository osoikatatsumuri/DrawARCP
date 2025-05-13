package com.example.drawarcp.domain.utils

import android.graphics.Bitmap
import androidx.core.graphics.get
import androidx.core.graphics.set

class BitmapProcessor {
    companion object {
        fun adjustOpacity(bitmap: Bitmap, alpha: Int): Bitmap {
            val rgbImage = bitmap.copy(Bitmap.Config.ARGB_8888, true)

            for (y in 0 until rgbImage.height) {
                for (x in 0 until rgbImage.width) {
                    val pixel = rgbImage[x, y]
                    val originalAlpha = (pixel shr 24) and 0xFF
                    val newAlpha = (alpha * originalAlpha) / 255
                    val newPixel = (pixel and 0x00FFFFFF) or (newAlpha shl 24)
                    rgbImage[x, y] = newPixel
                }
            }

            return rgbImage
        }
    }
}