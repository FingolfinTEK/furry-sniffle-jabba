package com.fingolfintek.swgohgg.unit

import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
open class CharacterRepository : UnitRepositorySupport() {

  @PostConstruct
  private fun populate() {
    units = extractUnitsFrom("https://swgoh.gg/")
  }
}