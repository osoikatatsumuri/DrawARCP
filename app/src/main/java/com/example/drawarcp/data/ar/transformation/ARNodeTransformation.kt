package com.example.drawarcp.data.ar.transformation

import com.example.drawarcp.data.models.ARNodeData

interface ARNodeTransformation<T>
{
    fun apply(node: ARNodeData, params: T): ARNodeData
}