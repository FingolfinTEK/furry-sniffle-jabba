package com.fingolfintek.swgohgg.unit

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
open class CharacterRepository : UnitRepositorySupport() {

  private val logger = LoggerFactory.getLogger(javaClass)

  @PostConstruct
  private fun populate() {
    units = extractUnitsFrom("https://swgoh.gg/")
    logger.info("Loaded ${units.size()} characters")
  }
}