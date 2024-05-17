package com.example.drawarcp

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drawarcp.domain.CustomConverter
import com.example.drawarcp.domain.NodeProcessor
import com.example.drawarcp.ui.theme.DrawARCPTheme
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.TrackingState
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.rotation
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.collision.Vector3
import io.github.sceneview.gesture.ScaleGestureDetector
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ImageNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import java.util.EnumSet


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainViewModel: MainViewModel by viewModels()

        setContent {
            DrawARCPTheme {

                val isNodeLocked by mainViewModel.isNodeLocked.collectAsStateWithLifecycle()

                Scaffold(
                    floatingActionButton = {
                       ExtendedFloatingActionButton(onClick = { mainViewModel.setNodeLockStatus(!isNodeLocked)}) {
                           if (isNodeLocked) {
                               Text("Pick up image")
                           } else {
                               Text("Place down image")
                           }
                       }
                    },
                    floatingActionButtonPosition = FabPosition.Center
                ) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(it)) {
                        ARSceneComponent(mainViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun ARSceneComponent(viewModel: MainViewModel) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    var frame by remember { mutableStateOf<Frame?>(null) }
    val arCameraStream = rememberARCameraStream(materialLoader = materialLoader)
    val arCameraNode = rememberARCameraNode(engine = engine)
    val view = rememberView(engine = engine)

    val childNodes by viewModel.childNodes.collectAsStateWithLifecycle()
    val isImageConfigurationEnabled by viewModel.isImageConfigurationEnabled.collectAsStateWithLifecycle()
    val isNodeLocked by viewModel.isNodeLocked.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentNodeIndex.collectAsStateWithLifecycle()

    var isDepthModeSupported by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val widthPixels = context.resources.displayMetrics.widthPixels
    val heightPixels = context.resources.displayMetrics.heightPixels

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        onSessionUpdated = { mSession, updatedFrame ->
            if (updatedFrame.camera.trackingState != TrackingState.TRACKING) {
                return@ARScene
            }

            frame = updatedFrame

            if (!isNodeLocked) {

                val hitResult = updatedFrame
                    .hitTest(widthPixels / 2f, heightPixels / 2f).firstOrNull {
                        val trackable = it.trackable
                        trackable is Plane || trackable is DepthPoint || trackable is Point
                    }

                if (hitResult != null) {
                    try {
                        val anchor = hitResult.createAnchor()

                        if (anchor != null) {
                            val planeNormal = hitResult.trackable.let {
                                when (it) {
                                    is Plane -> Vector3.dot(CustomConverter.convertFloat3ToVector3(it.centerPose.rotation), Vector3.down())
                                    is DepthPoint -> Vector3.down()
                                    else -> Vector3.down()
                                }

                                Vector3.down()

                            } ?: Vector3.down()

                            if (childNodes.isEmpty()) {
                                val anchorNode = AnchorNode(engine = engine, anchor = anchor)
                                val imageNode = createImageNode(materialLoader = materialLoader, imageFileLocation = "images/rabbit.png", planeNormal = planeNormal)

                                anchorNode.smoothTransformSpeed = 3f
                                anchorNode.isSmoothTransformEnabled = true
                                anchorNode.isPositionEditable = false

                                anchorNode.addChildNode(imageNode)

                                viewModel.addChildNode(anchorNode)

                                viewModel.setNodeIndex(0)
                            } else {
                                val anchorNode = childNodes[currentIndex] as AnchorNode

                                anchorNode.anchor = anchor

                                val oneQuaternion = io.github.sceneview.collision.Quaternion.lookRotation(planeNormal, Vector3.up())

                                anchorNode.childNodes.first().quaternion = Quaternion(oneQuaternion.x, oneQuaternion.y, oneQuaternion.z, oneQuaternion.w)

                                viewModel.changeChildNode(anchorNode, currentIndex)
                            }
                        }
                    } catch (exception: Exception) {
                        Log.d("!!!", "Here?")

                        exception.printStackTrace()

                        throw exception
                    }
                }
            }},

        cameraStream = arCameraStream,
        cameraNode = arCameraNode,
        view = view,

        sessionCameraConfig = {session ->
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

        sessionConfiguration = {session, config ->

            isDepthModeSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
            config.setFocusMode(Config.FocusMode.AUTO)

            if (isDepthModeSupported) {
                config.setDepthMode(Config.DepthMode.AUTOMATIC)
                //arCameraStream.isDepthOcclusionEnabled = true // Now it works very bad with ImageNode
            } else {
                config.setDepthMode(Config.DepthMode.DISABLED)
            }

            config.lightEstimationMode =
                Config.LightEstimationMode.ENVIRONMENTAL_HDR
        },

        childNodes = childNodes,
        onGestureListener = rememberOnGestureListener(
            onDoubleTap = { e, node ->
                if (node != null && isNodeLocked) {
                    val index = node.let { NodeProcessor.findIndex(childNodes,  node) }

                    viewModel.setNodeIndex(index)

                    viewModel.changeImageConfigurationFlag(true)
                }
            },

            onScale = { detector, e, node ->
                if (node != null) {
                    if (gestureTolerance(detector)) {
                        val index = node.let { NodeProcessor.findIndex(childNodes, node) }

                        if (index != -1) {

                            val newScale = node.scale.x * detector.scaleFactor

                            val detectedAnchorNode = childNodes[index]

                            detectedAnchorNode.childNodes.first().transform(scale = Scale(newScale, newScale, newScale))

                            viewModel.changeChildNode(detectedAnchorNode, index)
                        }
                    }
                }
            },
            // TODO: Write own rotation class as gesture listener, rotation works bad with images
        )
    )

    if (isImageConfigurationEnabled) {
        ImageConfigurationScreen(viewModel)
    }
}

fun gestureTolerance(detector: ScaleGestureDetector): Boolean {
    val spanDelta = Math.abs(detector.currentSpan - detector.previousSpan)
    return spanDelta > 7
}

fun createImageNode(materialLoader: MaterialLoader, imageFileLocation: String, planeNormal: Vector3): ImageNode {
    val imageNode = ImageNode(materialLoader = materialLoader, imageFileLocation = imageFileLocation)

    val oneQuaternion = io.github.sceneview.collision.Quaternion.lookRotation(planeNormal, Vector3.up())

    imageNode.quaternion = Quaternion(oneQuaternion.x, oneQuaternion.y, oneQuaternion.z, oneQuaternion.w)

    imageNode.scale = Scale(0.4f, 0.4f, 0.4f)

    imageNode.isSmoothTransformEnabled = true

    imageNode.smoothTransformSpeed = 3f

    imageNode.isPositionEditable = false

    return imageNode
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageConfigurationScreen(viewModel: MainViewModel) {
    val childNodes by viewModel.childNodes.collectAsState()
    val currentIndex by viewModel.currentNodeIndex.collectAsState()
    val anchorNode = childNodes[currentIndex]
    val currentImageNode = childNodes[currentIndex].childNodes.first() as ImageNode

    var rotationAngle by remember { mutableStateOf(0f) }
    var transparency by remember { mutableStateOf(255f) }

    ModalBottomSheet(onDismissRequest = {
        viewModel.changeImageConfigurationFlag(false)
    }) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Adjust Rotation Angle")

            Slider(
                value = rotationAngle,
                onValueChange = {
                    rotationAngle = it
                    val rotationQuaternion = Quaternion.fromAxisAngle(Float3(Vector3.left().x, Vector3.left().y, Vector3.left().z), rotationAngle)
                    currentImageNode.quaternion *= rotationQuaternion
                    viewModel.changeChildNode(anchorNode, currentIndex)
                },
                valueRange = 0f..360f,
                steps = 9
            )

            Text(text = "Adjust image transparency")

            Slider(
                value = transparency,
                onValueChange = {
                    transparency = it

                    val newBitmap = adjustOpacity(currentImageNode.bitmap, transparency.toInt())

                    if (newBitmap != null) {
                        currentImageNode.bitmap = newBitmap
                    }

                    viewModel.changeChildNode(anchorNode, currentIndex)

                },
                valueRange = 0f..255f,
                steps = 10
            )
        }
    }
}

private fun adjustOpacity(bitmap: Bitmap, alpha: Int): Bitmap? {
    val rgbImage = bitmap.copy(Bitmap.Config.ARGB_8888, true)

    if (alpha < 0 || alpha > 255) {
        return null
    }

    for (y in 0 until rgbImage.height) {
        for (x in 0 until rgbImage.width) {
            val argbPixel = rgbImage.getPixel(x, y)
            val newPixel = (argbPixel and 0x00FFFFFF) or (alpha shl 24)
            rgbImage.setPixel(x, y, newPixel)
        }
    }

    return rgbImage
}