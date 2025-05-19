package com.example.drawarcp.domain.interfaces

import com.example.drawarcp.data.models.ARNodeData
import com.example.drawarcp.data.models.ImageSource
import com.google.ar.core.Frame
import com.google.ar.core.Pose

interface IARPlacementController {
    fun createNodeAt(frame: Frame, x: Float, y: Float, imageSource: ImageSource): Result<ARNodeData>
}