package com.example.drawarcp.presentation.uistate

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.drawarcp.presentation.uistate.nodes.INodeUIState

data class ARSceneUIState(
    val nodesItems: List<INodeUIState> = listOf(),
    val activeNode: INodeUIState? = null,
    val planePlacementUIState: PlanePlacementUIState = PlanePlacementUIState(),
    val errorMsg: String? = null,
)