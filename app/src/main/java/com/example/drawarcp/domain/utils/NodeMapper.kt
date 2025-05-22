package com.example.drawarcp.domain.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.drawarcp.data.models.ARNodeData
import com.example.drawarcp.data.models.ImageSource
import com.example.drawarcp.domain.models.NodeDomainData
import com.example.drawarcp.presentation.uistate.nodes.AnchorNodeUIState
import com.example.drawarcp.presentation.uistate.nodes.PlaneNodeUIState
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.ar.core.Pose
import com.google.ar.core.Session
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.arcore.position
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.collision.Quaternion
import io.github.sceneview.collision.Vector3
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Color
import io.github.sceneview.math.toFloat3
import io.github.sceneview.math.toVector3
import io.github.sceneview.node.ImageNode
import io.github.sceneview.node.PlaneNode
import io.github.sceneview.texture.TextureSamplerExternal
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class NodeMapper @Inject constructor(
    var session: Session,
    private val engine: Engine,
    private val materialLoader: MaterialLoader
) {
    fun mapToDataLayer(domainNode: NodeDomainData): ARNodeData {
        return ARNodeData(
            id = domainNode.id,
            pose = domainNode.pose,
            imageSource = domainNode.imageSource,
            scale = domainNode.scale,
            initialWorldQuaternion = domainNode.initialWorldQuaternion,
            localAngles = domainNode.rotationAngles,
            normal = domainNode.normal.toVector3(),
            alpha = domainNode.opacity
        )
    }

    fun mapToDomainLayer(node: ARNodeData): NodeDomainData {
        return NodeDomainData(
            id = node.id,
            pose = node.pose,
            imageSource = node.imageSource,
            scale = node.scale,
            initialWorldQuaternion = node.initialWorldQuaternion,
            rotationAngles = node.localAngles,
            normal = node.normal.toFloat3(),
            opacity = node.alpha
        )
    }

    fun createPlaneNode(cameraPose: Pose, cameraLookDirection: Float3): PlaneNodeUIState {

        val center = cameraPose.position + cameraLookDirection * 2f

        val correctRotation = Quaternion.lookRotation(cameraLookDirection.toVector3(), Vector3.up())

        val convertedQuaternion = dev.romainguy.kotlin.math.Quaternion(
            correctRotation.x,
            correctRotation.y,
            correctRotation.z,
            correctRotation.w
        )

        val planePose = Pose(center.toFloatArray(), convertedQuaternion.toFloatArray())

        val planeNode = PlaneNode(
            engine = engine,
            normal = cameraLookDirection.toVector3().normalized().toFloat3(),
            materialInstance = materialLoader.createColorInstance(Color(0.2f, 0.2f, 0.2f, 0.8f), 0f, 0f, 0f)
        )

        val scale = Float3(1.1f, 0.8f, 1f)

        planeNode.scale = Float3(1.1f, 0.8f, 1f)

        val anchorNode = AnchorNode(engine = engine, anchor = session.createAnchor(planePose))

        anchorNode.addChildNode(planeNode)

        anchorNode.isPositionEditable = false

        val id = UUID.randomUUID().toString()

        return PlaneNodeUIState(
            id = id,
            node = anchorNode,
            scale = scale,
            rotation = convertedQuaternion,
            initialNodePosition = cameraPose.position
        )
    }

    fun mapToUILayer(context: Context, node: NodeDomainData, materialInstance: MaterialInstance): AnchorNodeUIState {
        val rotationQuaternion = (dev.romainguy.kotlin.math.Quaternion.fromEuler(node.rotationAngles) * node.initialWorldQuaternion)

        val actualPose = Pose(node.pose.position.toFloatArray(), rotationQuaternion.toFloatArray())

        val anchorNode = AnchorNode(engine = engine, anchor = session.createAnchor(actualPose))

        val imageNode = when (node.imageSource) {
            is ImageSource.FileSource -> {
                ImageNode(
                    materialLoader = materialLoader,
                    normal = node.normal,
                    imageFileLocation = node.imageSource.path
                )
            }

            is ImageSource.BitmapSource -> {
                val bitmap = uriToBitmap(context, node.imageSource.uri)

                ImageNode(
                    materialLoader = materialLoader,
                    normal = node.normal,
                    bitmap = bitmap!!,
                    textureSampler = TextureSamplerExternal(),
                )
            }
        }.apply {
            scale = node.scale
        }

        materialInstance.setParameter("texture", imageNode.texture, TextureSamplerExternal())
        materialInstance.setParameter("opacity", node.opacity / 255f)

        imageNode.materialInstance = materialInstance

        anchorNode.addChildNode(imageNode)

        return AnchorNodeUIState(
            id = node.id,
            node = anchorNode,
            scale = node.scale,
            rotationAngles = node.rotationAngles,
            opacity = node.opacity,
        )
    }
}

fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    val resolver: ContentResolver = context.contentResolver

    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)

            val bitmap = ImageDecoder.decodeBitmap(source)

            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            resolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = MediaStore.Images.Media.getBitmap(resolver, uri)
                bitmap.copy(Bitmap.Config.ARGB_8888, true)
            }
        }
    } catch (e: IOException) {
        Log.e("BitmapLoader", "Failed to load bitmap from URI", e)
        null
    }
}
