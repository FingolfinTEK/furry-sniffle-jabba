package com.fingolfintek.swgohgg.player

import com.fingolfintek.swgohgg.unit.Unit

data class CollectedShip(
    val ship: Unit,
    val level: Int,
    val power: Int,
    val rarity: Int) {

  override fun toString(): String {
    return "${ship.name} $rarity\u2606, L$level"
  }
}