package com.fingolfintek.swgohgg.player

import com.fingolfintek.swgohgg.character.Character

data class CollectedCharacter(
    val character: Character,
    val level: Int,
    val power: Int,
    val gearLevel: Int,
    val rarity: Int) {
  
  override fun toString(): String {
    return "${character.name} $rarity\u2606, L$level, G$gearLevel"
  }
}