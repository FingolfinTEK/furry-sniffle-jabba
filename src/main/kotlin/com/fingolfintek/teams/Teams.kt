package com.fingolfintek.teams

import com.fingolfintek.swgohgg.player.CollectedUnit
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("teams")
open class Teams {
  var tw = TerritoryWar()

  open class TerritoryWar {
    var defense = SquadTemplateRequirements()
  }

  open class SquadTemplateRequirements {
    var defaultRequirements = TeamRequirements()
    var characterRequirements: Map<String, CharacterRequirements> = LinkedHashMap()
    var templates: Map<String, SquadTemplate> = LinkedHashMap()
  }


  open class TeamRequirements : CharacterRequirements() {
    var minTotalPower: Int = 0
  }

  open class SquadTemplate {
    var tier = 0
    var tags = ArrayList<String>()
    var characters = ArrayList<SquadTemplateEntry>()

    fun isFulfilledBy(units: io.vavr.collection.Map<String, CollectedUnit>): Boolean {
      val fulfillments = characters.map { unitReq ->
        units[unitReq.name]
            .map { unitReq.isFulfilledBy(it) }
            .getOrElse(false)
      }
      return fulfillments.reduce({ b1, b2 -> b1 && b2 })
    }

    fun hasMinTotalPower(
        units: io.vavr.collection.Map<String, CollectedUnit>, minPower: Int): Boolean {

      val teamPower = characters.map { units[it.name].map { it.power }.getOrElse(0) }.sum()
      return teamPower >= minPower
    }
  }

  open class SquadTemplateEntry {
    var name = ""
    var requirements = CharacterRequirements()

    fun isFulfilledBy(unit: CollectedUnit): Boolean {
      return requirements.isFulfilledBy(unit)
    }
  }

  open class CharacterRequirements {
    var minLevel: Int = 0
    var minCharPower: Int = 0
    var minRarity: Int = 0
    var minGearLevel: Int = 0
    var zetas: List<String> = ArrayList()

    fun isFulfilledBy(unit: CollectedUnit): Boolean {
      return unit.level >= minLevel
          && unit.power >= minCharPower
          && unit.rarity >= minRarity
          && unit.gear_level >= minGearLevel
          && unit.zetas.containsAll(zetas)
    }

    override fun toString(): String =
        "CharacterRequirements(" +
            "minLevel=$minLevel, minCharPower=$minCharPower, " +
            "minRarity=$minRarity, minGearLevel=$minGearLevel, zetas=$zetas)"
  }
}
