package com.example.drawarcp.domain

import com.example.drawarcp.data.models.ARNodeData

sealed class SceneEvent {
    data class NodeAdded(val node: ARNodeData): SceneEvent()
    data class NodeRemoved(val nodeId: String): SceneEvent()
    data class NodeTransformed(val node: ARNodeData): SceneEvent()
    data class Initialize(val nodes: List<ARNodeData>): SceneEvent()
}