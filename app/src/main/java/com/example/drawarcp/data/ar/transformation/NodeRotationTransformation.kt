package com.example.drawarcp.data.ar.transformation

import com.example.drawarcp.data.models.ARNodeData
import dev.romainguy.kotlin.math.Float3

class NodeRotationTransformation : ARNodeTransformation<Float3> {
    override fun apply(
        node: ARNodeData,
        params: Float3
    ): ARNodeData {
        return node.copy(localAngles = params)
    }
}
