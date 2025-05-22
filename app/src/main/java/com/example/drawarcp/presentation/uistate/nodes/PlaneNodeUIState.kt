package com.example.drawarcp.presentation.uistate.nodes

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.node.Node

data class PlaneNodeUIState(
    override val id: String,
    override val node: Node,
    val scale: Float3,
    val rotation: Quaternion,
    val initialNodePosition: Float3,
): INodeUIState
