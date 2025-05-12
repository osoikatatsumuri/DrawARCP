package com.example.drawarcp.domain.interfaces

import com.example.drawarcp.data.models.ARNodeData

interface INodeProvider {
    fun updateNode(node: ARNodeData)
    fun getNodeById(id: String): Result<ARNodeData>
    fun registerNode(node: ARNodeData)
}