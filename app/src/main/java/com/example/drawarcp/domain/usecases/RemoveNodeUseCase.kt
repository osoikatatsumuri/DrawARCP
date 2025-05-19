package com.example.drawarcp.domain.usecases

import com.example.drawarcp.domain.interfaces.INodeProvider
import javax.inject.Inject

class RemoveNodeUseCase @Inject constructor(private val provider: INodeProvider) {
    operator fun invoke(nodeId: String) {
        provider.deleteNode(nodeId)
    }
}