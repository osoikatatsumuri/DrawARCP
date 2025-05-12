package com.example.drawarcp.data.ar.transformation

object TransformationFactory {
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> create(type: TransformationType<T>): ARNodeTransformation<T> {
        return when (type) {
            TransformationType.SCALE -> NodeScaleTransformation()
            TransformationType.ROTATE -> NodeRotationTransformation()
            TransformationType.OPACITY -> NodeOpacityTransformation()
        } as ARNodeTransformation<T>
    }
}