package com.example.drawarcp.data.models

import com.google.ar.core.Pose
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.collision.Vector3

data class ARNodeData(
    val id: String,
    val pose: Pose,
    val imageFileLocation: String,
    val scale: Float3,
    val localAngles: Float3,
    val initialWorldQuaternion: Quaternion,
    val normal: Vector3,
    val alpha: Int,
)