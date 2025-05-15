package com.example.drawarcp.presentation.uistate.nodes

import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.node.Node

data class NodeUIState(
    val id: String,
    val node: Node,
    var scale: Float3,
    var opacity: Int,
    var rotationAngles: Float3
)
