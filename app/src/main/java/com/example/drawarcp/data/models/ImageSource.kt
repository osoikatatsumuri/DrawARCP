package com.example.drawarcp.data.models

import android.graphics.Bitmap
import android.net.Uri

sealed class ImageSource {
    data class FileSource(val path: String): ImageSource()
    data class BitmapSource(val uri: Uri): ImageSource()
}