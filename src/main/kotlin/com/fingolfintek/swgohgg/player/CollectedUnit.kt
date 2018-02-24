package com.fingolfintek.swgohgg.player

import com.fingolfintek.swgohgg.unit.Unit
import java.io.Serializable

data class CollectedUnit(
    val unit: Unit = Unit("", -1, "", -1, -1),
    val player: String,
    val rarity: Int,
    val level: Int,
    val power: Int,
    val gear_level: Int,
    val combat_type: Int
) : Serializable {

  override fun toString(): String = "${unit.name} ${toPowerString()}"

  fun toPowerString(): String =
      if (combat_type == 1)
        "$rarity☆, L$level, G$gear_level"
      else
        "$rarity☆, L$level"
}
