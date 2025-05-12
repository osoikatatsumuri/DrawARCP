package com.example.drawarcp.presentation.uistate

data class ARSceneUIState(
    val nodesItems: List<NodeUIState> = listOf(),
    val activeNode: NodeUIState? = null,
    val errorMsg: String? = null,
)