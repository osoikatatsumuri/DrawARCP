package com.example.drawarcp.data.ar

import com.example.drawarcp.data.models.ARNodeData
import com.example.drawarcp.data.models.ImageSource
import com.example.drawarcp.domain.interfaces.IARPlacementController
import com.google.android.filament.utils.Ray
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Trackable
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.hitTest
import io.github.sceneview.ar.scene.PlaneRenderer
import io.github.sceneview.collision.Vector3
import io.github.sceneview.math.toFloat3
import io.github.sceneview.math.toVector3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.absoluteValue

class ARPlacementController: IARPlacementController {
    override fun createNodeAt(
        frame: Frame,
        x: Float,
        y: Float,
        imageSource: ImageSource,
    ): Result<ARNodeData> {
        val hit = frame.hitTest(x, y).firstOrNull {
            val trackable = it.trackable
            trackable is Plane || trackable is DepthPoint
        } ?: return Result.failure(Exception("Can't perform hit test!"))

        val anchor = hit.createAnchorOrNull() ?: return Result.failure(Exception("Anchor can't be created"))

        val pose = anchor.pose

        val hitPose = (hit.trackable as? Plane)?.centerPose ?: hit.hitPose

        var normalVector = hitPose.yAxis.toFloat3().toVector3()

        val scalarProduct = Vector3.dot(normalVector, Vector3.up()).absoluteValue

        val quaternion = if (scalarProduct < 0.1f) {
            val planeUp = Vector3.cross(normalVector.negated().normalized(), Vector3.up())
                .normalized()

            val planeForward = Vector3.cross(planeUp, normalVector.negated().normalized()).normalized()

            io.github.sceneview.collision.Quaternion.lookRotation(planeForward, planeUp)
        } else {
           io.github.sceneview.collision.Quaternion.lookRotation(normalVector.negated(), Vector3.forward())
        }

        return Result.success(
            ARNodeData(
                id = UUID.randomUUID().toString(),
                pose = pose,
                imageSource = imageSource,
                scale = Float3(0.8f, 0.8f, 0.8f),
                initialWorldQuaternion = Quaternion(quaternion.x, quaternion.y, quaternion.z, quaternion.w),
                localAngles = Float3(0f, 0f, 0f),
                normal = normalVector,
                alpha = 255,
            )
        )
    }
}