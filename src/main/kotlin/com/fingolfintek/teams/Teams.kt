package com.fingolfintek.teams

import com.fingolfintek.swgohgg.player.CollectedUnit
import com.fingolfintek.util.toVavrMap
import io.vavr.Tuple
import io.vavr.Tuple2
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("teams")
open class Teams {
  var defaultRequirements = TeamRequirements()
  var characterRequirements: Map<String, CharacterRequirements> = LinkedHashMap()
  var templates: Map<String, SquadTemplate> = LinkedHashMap()

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
        units: io.vavr.collection.Map<String, CollectedUnit>,
        minPower: Int): Boolean {
      return characters.map { unitReq ->
        units[unitReq.name].map { it.power }.getOrElse(0)
      }.sum() >= minPower
    }

    fun stepsToFulfilmentFor(
        units: io.vavr.collection.Map<String, CollectedUnit>): Map<Any, List<String>> {
      return characters.toVavrMap { unitReq ->
        units[unitReq.name]
            .map<Tuple2<Any, List<String>>> { Tuple.of(it, unitReq.stepsToFulfilmentFor(it)) }
            .getOrElse { Tuple.of(unitReq.name, listOf("needs to unlock ${unitReq.name}")) }
      }.toJavaMap()
    }
  }

  open class SquadTemplateEntry {
    var name = ""
    var requirements = CharacterRequirements()

    fun isFulfilledBy(unit: CollectedUnit): Boolean {
      return requirements.isFulfilledBy(unit)
    }

    fun stepsToFulfilmentFor(unit: CollectedUnit): List<String> {
      return requirements.stepsToFulfilmentFor(unit)
    }
  }

  open class CharacterRequirements {
    var minLevel: Int = 0
    var minCharPower: Int = 0
    var minRarity: Int = 0
    var minGearLevel: Int = 0
    var zetas: List<String> = ArrayList()

    fun isFulfilledBy(unit: CollectedUnit): Boolean {
      return stepsToFulfilmentFor(unit).isEmpty()
    }

    fun stepsToFulfilmentFor(unit: CollectedUnit): List<String> {
      val steps = ArrayList<String>()
      val conditions = ArrayList<Pair<() -> Boolean, () -> Unit>>()

      conditions += Pair({ unit.level < minLevel }, { steps += "needs to be L$minLevel" })
      conditions += Pair({ unit.power < minCharPower }, { steps += "needs to have $minCharPower GP" })
      conditions += Pair({ unit.rarity < minRarity }, { steps += "needs to be $minRarity*" })
      conditions += Pair({ unit.gear_level < minGearLevel }, { steps += "needs to be G$minGearLevel" })
      conditions += Pair({ !unit.zetas.containsAll(zetas) }, { steps += "needs ${zetas.toSet() - unit.zetas}" })

      conditions.forEach { if (it.first.invoke()) it.second.invoke() }

      return steps
    }

    override fun toString(): String =
        "CharacterRequirements(" +
            "minLevel=$minLevel, minCharPower=$minCharPower, " +
            "minRarity=$minRarity, minGearLevel=$minGearLevel, zetas=$zetas)"
  }
}
