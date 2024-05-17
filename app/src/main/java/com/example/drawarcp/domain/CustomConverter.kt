package com.example.drawarcp.domain

import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.collision.Vector3

class CustomConverter {
    companion object {
        fun convertFloat3ToVector3(t: Float3): Vector3 {
            return Vector3(t.x, t.y, t.z)
        }

        fun convertVector3ToFloat3(t: Vector3): Float3 {
            return Float3(t.x, t.y, t.z)
        }
    }
}