package com.fingolfintek.swgohgg.guild

import com.fingolfintek.swgohgg.player.PlayerCollection
import io.vavr.collection.Stream
import io.vavr.control.Try
import org.springframework.cache.CacheManager
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

const val REFRESH_DELAY_MILLIS = 3600000L

@Component
open class GuildChannelRepository(
    private val cacheManager: CacheManager,
    private val redisTemplate: RedisTemplate<String, String>,
    private val guildRepository: GuildRepository) {

  private val collectionsByChannel: MutableMap<String, GuildRoster> = ConcurrentHashMap()

  open fun assignGuildForChannel(channelId: String, swgohGgUrl: String) =
      Try.ofSupplier { rosterFor(swgohGgUrl) }
          .onSuccess { redisTemplate.boundValueOps("guilds-$channelId").set(swgohGgUrl) }
          .onSuccess { collectionsByChannel.put(channelId, it) }
          .get()!!

  private fun rosterFor(swgohGgUrl: String) =
      GuildRoster(swgohGgUrl, guildRepository.getForGuildUrl(swgohGgUrl))

  open fun getRosterForChannel(channelId: String): Map<String, PlayerCollection> {
    return collectionsByChannel[channelId]?.roster?.toJavaMap() ?: emptyMap()
  }

  @PostConstruct
  private fun populateFromRedis() {
    Stream.ofAll(redisTemplate.keys("guilds-*"))
        .forEach {
          assignGuildForChannel(
              channelId = it.removePrefix("guilds-"),
              swgohGgUrl = redisTemplate.boundValueOps(it).get())
        }
  }

  @Scheduled(
      initialDelay = REFRESH_DELAY_MILLIS,
      fixedDelay = REFRESH_DELAY_MILLIS)
  private fun refreshRosters() {
    cacheManager.cacheNames
        .forEach { cacheManager.getCache(it).clear() }

    collectionsByChannel
        .mapValues { it.value.guildUrl }
        .forEach { assignGuildForChannel(it.key, it.value) }
  }
}
