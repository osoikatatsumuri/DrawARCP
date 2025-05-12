package com.example.drawarcp.data.ar.transformation

import com.example.drawarcp.data.models.ARNodeData

class NodeOpacityTransformation: ARNodeTransformation<Int> {
    override fun apply(
        node: ARNodeData,
        params: Int
    ): ARNodeData {
        require(params in 0..255) {"Alpha channel must be between 0 and 255"}
        return node.copy(alpha = params)
    }
}