package com.fingolfintek.swgohgg.unit

import io.vavr.collection.Set
import java.io.Serializable

data class Unit(
    val name: String,
    val tags: Set<String>
): Serializable