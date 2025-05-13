package com.example.drawarcp.data.ar.transformation

import dev.romainguy.kotlin.math.Float3
import kotlin.reflect.KClass

sealed class TransformationType<T : Any>(val paramType: KClass<T>) {
    object OPACITY : TransformationType<Int>(Int::class)
    object SCALE : TransformationType<Float3>(Float3::class)
    object ROTATE : TransformationType<Float3>(Float3::class)
}