package com.example.drawarcp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import io.github.sceneview.node.Node
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel() : ViewModel() {
    private val _childNodes = MutableStateFlow(SnapshotStateList<Node>())
    private val _currentNodeIndex = MutableStateFlow(-1)
    private val _isImageConfigurationEnabled = MutableStateFlow(false)
    private val _isNodeLocked = MutableStateFlow(false)
    private val _bitmap = MutableStateFlow<Bitmap?>(null)

    var childNodes: StateFlow<SnapshotStateList<Node>> = _childNodes.asStateFlow()
    var currentNodeIndex: StateFlow<Int> = _currentNodeIndex.asStateFlow()
    var isImageConfigurationEnabled = _isImageConfigurationEnabled.asStateFlow()
    var isNodeLocked = _isNodeLocked.asStateFlow()
    val bitmap = _bitmap.asStateFlow()

    init {
        changeChildNodes(SnapshotStateList())
        changeImageConfigurationFlag(false)
        setNodeIndex(-1)
        setNodeLockStatus(false)
    }

    private fun changeChildNodes(childNodes: SnapshotStateList<Node>) {
        _childNodes.value = childNodes
    }

    fun changeChildNode(childNode: Node, index: Int) {
        _childNodes.value[index] = childNode
    }

    fun setNodeIndex(newIndex: Int) {
        _currentNodeIndex.value = newIndex
    }

    fun setChildNodes(childNodes: SnapshotStateList<Node>) {
        _childNodes.value = childNodes
    }

    fun setBitmap(newBitmap: Bitmap) {
        _bitmap.value = newBitmap
    }

    fun addChildNode(node: Node) {
        _childNodes.value.add(node)
    }

    fun changeImageConfigurationFlag(bool: Boolean) {
        _isImageConfigurationEnabled.value = bool
    }

    fun setNodeLockStatus(bool: Boolean) {
        _isNodeLocked.value = bool
    }
}