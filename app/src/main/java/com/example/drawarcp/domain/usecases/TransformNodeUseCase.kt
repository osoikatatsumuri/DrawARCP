package com.example.drawarcp.domain.usecases

import com.example.drawarcp.data.ar.transformation.TransformationFactory
import com.example.drawarcp.data.ar.transformation.TransformationType
import com.example.drawarcp.data.models.ARNodeData
import com.example.drawarcp.domain.interfaces.INodeProvider
import javax.inject.Inject

class TransformNodeUseCase @Inject constructor(private val provider: INodeProvider) {
    operator fun <T : Any> invoke(nodeId: String, type: TransformationType<T>, params: T): Result<ARNodeData> {
        return provider.getNodeById(nodeId)
            .mapCatching { node ->
                TransformationFactory.create(type = type).apply(node = node, params = params)
            }
    }
}