package com.example.drawarcp.data.ar.transformation

import com.example.drawarcp.data.models.ARNodeData
import dev.romainguy.kotlin.math.Float3

class NodeScaleTransformation: ARNodeTransformation<Float3>
{
    override fun apply(
        node: ARNodeData,
        params: Float3
    ): ARNodeData {
        require(params.toFloatArray().all { it >= 0.3f } ) { "Scale must be greater than 0.3f" }

        return node.copy(scale = params)
    }
}