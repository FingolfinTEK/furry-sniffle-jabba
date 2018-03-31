package com.fingolfintek.teams

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

  open class SquadTemplate : ArrayList<SquadTemplateEntry>()

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

    override fun toString(): String =
        "CharacterRequirements(" +
            "minLevel=$minLevel, minCharPower=$minCharPower, " +
            "minRarity=$minRarity, minGearLevel=$minGearLevel, zetas=$zetas)"
  }
}
