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
  open fun getForGuildUrl(swgohGgUrl: String): Map<String, PlayerCollection> {
    logger.info("Fetching rosters for $swgohGgUrl")

    val zetas = fetchZetaCollectionFor(swgohGgUrl)
    return fetchCharacterCollectionFor(swgohGgUrl)
        .mapValues { it.withZetas(zetas) }
  }

  private fun fetchCharacterCollectionFor(swgohGgUrl: String): Map<String, PlayerCollection> {
    val guildId = swgohGgUrl
        .replace("https://swgoh.gg/g/(\\d+)/.+".toRegex(), "$1")
        .toInt()

    val jsonBody = Jsoup
        .connect("https://swgoh.gg/api/guilds/$guildId/units/")
        .ignoreContentType(true)
        .execute().body()

    return mapper
        .readValue<Map<String, List<CollectedUnit>>>(jsonBody)
        .mapKeys { unitRepository.searchById(it) }
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
              it.select("td:nth-child(1)").text(),
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

}
