package com.example.drawarcp.data.ar

import android.util.Log
import com.example.drawarcp.data.models.ARNodeData
import com.example.drawarcp.domain.interfaces.IARPlacementController
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Point
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.lookAt
import dev.romainguy.kotlin.math.normal
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.quaternion
import io.github.sceneview.ar.arcore.rotation
import io.github.sceneview.collision.Vector3
import io.github.sceneview.collision.Vector3Evaluator
import io.github.sceneview.math.lookTowards
import io.github.sceneview.math.toFloat3
import io.github.sceneview.math.toVector3
import java.util.UUID
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.acos
import kotlin.math.sqrt

class ARPlacementController: IARPlacementController {
    override fun createNodeAt(
        frame: Frame,
        x: Float,
        y: Float,
        imageFileLocation: String,
    ): Result<ARNodeData> {

        val hit = frame.hitTest(x, y)
            .firstOrNull { hit ->
                when (val trackable = hit.trackable!!) {
                    is Plane, is DepthPoint, is Point -> true
                    else -> false
                }
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
           io.github.sceneview.collision.Quaternion.lookRotation(normalVector.negated(), Vector3.forward() )
        }

        return Result.success(
            ARNodeData(
                id = UUID.randomUUID().toString(),
                pose = pose,
                imageFileLocation = imageFileLocation,
                scale = 0.8f,
                initialWorldQuaternion = Quaternion(quaternion.x, quaternion.y, quaternion.z, quaternion.w),
                localAngles = Float3(0f, 0f, 0f),
                normal = normalVector,
                alpha = 255,
            )
        )
    }
}