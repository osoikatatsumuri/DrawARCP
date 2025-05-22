package com.example.drawarcp.presentation.viewmodels

import android.content.Context
import android.net.Uri
import android.opengl.Matrix
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drawarcp.data.ar.ARNodeProvider
import com.example.drawarcp.data.ar.transformation.TransformationType
import com.example.drawarcp.data.models.ImageSource
import com.example.drawarcp.domain.usecases.AddNodeUseCase
import com.example.drawarcp.domain.usecases.RemoveNodeUseCase
import com.example.drawarcp.domain.usecases.TransformNodeUseCase
import com.example.drawarcp.domain.utils.NodeMapper
import com.example.drawarcp.presentation.uistate.ARSceneUIState
import com.example.drawarcp.presentation.uistate.PlanePlacementUIState
import com.example.drawarcp.presentation.uistate.nodes.AnchorNodeUIState
import com.example.drawarcp.presentation.uistate.nodes.PlaneNodeUIState
import com.google.android.filament.Engine
import com.google.android.filament.Material
import com.google.android.filament.utils.Float3
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.slerp
import io.github.sceneview.ar.arcore.xDirection
import io.github.sceneview.ar.arcore.zDirection
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.toFloat3
import io.github.sceneview.math.toVector3
import io.github.sceneview.node.ImageNode
import io.github.sceneview.node.PlaneNode
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

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

    var imageMaterial: Material? = null

    fun updateCurrentFrame(frame: Frame) {
        _currentFrame.value = frame
    }

    private var nodeMapper: NodeMapper? = null

    fun initARDependencies(
        context: Context,
        session: Session,
        engine: Engine,
        materialLoader: MaterialLoader
    ) {
        nodeMapper = NodeMapper(session, engine, materialLoader)

        val byteArray = context.assets
            .open("materials/image_texture.filamat")
            .readBytes()

        val buffer = ByteBuffer
            .allocateDirect(byteArray.size)
            .order(ByteOrder.nativeOrder())
            .put(byteArray)
            .flip()

        val material = Material.Builder()
            .payload(buffer, buffer.remaining())
            .build(engine)

        imageMaterial = material
    }

    fun updateMapperSession(newSession: Session) {
        nodeMapper?.session = newSession
    }

    fun <T : Any> applyTransformation(
        nodeId: String,
        type: TransformationType<T>,
        params: T,
    ) = viewModelScope.launch {

        val result = withContext(Dispatchers.Default) {
            transformNodeUseCase.invoke(nodeId, type, params)
        }

        val mapper =
            nodeMapper ?: throw IllegalStateException("AR scene dependencies is not initialized")

        result.onSuccess { updatedNode ->
                provider.updateNode(updatedNode)

                val domainNode = mapper.mapToDomainLayer(updatedNode)

                    (_sceneState.value.nodesItems.find { it.id == updatedNode.id } as AnchorNodeUIState).apply {
                        when (type) {
                            TransformationType.SCALE -> {
                                node.scale = domainNode.scale
                                scale = domainNode.scale
                            }

                            TransformationType.OPACITY -> {
                                (node.childNodes.first() as ImageNode).materialInstance.setParameter(
                                    "opacity",
                                    opacity / 255f
                                )
                                opacity = domainNode.opacity
                            }

                            TransformationType.ROTATE -> {
                                rotationAngles = domainNode.rotationAngles

                                withContext(Dispatchers.Main) {
                                    node.quaternion = updatedNode.initialWorldQuaternion * Quaternion.fromEuler(rotationAngles)
                                }
                            }
                        }
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

    fun updatePlanePlacementDistance(newDistance: Float) {
        _sceneState.update { sceneState ->
            (sceneState.nodesItems.find { it.id == sceneState.planePlacementUIState.id } as PlaneNodeUIState).apply {
                node.position = initialNodePosition + sceneState.planePlacementUIState.directionVector.toFloat3() * newDistance
            }

            sceneState.copy(planePlacementUIState = sceneState.planePlacementUIState.copy(currentDistance = newDistance))
        }
    }

    fun closePlanePlacement() {
        _sceneState.update { sceneState ->
            sceneState.nodesItems.filter { it.id == sceneState.planePlacementUIState.id }

            sceneState.copy(
                planePlacementUIState = sceneState.planePlacementUIState.copy(isPlacement = false))
        }
    }

    fun confirmPlanePlacement() {
        _sceneState.update { sceneState ->
            sceneState.copy(
                planePlacementUIState = sceneState.planePlacementUIState.copy(
                    isPlacement = false
                )
            )
        }
    }

    fun addPlane() {
        if (currentFrame.value == null || currentFrame.value!!.camera.trackingState != TrackingState.TRACKING) {
            return
        }

        val mapper =
            nodeMapper ?: throw IllegalStateException("AR scene dependencies is not initialized")

        val cameraDirection =
            currentFrame.value!!.camera.pose.zDirection.toVector3().normalized().negated()
                .toFloat3()

        val createdPlane = mapper.createPlaneNode(
            currentFrame.value!!.camera.pose,
            cameraDirection
        )

        _sceneState.update { sceneState ->
            sceneState.copy(
                nodesItems = sceneState.nodesItems + createdPlane,
                planePlacementUIState = PlanePlacementUIState(
                    id = createdPlane.id,
                    directionVector = cameraDirection.toVector3(),
                    isPlacement = true),
            )
        }
    }

    fun addNode(context: Context, x: Float, y: Float, uri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            val frame =
                _currentFrame.value ?: throw IllegalStateException("AR Frame is not initialized")

            val mapper = nodeMapper
                ?: throw IllegalStateException("AR scene dependencies is not initialized")

            if (frame.camera.trackingState != TrackingState.TRACKING) {
                return@launch
            }

            addNodeUseCase.invoke(frame, x, y, ImageSource.BitmapSource(uri!!))
                .onSuccess { createdNode ->
                    val domainNode = mapper.mapToDomainLayer(createdNode)

                    _sceneState.update { sceneState ->
                        sceneState.copy(
                            nodesItems = sceneState.nodesItems + mapper.mapToUILayer(
                                context,
                                domainNode,
                                imageMaterial?.createInstance()!!
                            )
                        )
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