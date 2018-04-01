package com.fingolfintek.swgohgg.guild

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fingolfintek.swgohgg.player.CollectedUnit
import com.fingolfintek.swgohgg.player.PlayerCollection
import com.fingolfintek.swgohgg.unit.UnitRepository
import com.fingolfintek.util.toVavrMap
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

  @Cacheable(cacheNames = ["guilds"], key = "#swgohGgUrl")
  open fun getForGuildUrl(swgohGgUrl: String): List<PlayerCollection> {
    logger.info("Fetching rosters for $swgohGgUrl")

    return fetchZetaCollectionFor(swgohGgUrl)
        .let { zetas ->
          fetchCharacterCollectionFor(swgohGgUrl)
              .map { it.withZetas(zetas) }
              .toList()
        }
  }

  private fun fetchZetaCollectionFor(swgohGgUrl: String): Map<String, Map<String, Set<String>>> {
    return Jsoup
        .connect("${swgohGgUrl.removeSuffix("/")}/zetas/")
        .header("User- Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:52.0) Gecko/20100101 Firefox/52.0")
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .header("Host", "swgoh.gg")
        .execute()
        .parse()
        .select(".table > tbody > tr")
        .toVavrMap {
          Tuple.of(
              it.select("td:nth-child(1) a").attr("href"),
              it.select("div.guild-member-zeta")
                  .toVavrMap { zeta ->
                    Tuple.of(
                        zeta.select(".char-portrait").attr("title"),
                        zeta.select("img.guild-member-zeta-ability ")
                            .eachAttr("title")
                            .toSet()
                    )
                  }
          )
        }
  }

  private fun fetchCharacterCollectionFor(swgohGgUrl: String): Iterable<PlayerCollection> {
    val jsonBody = fetchRawJsonFrom(swgohGgUrl)
    val collectedUnits = parseCollectedUnitsFrom(jsonBody)

    val unitsByPlayerUrl = collectedUnits
        .groupBy { it.url }

    // bug in swgoh.gg data, no URLs for ships
    val ships = unitsByPlayerUrl.get("")

    val collections = unitsByPlayerUrl
        .remove("")
        .mapValues { collection -> PlayerCollection(collection) }
        .values()

    return ships.map {
      val shipsByPlayerName = it.groupBy { it.player }
      return@map collections.map { collection ->
        shipsByPlayerName[collection.name]
            .map { collection.copy(units = collection.units.appendAll(it)) }
            .getOrElse { collection }
      }
    }.getOrElse { collections }

  }

  private fun fetchRawJsonFrom(swgohGgUrl: String): String {
    val guildId = swgohGgUrl
        .replace("https://swgoh.gg/g/(\\d+)/.+".toRegex(), "$1")
        .toInt()

    return Jsoup
        .connect("https://swgoh.gg/api/guilds/$guildId/units/")
        .ignoreContentType(true)
        .execute().body()
  }

  private fun parseCollectedUnitsFrom(jsonBody: String): io.vavr.collection.List<CollectedUnit> {
    return mapper
        .readValue<Map<String, List<CollectedUnit>>>(jsonBody)
        .mapKeys { unitRepository.searchById(it) }
        .map { unit, collection ->
          Tuple.of(unit, collection.map { c -> c.copy(unit = unit) })
        }
        .values()
        .flatMap { it }
        .toList()
  }

}
