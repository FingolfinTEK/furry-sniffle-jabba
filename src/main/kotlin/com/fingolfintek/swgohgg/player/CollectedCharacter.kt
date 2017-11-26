package com.fingolfintek.swgohgg.player

import com.fingolfintek.swgohgg.unit.Unit
import java.io.Serializable

data class CollectedCharacter(
    val character: Unit,
    val level: Int,
    val power: Int,
    val gearLevel: Int,
    val rarity: Int): Serializable {
  
  override fun toString(): String = "${character.name} ${toPowerString()}"

  fun toPowerString(): String = "$rarity☆, L$level, G$gearLevel"
}