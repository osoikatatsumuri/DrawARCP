package com.example.drawarcp.presentation.screens.arscene

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drawarcp.data.ar.transformation.TransformationType
import com.example.drawarcp.presentation.components.VerticalSlider
import com.example.drawarcp.presentation.uistate.nodes.AnchorNodeUIState
import com.example.drawarcp.presentation.viewmodels.ARSceneViewModel
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Session
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.collision.Vector3
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import kotlinx.coroutines.FlowPreview
import java.util.EnumSet


@SuppressLint("NewApi")
@Composable
fun ARSceneScreen(viewModel: ARSceneViewModel) {
    val context = LocalContext.current
    var surfaceSize by remember { mutableStateOf(IntSize(0, 0)) }

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val arCameraStream = rememberARCameraStream(materialLoader = materialLoader)
    val arCameraNode = rememberARCameraNode(engine = engine)
    val view = rememberView(engine = engine)
    var arSession: Session? by remember { mutableStateOf(null) }
    var isDepthModeSupported by remember { mutableStateOf(false) }

    val widthPixels = context.resources.displayMetrics.widthPixels
    val heightPixels = context.resources.displayMetrics.heightPixels

    val sceneState by viewModel.sceneState.collectAsStateWithLifecycle()
    var sheetVisible by remember { mutableStateOf(false) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    val childNodes = sceneState.nodesItems.map { it.node }
    var currentImageUri by remember {mutableStateOf<Uri?>(null)}

    val planePlacementState = sceneState.planePlacementUIState

    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val pickMedia = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        uri?.let { safeUri ->
            currentImageUri = safeUri
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pickMedia.launch("image/*")
        }
    }

    fun handleImagePick() {
        val permissionCheckResult = ContextCompat.checkSelfPermission(context, storagePermission)

        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
            pickMedia.launch("image/*")
        } else {
            permissionLauncher.launch(storagePermission)
        }
    }

    LaunchedEffect(arSession) {
        arSession?.let {
            viewModel.initARDependencies(
                context = context,
                session = it,
                engine = engine,
                materialLoader = materialLoader
            )
        }
    }

    Scaffold(
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Transparent,
                tonalElevation = 4.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (planePlacementState.isPlacement) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(onClick = { viewModel.confirmPlanePlacement() }) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Подтвердить"
                                )
                            }

                            IconButton(onClick = { viewModel.closePlanePlacement() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Отменить"
                                )
                            }
                        }

                    }
                } else {
                    ARBottomBar(
                        onPickImage = { handleImagePick() },
                        onAddPlane = { viewModel.addPlane() },
                        onAddImage = {
                            viewModel.addNode(context, widthPixels / 2f, heightPixels / 2f, currentImageUri)
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .onSizeChanged { newSize ->
                    surfaceSize = newSize
                }
        ) {
            ARScene(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                planeRenderer = true,

                onSessionUpdated = { mSession, updatedFrame ->
                    viewModel.updateCurrentFrame(updatedFrame)

                    arSession = mSession

                    viewModel.updateMapperSession(mSession)
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

                    isDepthModeSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)

                    if (isDepthModeSupported) {
                        config.depthMode = Config.DepthMode.AUTOMATIC
                    } else {
                        config.depthMode = Config.DepthMode.DISABLED
                    }

                    if (session.isImageStabilizationModeSupported(Config.ImageStabilizationMode.EIS)) {
                        Log.d("AR", "EIS stabilization mode supported. Activating it...")

                        config.imageStabilizationMode = Config.ImageStabilizationMode.EIS
                    }

                    config.instantPlacementMode = Config.InstantPlacementMode.DISABLED

                    config.updateMode = Config.UpdateMode.BLOCKING

                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL

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

            if (sceneState.planePlacementUIState.isPlacement) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    VerticalSlider(value = sceneState.planePlacementUIState.currentDistance, onValueChange = {
                        viewModel.updatePlanePlacementDistance(it)
                    }, valueRange = 0f..5f, modifier = Modifier.width(200.dp).height(50.dp))

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${"%.2f".format(sceneState.planePlacementUIState.currentDistance)} м",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (sheetVisible && selectedNodeId != null) {
                TransformNodeSheet(
                    nodeSelected = sceneState.nodesItems.find { it.id == selectedNodeId } as AnchorNodeUIState,
                    onDismiss = { sheetVisible = false },
                    onScaleChange = { scale ->
                        viewModel.applyTransformation(
                            nodeId = selectedNodeId!!,
                            type = TransformationType.SCALE,
                            params = Float3(scale, scale, scale)
                        )
                    },
                    onRotationChange = { eulerAngles ->
                        viewModel.applyTransformation(
                            nodeId = selectedNodeId!!,
                            type = TransformationType.ROTATE,
                            params = eulerAngles
                        )
                    },
                    onOpacityChange = { alphaChannel ->
                        viewModel.applyTransformation(
                            nodeId = selectedNodeId!!,
                            type = TransformationType.OPACITY,
                            params = alphaChannel
                        )
                    },
                    onNodeDelete = { nodeId ->
                        viewModel.deleteNode(nodeId)
                    }
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