package com.fingolfintek.swgohgg.player

import io.vavr.collection.List

data class PlayerCollection(
    val name: String,
    val characters: List<CollectedCharacter>,
    val ships: List<CollectedShip>
)