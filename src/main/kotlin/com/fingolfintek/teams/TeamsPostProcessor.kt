package com.fingolfintek.teams

import com.fingolfintek.swgohgg.unit.UnitRepository
import com.fingolfintek.teams.Teams.*
import io.vavr.control.Option
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
open class TeamsPostProcessor(
    private val teamDefinitions: Teams,
    private val unitRepository: UnitRepository) {

  private val logger = LoggerFactory.getLogger(javaClass)

  @PostConstruct
  private fun finishInit() {
    postProcess(teamDefinitions)
  }

  open fun postProcess(teams: Teams) {
    logger.info(
        "Loaded {} defensive teams for TW",
        teams.tw.defense.templates.size)

    teams.tw.replaceUnitNames(unitRepository)
    teams.tw.populateDefaults()
  }

  private fun TerritoryWar.replaceUnitNames(unitRepository: UnitRepository) {
    defense.replaceUnitNames(unitRepository)
  }

  private fun TerritoryWar.populateDefaults() {
    defense.populateDefaults()
  }

  private fun SquadTemplateRequirements.replaceUnitNames(unitRepository: UnitRepository) {
    templates.values.forEach { it.replaceUnitNames(unitRepository) }

    characterRequirements = characterRequirements
        .mapKeys {
          unitRepository.searchByName(it.key)
              .map { it.name }
              .getOrElse(it.key)
        }
  }

  private fun SquadTemplateRequirements.populateDefaults() {
    characterRequirements.values.forEach { it.populateFrom(defaultRequirements) }

    templates = templates.mapValues {
      it.value.characters.forEach {
        val requirements = Option
            .of(characterRequirements[it.name])
            .getOrElse(defaultRequirements)

        it.requirements.populateFrom(requirements!!)
      }
      it.value
    }
  }

  private fun SquadTemplate.replaceUnitNames(unitRepository: UnitRepository) {
    characters.forEach {
      unitRepository.searchByName(it.name)
          .peek { unit -> it.name = unit.name }
    }
  }

  private fun CharacterRequirements.populateFrom(defaults: CharacterRequirements) {
    minLevel = if (minLevel == 0) defaults.minLevel else minLevel
    minCharPower = if (minCharPower == 0) defaults.minCharPower else minCharPower
    minRarity = if (minRarity == 0) defaults.minRarity else minRarity
    minGearLevel = if (minGearLevel == 0) defaults.minGearLevel else minGearLevel
    zetas = if (zetas.isEmpty()) defaults.zetas else zetas
  }
}
