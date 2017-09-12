package com.fingolfintek.swgohgg.guild

import com.fingolfintek.swgohgg.player.PlayerCollection
import com.fingolfintek.swgohgg.player.PlayerCollectionRepository
import com.fingolfintek.util.htmlOf
import io.vavr.Tuple
import io.vavr.collection.Map
import io.vavr.collection.Stream
import io.vavr.control.Try
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@Component
open class GuildRepository(
    private val executorService: ExecutorService,
    private val playerCollectionRepository: PlayerCollectionRepository) {

  private val logger = LoggerFactory.getLogger(javaClass)

  @Cacheable(cacheNames = arrayOf("collections"), key = "#swgohGgUrl")
  open fun getForGuildUrl(swgohGgUrl: String): Map<String, PlayerCollection> {
    logger.info("Fetching rosters for $swgohGgUrl")

    val playerUrls = htmlOf(swgohGgUrl)
        .select(".character-list table > tbody > tr > td:nth-child(1) > a")
        .map { "https://swgoh.gg${it.attr("href")}collection" }

    val threadPool = ExecutorCompletionService<PlayerCollection>(executorService)
    playerUrls.forEach { threadPool.submit { playerCollectionRepository.getForPlayerCollectionUrl(it) } }

    return Stream
        .continually { Try.ofSupplier { threadPool.poll(1, TimeUnit.MINUTES) } }
        .take(playerUrls.size)
        .map { it.get().get() }
        .toMap { Tuple.of(it.name, it) }
  }

}
