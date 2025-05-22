package com.example.drawarcp.presentation.uistate

import io.github.sceneview.collision.Vector3

data class PlanePlacementUIState(
    val id: String? = null,
    val isPlacement: Boolean = false,
    var currentDistance: Float = 2f,
    var directionVector: Vector3 = Vector3(0f, 0f, 0f),
)