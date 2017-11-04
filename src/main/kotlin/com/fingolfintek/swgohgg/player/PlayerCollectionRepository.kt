package com.fingolfintek.swgohgg.player

import com.fingolfintek.swgohgg.unit.CharacterRepository
import com.fingolfintek.swgohgg.unit.ShipRepository
import com.fingolfintek.util.htmlOf
import io.vavr.Tuple
import io.vavr.Tuple2
import io.vavr.collection.List
import io.vavr.control.Try
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
open class PlayerCollectionRepository(
    private val characterRepository: CharacterRepository,
    private val shipRepository: ShipRepository) {

  private val logger = LoggerFactory.getLogger(javaClass)

  @Cacheable(cacheNames = arrayOf("collections"), key = "#basePlayerUrl")
  open fun getForPlayerBaseUrl(basePlayerUrl: String): PlayerCollection {
    return Try.ofSupplier { rosterPagesFor(basePlayerUrl) }
        .map { playerCollectionFrom(it) }
        .onSuccess { logger.info("Collected player roster for ${it.name}") }
        .onFailure { logger.error("Could not fetch player roster from $basePlayerUrl", it) }
        .getOrElse { PlayerCollection("N/A", List.empty(), List.empty()) }
  }

  private fun rosterPagesFor(basePlayerUrl: String): Tuple2<Document, Document> {
    return Tuple.of(
        htmlOf("${basePlayerUrl}collection"),
        htmlOf("${basePlayerUrl}ships")
    )
  }

  private fun playerCollectionFrom(charactersAndShipsPages: Tuple2<Document, Document>) =
      PlayerCollection(
          playerNameFrom(charactersAndShipsPages._1),
          characterRosterFor(charactersAndShipsPages._1),
          shipsRosterFor(charactersAndShipsPages._2)
      )

  private fun playerNameFrom(playerPage: Document) =
      playerPage.select(".char-name")[1].text()

  private fun characterRosterFor(playerPage: Document): List<CollectedCharacter> {
    return List.ofAll(
        playerPage
            .select(".collection-char:not(.collection-char-missing)")
            .map { characterEntryFrom(it) }
    )
  }

  private fun characterEntryFrom(characterDiv: Element): CollectedCharacter {
    return CollectedCharacter(
        characterFrom(characterDiv),
        characterLevelFrom(characterDiv),
        characterPowerFrom(characterDiv),
        gearLevelFrom(characterDiv),
        characterStarsFrom(characterDiv)
    )
  }

  private fun characterFrom(characterDiv: Element) =
      characterRepository.searchByName(characterDiv.select(".collection-char-name-link").text()).get()

  private fun characterLevelFrom(characterDiv: Element) =
      characterDiv.select(".char-portrait-full-level").text().toInt()

  private fun characterPowerFrom(characterDiv: Element): Int =
      characterDiv.select(".collection-char-gp")
          .attr("title")
          .replace(",", "")
          .replace(Regex("Power (\\d+).+"), "$1")
          .toInt()

  private fun gearLevelFrom(it: Element): Int =
      it.select(".char-portrait-full")
          .first()
          .classNames()
          .filter { it.startsWith("char-portrait-full-gear-t") }
          .map { it.removePrefix("char-portrait-full-gear-t") }
          .map { it.toInt() }
          .first()

  private fun characterStarsFrom(it: Element) = it.select(".star:not(.star-inactive)").size

  private fun shipsRosterFor(shipsPage: Document): List<CollectedShip> {
    return List.ofAll(
        shipsPage
            .select(".collection-ship:not(.collection-ship-missing)")
            .map { shipEntryFrom(it) }
    )
  }

  private fun shipEntryFrom(shipDiv: Element): CollectedShip {
    return CollectedShip(
        shipFrom(shipDiv),
        shipLevelFrom(shipDiv),
        shipPowerFrom(shipDiv),
        shipStarsFrom(shipDiv)
    )
  }

  private fun shipFrom(shipDiv: Element) =
      shipRepository.searchByName(shipDiv.select(".collection-ship-name-link").text()).get()

  private fun shipLevelFrom(shipDiv: Element) =
      shipDiv.select(".ship-portrait-full-frame-level").text().toInt()

  private fun shipPowerFrom(shipDiv: Element): Int {
    val shipUri = shipDiv.select(".ship-portrait-full-link").attr("href")
    return htmlOf("https://swgoh.gg$shipUri")
        .select("li.p-sm:nth-child(1) > div:nth-child(1) > div:nth-child(1) > " +
            "div:nth-child(1) > span:nth-child(2)")
        .text().toInt()
  }

  private fun shipStarsFrom(shipDiv: Element) =
      shipDiv.select(".ship-portrait-full-star:not(.ship-portrait-full-star-inactive)").size

}
