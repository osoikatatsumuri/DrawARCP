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
                    val argbPixel = rgbImage[x, y]
                    val newPixel = (argbPixel and 0x00FFFFFF) or (alpha shl 24)
                    rgbImage[x, y] = newPixel
                }
            }

            return rgbImage
        }
    }
}