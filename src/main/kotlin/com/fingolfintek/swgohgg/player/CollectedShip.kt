package com.fingolfintek.swgohgg.player

import com.fingolfintek.swgohgg.unit.Unit

data class CollectedShip(
    val ship: Unit,
    val level: Int,
    val power: Int,
    val rarity: Int) {

  override fun toString(): String = "${ship.name} ${toPowerString()}"

  fun toPowerString(): String = "$rarity☆, L$level"
}