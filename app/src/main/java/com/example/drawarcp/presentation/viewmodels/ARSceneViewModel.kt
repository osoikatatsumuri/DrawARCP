package com.example.drawarcp.presentation.viewmodels

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drawarcp.data.ar.ARNodeProvider
import com.example.drawarcp.data.ar.transformation.TransformationType
import com.example.drawarcp.data.models.ImageSource
import com.example.drawarcp.domain.usecases.AddNodeUseCase
import com.example.drawarcp.domain.usecases.RemoveNodeUseCase
import com.example.drawarcp.domain.usecases.TransformNodeUseCase
import com.example.drawarcp.domain.utils.BitmapProcessor
import com.example.drawarcp.domain.utils.NodeMapper
import com.example.drawarcp.presentation.uistate.ARSceneUIState
import com.example.drawarcp.presentation.uistate.nodes.AnchorNodeUIState
import com.google.android.filament.Colors
import com.google.android.filament.Engine
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romainguy.kotlin.math.Quaternion
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
import java.io.File
import java.io.IOException

@HiltViewModel
class ARSceneViewModel @Inject constructor(
    internal val provider: ARNodeProvider,
    private val addNodeUseCase: AddNodeUseCase,
    private val transformNodeUseCase: TransformNodeUseCase,
    private val removeNodeUseCase: RemoveNodeUseCase,
): ViewModel() {

    private val _sceneState = MutableStateFlow<ARSceneUIState>(ARSceneUIState())
    val sceneState: StateFlow<ARSceneUIState> = _sceneState.asStateFlow()

    private val _currentFrame = MutableStateFlow<Frame?>(null)
    val currentFrame: StateFlow<Frame?> = _currentFrame.asStateFlow()

    fun updateCurrentFrame(frame: Frame) {
        _currentFrame.value = frame
    }

    private var nodeMapper: NodeMapper? = null

    fun initARDependencies(session: Session, engine: Engine, materialLoader: MaterialLoader) {
        nodeMapper = NodeMapper(session, engine, materialLoader)
    }

    fun updateMapperSession(newSession: Session) {
        nodeMapper?.session = newSession
    }

    fun <T : Any> applyTransformation(
        nodeId: String,
        type: TransformationType<T>,
        params: T,
    ) = viewModelScope.launch(Dispatchers.Default) {

        val mapper = nodeMapper ?: throw IllegalStateException("AR scene dependencies is not initialized")

        val transformationResult = transformNodeUseCase.invoke(nodeId, type, params)
            .onSuccess { updatedNode ->
                provider.updateNode(updatedNode)

                val domainNode = mapper.mapToDomainLayer(updatedNode)

                _sceneState.update { sceneState ->
                    (sceneState.nodesItems.find {it.id == updatedNode.id } as AnchorNodeUIState).apply {
                        when (type) {
                            TransformationType.SCALE -> {
                                node.scale = domainNode.scale
                                scale = domainNode.scale
                            }
                            TransformationType.OPACITY -> {
                                val materialInstance = (node.childNodes.first() as ImageNode).materialInstance

                                (node.childNodes.first() as ImageNode).materialInstance = materialInstance
                            }
                            TransformationType.ROTATE -> {
                                rotationAngles = domainNode.rotationAngles
                                node.childNodes.first().quaternion = Quaternion.fromEuler(domainNode.rotationAngles) * updatedNode.initialWorldQuaternion
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

    fun deleteNode(nodeId: String) {
        viewModelScope.launch {
            removeNodeUseCase.invoke(nodeId)

            _sceneState.update { sceneState ->
                sceneState.copy(nodesItems = sceneState.nodesItems.filter { it.id != nodeId })
            }
        }
    }

    fun addNode(context: Context, x: Float, y: Float, uri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            val frame = _currentFrame.value ?: throw IllegalStateException("AR Frame is not initialized")

            val mapper = nodeMapper ?: throw IllegalStateException("AR scene dependencies is not initialized")

            if (frame.camera.trackingState != TrackingState.TRACKING) {
                return@launch
            }

            addNodeUseCase.invoke(frame, x, y, ImageSource.BitmapSource(uri!!))
                .onSuccess { createdNode ->
                    val domainNode = mapper.mapToDomainLayer(createdNode)

                    _sceneState.update { sceneState ->
                        sceneState.copy(nodesItems = sceneState.nodesItems + mapper.mapToUILayer(context, domainNode))
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