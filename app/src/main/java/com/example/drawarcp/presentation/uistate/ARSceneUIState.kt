package com.example.drawarcp.presentation.uistate

import com.example.drawarcp.presentation.uistate.nodes.INodeUIState

data class ARSceneUIState(
    val nodesItems: List<INodeUIState> = listOf(),
    val activeNode: INodeUIState? = null,
    val errorMsg: String? = null,
)