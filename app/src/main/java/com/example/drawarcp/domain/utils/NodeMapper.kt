package com.example.drawarcp.domain.utils

import com.example.drawarcp.data.models.ARNodeData
import com.example.drawarcp.domain.models.NodeDomainData
import com.example.drawarcp.presentation.uistate.NodeUIState
import com.google.android.filament.Engine
import com.google.ar.core.Session
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.toFloat3
import io.github.sceneview.math.toVector3
import io.github.sceneview.node.ImageNode
import javax.inject.Inject

class NodeMapper @Inject constructor(private val session: Session, private val engine: Engine, private val materialLoader: MaterialLoader) {
    fun mapToDataLayer(domainNode: NodeDomainData): ARNodeData {
        return ARNodeData(
            id = domainNode.id,
            pose = domainNode.pose,
            imageFileLocation = domainNode.imageFileLocation,
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
            imageFileLocation = node.imageFileLocation,
            scale = node.scale,
            initialWorldQuaternion = node.initialWorldQuaternion,
            rotationAngles = node.localAngles,
            normal = node.normal.toFloat3(),
            opacity = node.alpha
        )
    }

    fun mapToUILayer(node: NodeDomainData): NodeUIState {
        val anchor = session.createAnchor(node.pose)

        val anchorNode = AnchorNode(engine = engine, anchor = anchor)

        val imageNode = ImageNode(
            materialLoader = materialLoader,
            imageFileLocation = node.imageFileLocation,
            normal = node.normal
        ).apply {
            scale = node.scale
            quaternion = node.initialWorldQuaternion * Quaternion.fromEuler(node.rotationAngles)
        }

        anchorNode.addChildNode(imageNode)

        return NodeUIState(
            id = node.id,
            node = anchorNode,
            scale = node.scale,
            rotationAngles = node.rotationAngles,
            opacity = node.opacity,
            )
    }
}