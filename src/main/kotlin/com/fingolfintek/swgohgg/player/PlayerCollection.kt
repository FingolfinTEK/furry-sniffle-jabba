package com.fingolfintek.swgohgg.player

import io.vavr.collection.List
import java.io.Serializable

data class PlayerCollection(
    val name: String,
    val units: List<CollectedUnit>
) : Serializable
