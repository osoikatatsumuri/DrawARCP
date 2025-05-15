package com.example.drawarcp.presentation.viewmodels

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.compose.ui.graphics.Paint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drawarcp.data.ar.ARNodeProvider
import com.example.drawarcp.data.ar.transformation.TransformationType
import com.example.drawarcp.domain.usecases.AddNodeUseCase
import com.example.drawarcp.domain.usecases.TransformNodeUseCase
import com.example.drawarcp.domain.utils.BitmapProcessor
import com.example.drawarcp.domain.utils.NodeMapper
import com.example.drawarcp.presentation.uistate.ARSceneUIState
import com.example.drawarcp.presentation.uistate.nodes.AnchorNodeUIState
import com.google.android.filament.Engine
import com.google.ar.core.Frame
import com.google.ar.core.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.node.ImageNode
import io.github.sceneview.node.Node
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ARSceneViewModel @Inject constructor(
    internal val provider: ARNodeProvider,
    private val addNodeUseCase: AddNodeUseCase,
    private val transformNodeUseCase: TransformNodeUseCase,

): ViewModel() {

    private val _sceneState = MutableStateFlow<ARSceneUIState>(ARSceneUIState())
    val sceneState: StateFlow<ARSceneUIState> = _sceneState.asStateFlow()

    private var nodeMapper: NodeMapper? = null

    fun initARDependencies(session: Session, engine: Engine, materialLoader: MaterialLoader) {
        nodeMapper = NodeMapper(session, engine, materialLoader)
    }

    fun <T : Any> applyTransformation(
        nodeId: String,
        type: TransformationType<T>,
        params: T
    ) = viewModelScope.launch(Dispatchers.Default) {

        val mapper = nodeMapper ?: throw IllegalStateException("AR scene dependencies is not initialized")


        val transformationResult = transformNodeUseCase.invoke(nodeId, type, params)
            .onSuccess { updatedNode ->
                provider.updateNode(updatedNode)

                val domainNode = mapper.mapToDomainLayer(updatedNode)
                val uiNode = mapper.mapToUILayer(domainNode)

                _sceneState.update { sceneState ->
                    (sceneState.nodesItems.find {it.id == updatedNode.id } as AnchorNodeUIState).apply {
                        when (type) {
                            TransformationType.SCALE -> {
                                withContext(Dispatchers.Main) {
                                    node.scale = domainNode.scale
                                    scale = domainNode.scale
                                }
                            }
                            TransformationType.OPACITY -> {
                                val bitmap = withContext(Dispatchers.IO) {
                                    BitmapProcessor.adjustOpacity((uiNode.node.childNodes.first() as ImageNode).bitmap, domainNode.opacity)
                                }

                                withContext(Dispatchers.Main) {
                                    opacity = domainNode.opacity
                                    (node.childNodes.first() as ImageNode).bitmap = bitmap
                                }
                            }
                            TransformationType.ROTATE -> {
                                withContext(Dispatchers.Main) {
                                    rotationAngles = domainNode.rotationAngles
                                    node.childNodes.first().quaternion = uiNode.node.childNodes.first().quaternion
                                }

                            }
                        }
                    }

                    sceneState
                }
            }
            .onFailure { exception ->
                Log.d("AR", exception.toString())
            }
    }

    fun addNode(node: Node) {
        viewModelScope.launch {
            _sceneState.update { sceneState ->
                sceneState.copy(nodesItems = sceneState.nodesItems )
            }
        }
    }

    fun addNode(frame: Frame?, x: Float, y: Float, imageFileLocation: String = "images/images.jpg") {
        viewModelScope.launch {
            if (frame == null) {
                return@launch
            }

            val mapper = nodeMapper ?: throw IllegalStateException("AR scene dependencies is not initialized")

            addNodeUseCase.invoke(frame, x, y, imageFileLocation)
                .onSuccess { createdNode ->
                    val domainNode = mapper.mapToDomainLayer(createdNode)

                    _sceneState.update { sceneState ->
                        sceneState.copy(nodesItems = sceneState.nodesItems + mapper.mapToUILayer(domainNode))
                    }

                    provider.registerNode(createdNode)
                }
                .onFailure { exception ->
                    Log.d("AR", exception.toString())

                    _sceneState.update { sceneState ->
                        sceneState.copy(errorMsg = exception.toString())
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}