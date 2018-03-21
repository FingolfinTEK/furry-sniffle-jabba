package com.fingolfintek.teams

import com.fingolfintek.swgohgg.player.CollectedUnit
import com.fingolfintek.swgohgg.player.PlayerCollection
import com.fingolfintek.swgohgg.unit.UnitRepository
import io.vavr.Tuple
import io.vavr.control.Option
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
@ConfigurationProperties("teams")
open class Teams(
    private val unitRepository: UnitRepository) {

  var tw = TerritoryWar()

  @PostConstruct
  private fun finishInit() {
    tw.replaceUnitNames(unitRepository)
    tw.populateDefaults()
  }
}

open class TerritoryWar {
  var defense = SquadTemplateRequirements()

  fun replaceUnitNames(unitRepository: UnitRepository) {
    defense.replaceUnitNames(unitRepository)
  }

  fun populateDefaults() {
    defense.populateDefaults()
  }
}

open class SquadTemplateRequirements {
  var defaultRequirements = TeamRequirements()
  var characterRequirements: Map<String, CharacterRequirements> = LinkedHashMap()
  var templates: Map<String, SquadTemplate> = LinkedHashMap()

  fun replaceUnitNames(unitRepository: UnitRepository) {
    templates.values.forEach { it.replaceUnitNames(unitRepository) }

    characterRequirements = characterRequirements
        .mapKeys {
          unitRepository.searchByName(it.key)
              .map { it.name }
              .getOrElse(it.key)
        }
  }

  fun populateDefaults() {
    characterRequirements.values.forEach { it.populateFrom(defaultRequirements) }

    templates = templates.mapValues {
      it.value.forEach {
        val requirements = Option
            .of(characterRequirements[it.name])
            .getOrElse(defaultRequirements)

        it.requirements.populateFrom(requirements!!)
      }
      it.value
    }
  }

  fun compatibleTeamsFor(collection: PlayerCollection): PlayerTeamCollection {
    val unitsByName = collection.units
        .toMap { it -> Tuple.of(it.unit.name, it) }

    val teams = templates
        .filter {
          it.value.isFulfilledBy(unitsByName) && hasMinTotalPower(it.value, unitsByName)
        }
        .map {
          val units = it.value.map { unitsByName[it.name].get() }
          return@map Team(it.key, units)
        }

    return PlayerTeamCollection(collection.name, teams)
  }

  private fun hasMinTotalPower(
      squad: SquadTemplate,
      units: io.vavr.collection.Map<String, CollectedUnit>): Boolean {
    val teamPower = squad.map { units[it.name].map { it.power }.get() }.sum()
    return teamPower >= defaultRequirements.minTotalPower
  }
}

open class TeamRequirements : CharacterRequirements() {
  var minTotalPower: Int = 0
}

open class SquadTemplate : ArrayList<SquadTemplateEntry>() {

  fun replaceUnitNames(unitRepository: UnitRepository) {
    forEach {
      unitRepository.searchByName(it.name)
          .peek { unit -> it.name = unit.name }
    }
  }

  fun isFulfilledBy(units: io.vavr.collection.Map<String, CollectedUnit>): Boolean {
    val fulfillments = map { unitReq ->
      units[unitReq.name]
          .map { unitReq.isFulfilledBy(it) }
          .getOrElse(false)
    }
    return fulfillments.reduce({ b1, b2 -> b1 && b2 })
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
  var zetas: List<String> = emptyList()

  fun populateFrom(defaults: CharacterRequirements) {
    minLevel = if (minLevel == 0) defaults.minLevel else minLevel
    minCharPower = if (minCharPower == 0) defaults.minCharPower else minCharPower
    minRarity = if (minRarity == 0) defaults.minRarity else minRarity
    minGearLevel = if (minGearLevel == 0) defaults.minGearLevel else minGearLevel
  }

  fun isFulfilledBy(unit: CollectedUnit): Boolean {
    return unit.level >= minLevel
        && unit.power >= minCharPower
        && unit.rarity >= minRarity
        && unit.gear_level >= minGearLevel
  }

  override fun toString(): String =
      "CharacterRequirements(" +
          "minLevel=$minLevel, minCharPower=$minCharPower, " +
          "minRarity=$minRarity, minGearLevel=$minGearLevel, zetas=$zetas)"
}
