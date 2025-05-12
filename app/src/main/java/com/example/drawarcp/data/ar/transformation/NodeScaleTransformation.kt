package com.example.drawarcp.data.ar.transformation

import com.example.drawarcp.data.models.ARNodeData

class NodeScaleTransformation: ARNodeTransformation<Float>
{
    override fun apply(
        node: ARNodeData,
        params: Float
    ): ARNodeData {
        require(params > 0) { "Scale must be greater than 0" }

        return node.copy(scale = params)
    }
}