package com.fingolfintek.swgohgg.guild

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fingolfintek.swgohgg.player.CollectedUnit
import com.fingolfintek.swgohgg.player.PlayerCollection
import com.fingolfintek.swgohgg.unit.UnitRepository
import io.vavr.Tuple
import io.vavr.collection.Map
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
open class GuildRepository(
    private val mapper: ObjectMapper,
    private val unitRepository: UnitRepository) {

  private val logger = LoggerFactory.getLogger(javaClass)

  @Cacheable(cacheNames = arrayOf("guilds"), key = "#swgohGgUrl")
  open fun getForGuildUrl(swgohGgUrl: String): Map<String, PlayerCollection> {
    logger.info("Fetching rosters for $swgohGgUrl")

    val guildId = swgohGgUrl
        .replace("https://swgoh.gg/g/(\\d+)/.+".toRegex(), "$1")
        .toInt()

    val jsonBody = Jsoup
        .connect("https://swgoh.gg/api/guilds/$guildId/units/")
        .ignoreContentType(true)
        .execute().body()

    val units: Map<String, List<CollectedUnit>> = mapper.readValue(jsonBody)

    return units.mapKeys { unitRepository.searchById(it) }
        .map { unit, collection ->
          Tuple.of(unit, collection.map { c -> c.copy(unit = unit) })
        }
        .values()
        .flatMap { it }
        .groupBy { it.player }
        .map { player, collection ->
          Tuple.of(player, PlayerCollection(player, collection.toList()))
        }
  }

}
