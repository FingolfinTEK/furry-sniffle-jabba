package com.fingolfintek.swgohgg.player

import com.fingolfintek.swgohgg.character.CharacterRepository
import com.fingolfintek.util.htmlOf
import io.vavr.collection.List
import io.vavr.control.Try
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
open class PlayerCollectionRepository(
    private val characterRepository: CharacterRepository) {

  private val logger = LoggerFactory.getLogger(javaClass)

  @Cacheable(cacheNames = arrayOf("collections"), key = "#swgohGgUrl")
  open fun getForPlayerCollectionUrl(swgohGgUrl: String): PlayerCollection {
    return Try.ofSupplier { htmlOf(swgohGgUrl) }
        .map { playerCollectionFrom(it) }
        .onSuccess { logger.info("Collected player roster from $swgohGgUrl: ${it.collection.size()} chars") }
        .onFailure { logger.error("Could not fetch player roster from $swgohGgUrl") }
        .get()
  }

  private fun playerCollectionFrom(playerPage: Document) =
      PlayerCollection(playerNameFrom(playerPage), characterRosterFor(playerPage))

  private fun playerNameFrom(playerPage: Document) =
      playerPage.select(".char-name")[1].text()

  private fun characterRosterFor(playerPage: Document): List<CollectedCharacter> {
    return List.ofAll(
        playerPage
            .select(".collection-char:not(.collection-char-missing)")
            .map { rosterEntryFrom(it) }
    )
  }

  private fun rosterEntryFrom(characterDiv: Element): CollectedCharacter {
    return CollectedCharacter(
        characterFrom(characterDiv),
        levelFrom(characterDiv),
        powerFrom(characterDiv),
        gearLevelFrom(characterDiv),
        starsFrom(characterDiv)
    )
  }

  private fun characterFrom(characterDiv: Element) =
      characterRepository.searchByName(characterDiv.select(".collection-char-name-link").text()).get()

  private fun levelFrom(characterDiv: Element) =
      characterDiv.select(".char-portrait-full-level").text().toInt()

  private fun powerFrom(characterDiv: Element): Int =
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

  private fun starsFrom(it: Element) = it.select(".star:not(.star-inactive)").size

}