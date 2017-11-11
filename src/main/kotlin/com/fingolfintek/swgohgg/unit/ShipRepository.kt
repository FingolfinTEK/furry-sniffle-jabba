package com.fingolfintek.swgohgg.unit

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
open class ShipRepository : UnitRepositorySupport() {

  private val logger = LoggerFactory.getLogger(javaClass)

  @PostConstruct
  private fun populate() {
    units = extractUnitsFrom("https://swgoh.gg/ships")
    logger.info("Loaded ${units.size()} ships")
  }
}