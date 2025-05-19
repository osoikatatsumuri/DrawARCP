package com.example.drawarcp.domain.models

import com.example.drawarcp.data.models.ImageSource
import com.google.ar.core.Pose
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion

data class NodeDomainData(
    val id: String,
    val pose: Pose,
    val imageSource: ImageSource,
    val scale: Float3,
    val rotationAngles: Float3,
    val initialWorldQuaternion: Quaternion,
    val normal: Float3,
    val opacity: Int = 255
)
