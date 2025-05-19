package com.example.drawarcp.data.ar

import com.example.drawarcp.data.models.ARNodeData
import com.example.drawarcp.domain.interfaces.INodeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ARNodeProvider: INodeProvider {
    private val _nodes = MutableStateFlow<List<ARNodeData>>(emptyList())
    val nodes: StateFlow<List<ARNodeData>> = _nodes.asStateFlow()

    override fun registerNode(node: ARNodeData) {
        _nodes.update { currentNodes ->
            if (currentNodes.any { it.id == node.id }) currentNodes
            else currentNodes + node
        }
    }

    override fun deleteNode(nodeId: String) {
        _nodes.update { nodes ->
            nodes.filter { it.id != nodeId }
        }
    }

    override fun updateNode(node: ARNodeData) {
        _nodes.update { nodes ->
            nodes.map { if (it.id == node.id) node else it}
        }
    }

    override fun getNodeById(id: String): Result<ARNodeData> {
        val selectedNode = _nodes.value.find {it.id == id}

        if (selectedNode == null) {
            return Result.failure(Exception("Selected node is not found"))
        }

        return Result.success(selectedNode)
    }
}