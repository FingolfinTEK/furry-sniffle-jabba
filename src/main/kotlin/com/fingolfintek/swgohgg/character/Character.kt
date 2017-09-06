package com.fingolfintek.swgohgg.character

import io.vavr.collection.Set

data class Character(
    val name: String,
    val tags: Set<String>
)