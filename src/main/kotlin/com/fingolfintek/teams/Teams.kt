package com.fingolfintek.teams

import com.fingolfintek.swgohgg.player.CollectedUnit
import com.fingolfintek.swgohgg.unit.UnitRepository
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
  var offense = SquadTemplateRequirements()
  var defense = SquadTemplateRequirements()

  fun replaceUnitNames(unitRepository: UnitRepository) {
    offense.replaceUnitNames(unitRepository)
    defense.replaceUnitNames(unitRepository)
  }

  fun populateDefaults() {
    offense.populateDefaults()
    defense.populateDefaults()
  }
}

open class SquadTemplateRequirements {
  var defaultRequirements = TeamRequirements()
  var characterRequirements: Map<String, CharacterRequirements> = LinkedHashMap()
  var templates: Map<String, SquadTemplate> = LinkedHashMap()

  fun replaceUnitNames(unitRepository: UnitRepository) {
    templates.values.forEach { it.replaceUnitNames(unitRepository) }

    characterRequirements.mapKeys {
      unitRepository.searchByName(it.key)
          .map { it.name }
          .getOrElse(it.key)
    }
  }

  fun populateDefaults() {
    characterRequirements.values.forEach { it.populateFrom(defaultRequirements) }
    templates.mapValues {
      it.value.forEach {
        val requirements = characterRequirements[it.name]
        it.requirements.populateFrom(requirements!!)
      }
      it.value
    }
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
}

open class SquadTemplateEntry {
  var name = ""
  var requirements = CharacterRequirements()
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

  fun fulfills(unit: CollectedUnit): Boolean {
    return unit.level >= minLevel
        && unit.power >= minCharPower
        && unit.rarity >= minRarity
        && unit.gear_level >= minGearLevel
  }
}
