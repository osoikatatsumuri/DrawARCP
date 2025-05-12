package com.example.drawarcp.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drawarcp.data.models.ARNodeData
import com.example.drawarcp.data.ar.ARNodeProvider
import com.example.drawarcp.data.ar.transformation.TransformationType
import com.example.drawarcp.domain.usecases.AddNodeUseCase
import com.example.drawarcp.domain.usecases.TransformNodeUseCase
import com.example.drawarcp.domain.utils.NodeMapper
import com.example.drawarcp.presentation.uistate.ARSceneUIState
import com.example.drawarcp.presentation.uistate.NodeUIState
import com.google.android.filament.Engine
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.dot
import dev.romainguy.kotlin.math.normalize
import dev.romainguy.kotlin.math.slerp
import io.github.sceneview.collision.Quaternion
import io.github.sceneview.loaders.MaterialLoader
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.typeOf

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
    ) = viewModelScope.launch(Dispatchers.IO) {

        val mapper = nodeMapper ?: throw IllegalStateException("AR scene dependencies is not initialized")


        val transformationResult = transformNodeUseCase.invoke(nodeId, type, params)
            .onSuccess { updatedNode ->
                provider.updateNode(updatedNode)

                val domainNode = mapper.mapToDomainLayer(updatedNode)
                val uiNode = mapper.mapToUILayer(domainNode)

                withContext(Dispatchers.Main) {
                    _sceneState.update { sceneState ->
                        sceneState.nodesItems.find { it.id == updatedNode.id }?.apply {
                            when (type) {
                                TransformationType.SCALE -> node.scale = domainNode.scale
                                TransformationType.OPACITY -> TODO()
                                TransformationType.ROTATE -> {
                                    node.childNodes.first().quaternion = uiNode.node.childNodes.first().quaternion
                                }
                            }
                        }

                        sceneState
                    }
                }
            }
            .onFailure { exception ->
                Log.d("!!!", exception.toString())
            }
    }

    fun addNode(frame: Frame?, x: Float, y: Float, imageFileLocation: String = "images/rabbit.png") {
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
}