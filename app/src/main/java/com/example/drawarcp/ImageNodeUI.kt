package com.example.drawarcp
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ImageNode
import io.github.sceneview.node.Node

class ImageNodeUI(private val imageNode: ImageNode, private var scale: Scale, private var quaternion: Quaternion) {
    init {
        imageNode.scale = scale
        imageNode.quaternion = quaternion
    }

    fun setScale(newScale: Scale) {
        scale = newScale
        imageNode.scale = newScale
    }

    fun setQuaternion(newQuaternion: Quaternion) {
        quaternion = newQuaternion
        imageNode.quaternion = quaternion
    }

    fun getScale(): Scale {
        return scale
    }

    fun getQuaternion(): Quaternion {
        return quaternion
    }
}
