package com.example.drawarcp.presentation.uistate.nodes

import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.node.AnchorNode

data class AnchorNodeUIState(
    override val id: String,
    override val node: AnchorNode,
    var scale: Float3,
    var opacity: Int,
    var rotationAngles: Float3
): INodeUIState
