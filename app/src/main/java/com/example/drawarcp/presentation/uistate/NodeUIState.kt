package com.example.drawarcp.presentation.uistate

import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.node.AnchorNode

data class NodeUIState(
    val id: String,
    val node: AnchorNode,
    var scale: Float3,
    var opacity: Int,
    var rotationAngles: Float3
)
