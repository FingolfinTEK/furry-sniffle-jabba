package com.fingolfintek.swgohgg.unit

import io.vavr.collection.Set

data class Unit(
    val name: String,
    val tags: Set<String>
)