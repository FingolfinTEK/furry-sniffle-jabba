package com.fingolfintek.swgohgg.unit

import java.io.Serializable

data class Unit(
    val name: String,
    val power: Int,
    val base_id: String,
    val pk: Int,
    val combat_type: Int
) : Serializable
