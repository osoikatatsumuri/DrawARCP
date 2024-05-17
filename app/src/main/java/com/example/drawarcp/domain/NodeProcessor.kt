package com.example.drawarcp.domain

import androidx.compose.runtime.snapshots.SnapshotStateList
import io.github.sceneview.node.Node

class NodeProcessor {
    companion object {
        fun findIndex(list: SnapshotStateList<Node>, target: Node): Int {
            for (i in list.indices) {
                if (list[i].childNodes.contains(target)) {
                    return i
                }
            }

            return -1
        }
    }
}