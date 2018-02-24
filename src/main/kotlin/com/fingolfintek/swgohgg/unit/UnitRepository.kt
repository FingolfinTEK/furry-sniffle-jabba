package com.fingolfintek.swgohgg.unit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fingolfintek.bot.handler.logger
import io.vavr.collection.List
import io.vavr.control.Option
import io.vavr.control.Try
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
open class UnitRepository(private val mapper: ObjectMapper) {
  private lateinit var units: List<Unit>

  open fun searchByName(name: String): Option<Unit> {
    return Try.ofSupplier { findByExactName(name) }
        .orElse { Try.ofSupplier { findByAbbreviation(name) } }
        .orElse { Try.ofSupplier { findByPartialName(name) } }
        .toOption()
  }

  private fun findByExactName(name: String): Unit =
      units.first { it.name.equals(name, true) }

  private fun findByPartialName(name: String): Unit =
      units.first { it.name.contains(name, true) }

  private fun findByAbbreviation(name: String): Unit =
      units.first {
        it.name.split(Regex("[\\s()]+"))
            .filter { StringUtils.isNotBlank(it) }
            .map { it.trim()[0] }
            .joinToString("")
            .equals(name, true)
      }

  open fun searchById(id: String): Unit {
    return units.first { it.base_id.equals(id, true) }
  }

  @PostConstruct
  private fun populate() {
    val chars = extractUnitsFrom("https://swgoh.gg/api/characters")
    logger.info("Loaded ${chars.size()} characters")

    val ships = extractUnitsFrom("https://swgoh.gg/api/ships")
    logger.info("Loaded ${ships.size()} ships")

    units = chars.appendAll(ships)
  }

  private fun extractUnitsFrom(url: String): List<Unit> {
    return mapper.readValue(Jsoup
        .connect(url)
        .ignoreContentType(true)
        .execute()
        .body()
    )
  }

}
