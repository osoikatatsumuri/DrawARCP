package com.example.drawarcp.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drawarcp.data.ar.transformation.TransformationType
import com.example.drawarcp.presentation.viewmodels.ARSceneViewModel
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.collision.Vector3
import io.github.sceneview.math.toFloat3
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import java.util.EnumSet
import java.util.logging.Logger

@Composable
fun ARSceneScreen(viewModel: ARSceneViewModel) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val arCameraStream = rememberARCameraStream(materialLoader = materialLoader)
    val arCameraNode = rememberARCameraNode(engine = engine)
    val view = rememberView(engine = engine)

    var arSession: Session? by remember { mutableStateOf(null) }
    var frame by remember { mutableStateOf<Frame?>(null) }

    var isDepthModeSupported by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val widthPixels = context.resources.displayMetrics.widthPixels
    val heightPixels = context.resources.displayMetrics.heightPixels

    val sceneState by viewModel.sceneState.collectAsStateWithLifecycle()
    var sheetVisible by remember { mutableStateOf(false) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }

    val childNodes by remember(sceneState.nodesItems) {
        mutableStateOf(sceneState.nodesItems.map {it.node})
    }

    LaunchedEffect(arSession) {
        arSession?.let {
            viewModel.initARDependencies(
                session = it,
                engine = engine,
                materialLoader = materialLoader
            )
        }
    }


    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val selectedFrame = frame

                    if (selectedFrame?.camera?.isTracking == false) {
                        return@ExtendedFloatingActionButton
                    }

                    viewModel.addNode(frame = selectedFrame, x = widthPixels / 2f, y = heightPixels / 2f)
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Add Node")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ARScene(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                onSessionUpdated = { mSession, updatedFrame ->
                    if (updatedFrame.camera.trackingState == TrackingState.TRACKING) {
                        frame = updatedFrame
                    }
                },
                cameraStream = arCameraStream,
                cameraNode = arCameraNode,
                view = view,
                onSessionCreated = { session ->
                    arSession = session
                },
                sessionCameraConfig = { session ->
                    val cameraConfigFilter = CameraConfigFilter(session)

                    cameraConfigFilter.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30))

                    if (isDepthModeSupported) {
                        cameraConfigFilter.setDepthSensorUsage(EnumSet.of(CameraConfig.DepthSensorUsage.REQUIRE_AND_USE))
                    } else {
                        cameraConfigFilter.setDepthSensorUsage(EnumSet.of(CameraConfig.DepthSensorUsage.DO_NOT_USE))
                    }

                    val cameraConfigList = session.getSupportedCameraConfigs(cameraConfigFilter)

                    session.cameraConfig = cameraConfigList[0]

                    session.cameraConfig
                },
                sessionConfiguration = { session, config ->
                    config.focusMode = Config.FocusMode.AUTO

                    isDepthModeSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)

                    if (isDepthModeSupported) {
                        Log.d("AR", "DepthMode supported. Activating it...")

                        config.apply { depthMode = Config.DepthMode.AUTOMATIC }
                    } else {
                        config.apply { depthMode = Config.DepthMode.DISABLED }
                    }

                    if (session.isImageStabilizationModeSupported(Config.ImageStabilizationMode.EIS)) {
                        Log.d("AR", "EIS stabilization mode supported. Activating it...")

                        config.apply { imageStabilizationMode = Config.ImageStabilizationMode.EIS }
                    }

                    config.instantPlacementMode = Config.InstantPlacementMode.DISABLED

                    config.lightEstimationMode =
                        Config.LightEstimationMode.ENVIRONMENTAL_HDR
                },
                onGestureListener = rememberOnGestureListener(
                    onDoubleTap = { hitResult, tappedNode ->
                        selectedNodeId = sceneState.nodesItems.find {tappedNode?.parent == it.node}?.id

                        sheetVisible = true
                    }
                ),
                childNodes = childNodes,
            )

            if (sheetVisible && selectedNodeId != null) {
                TransformNodeSheet(
                    nodeId = selectedNodeId!!,
                    onDismiss = { sheetVisible = false },
                    onScaleChange = { scale ->
                        viewModel.applyTransformation(
                            nodeId = selectedNodeId!!,
                            type = TransformationType.SCALE,
                            params = scale
                        )
                    },
                    onRotationChange = { eulerAngles ->
                        viewModel.applyTransformation(
                            nodeId = selectedNodeId!!,
                            type = TransformationType.ROTATE,
                            params = eulerAngles
                        )
                    },
                    onOpacityChange = {

                    },
                )
            }
        }
    }
}

@Composable
fun LocalOrientationAxisChip(rotationAxis: Pair<String, Vector3>, onSelected: () -> Unit) {
    var selected by remember { mutableStateOf(false) }

    FilterChip(
        onClick = {
            onSelected()
            selected = !selected
                  },
        label = { Text(rotationAxis.first) },
        selected = selected,
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = "Done icon",
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else {
            null
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransformNodeSheet(
    nodeId: String,
    onDismiss: () -> Unit,
    onScaleChange: (Float) -> Unit,
    onOpacityChange: (Int) -> Unit,
    onRotationChange: (Float3) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }

    var rotations = remember {
        mutableStateMapOf(
            "X" to Pair(0f, Float3(1f, 0f, 0f)),
            "Y" to Pair(0f, Float3(0f, 1f, 0f)),
            "Z" to Pair(0f, Float3(0f, 0f, 1f))
        )
    }

    var opacity by remember { mutableIntStateOf(255) }

    var selectedAxis by remember {mutableStateOf(Pair("X", Vector3(1f, 0f, 0f)))}

    val rotationAxes = listOf(
        Pair<String, Vector3>("X", Vector3(1f, 0f, 0f)),
        Pair<String, Vector3>("Y", Vector3(0f, 1f, 0f)),
        Pair<String, Vector3>("Z", Vector3(0f, 0f, 1f)),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Трансформация объекта", modifier = Modifier.padding(bottom = 8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                rotationAxes.forEach { axis ->
                    LocalOrientationAxisChip(axis, onSelected = {
                        selectedAxis = axis
                    })
                }
            }

            Text("Масштаб: ${"%.2f".format(scale)}")
            Slider(
                value = scale,
                onValueChange = {
                    scale = it
                    onScaleChange(it)
                },
                valueRange = 0.1f..3f
            )

            Text("Поворот: ${"%.0f".format(rotations[selectedAxis.first]!!.first)}°")
            Slider(
                value = rotations[selectedAxis.first]!!.first,
                onValueChange = {
                    rotations[selectedAxis.first] = Pair(it, rotations[selectedAxis.first]!!.second)
                    onRotationChange(rotations.values.map { it.first }.toFloatArray().toFloat3())
                },
                valueRange = 0f..360f
            )

            Text("Прозрачность: $opacity")
            Slider(
                value = opacity.toFloat(),
                onValueChange = {
                    opacity = it.toInt()
                    onOpacityChange(it.toInt())
                },
                valueRange = 0f..255f
            )
        }
    }
}