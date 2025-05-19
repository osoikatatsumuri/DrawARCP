package com.example.drawarcp.domain.usecases

import com.example.drawarcp.data.models.ARNodeData
import com.example.drawarcp.data.models.ImageSource
import com.example.drawarcp.domain.interfaces.IARPlacementController
import com.google.ar.core.Frame

class AddNodeUseCase(private val placementController: IARPlacementController) {
    operator fun invoke(frame: Frame, x: Float, y: Float, imageSource: ImageSource): Result<ARNodeData> {
        return placementController.createNodeAt(frame, x, y, imageSource)
    }
}