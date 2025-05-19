package com.example.drawarcp.presentation.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.BottomAppBar
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
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.decodeBitmap
import androidx.core.net.toFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drawarcp.data.ar.transformation.TransformationType
import com.example.drawarcp.data.models.ImageSource
import com.example.drawarcp.presentation.RequirePermission
import com.example.drawarcp.presentation.uistate.nodes.AnchorNodeUIState
import com.example.drawarcp.presentation.viewmodels.ARSceneViewModel
import com.example.drawarcp.presentation.viewmodels.PermissionsViewModel
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
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

    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_IMAGES
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
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
                session = it,
                engine = engine,
                materialLoader = materialLoader
            )
        }
    }

    Scaffold(
        bottomBar = {
            BottomAppBar(
                containerColor = Color.White.copy(alpha = 0.95f),
                tonalElevation = 4.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            handleImagePick()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .weight(1f)
                    ) {
                        Text(text = "Выбрать новое изображение", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    TextButton(
                        onClick = {
                            viewModel.addNode(context, widthPixels / 2f, heightPixels / 2f, currentImageUri)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .weight(1f)
                    ) {
                        Text(text = "Добавить изображение", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
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

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun TransformNodeSheet(
    onDismiss: () -> Unit,
    nodeSelected: AnchorNodeUIState,
    onScaleChange: (Float) -> Unit,
    onOpacityChange: (Int) -> Unit,
    onRotationChange: (Float3) -> Unit,
    onNodeDelete: (String) -> Unit,
) {

    var scale by remember(nodeSelected.id) { mutableFloatStateOf(nodeSelected.scale.x) }
    var opacity by remember(nodeSelected.id) { mutableIntStateOf(nodeSelected.opacity) }

    var rotationAngles by remember(nodeSelected.id) { mutableStateOf(nodeSelected.rotationAngles) }

    val debouncedOpacity by rememberUpdatedState(opacity)

    LaunchedEffect(debouncedOpacity) {
        snapshotFlow { debouncedOpacity }
            .debounce(100)
            .collectLatest {
                onOpacityChange(it)
            }
    }

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

            Text(
                "Rotation: ${
                    "%.0f".format(
                        rotationAngles.toFloatArray()[ when (selectedAxis.first) {
                            "X" -> 0
                            "Y" -> 1
                            else -> 2
                        } ]
                    )
                }° on ${selectedAxis.first}"
            )

            Slider(
                value = rotationAngles.toFloatArray()[ when (selectedAxis.first) {
                    "X" -> 0
                    "Y" -> 1
                    else -> 2
                } ],
                onValueChange = { newValue ->
                    val (x, y, z) = rotationAngles.toFloatArray()
                    val updated = when (selectedAxis.first) {
                        "X" -> Float3(newValue, y, z)
                        "Y" -> Float3(x, newValue, z)
                        else -> Float3(x, y, newValue)
                    }
                    rotationAngles = updated
                    onRotationChange(updated)
                },
                valueRange = 0f..360f
            )

            Text("Прозрачность: $opacity")
            Slider(
                value = opacity.toFloat(),
                onValueChange = {
                    opacity = it.toInt()
                },
                valueRange = 0f..255f
            )

            TextButton(
                onClick = {
                    onDismiss()

                    onNodeDelete(nodeSelected.id)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .height(6.dp)
                    .weight(1f)
            ) {
                Text(text = "Удалить узел", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}