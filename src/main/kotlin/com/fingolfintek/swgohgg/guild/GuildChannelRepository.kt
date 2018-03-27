package com.fingolfintek.swgohgg.guild

import com.fingolfintek.swgohgg.player.PlayerCollection
import io.vavr.collection.Stream
import io.vavr.control.Try
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

@Component
open class GuildChannelRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    private val guildRepository: GuildRepository) {

  private val collectionsByChannel: MutableMap<String, GuildRoster> = ConcurrentHashMap()

  open fun assignGuildForChannel(channelId: String, swgohGgUrl: String, callback: () -> Unit = {}) {
    Try.ofSupplier { rosterFor(swgohGgUrl) }
        .onSuccess { redisTemplate.boundValueOps("channel-$channelId").set(swgohGgUrl) }
        .onSuccess { collectionsByChannel[channelId] = it }
        .onSuccess { callback() }
  }

  private fun rosterFor(swgohGgUrl: String) =
      GuildRoster(swgohGgUrl, guildRepository.getForGuildUrl(swgohGgUrl))

  open fun getRosterForChannel(channelId: String): Map<String, PlayerCollection> =
      collectionsByChannel[channelId]?.roster?.toJavaMap() ?: emptyMap()

  open fun collectionsByChannel(): Map<String, GuildRoster> =
      Collections.unmodifiableMap(collectionsByChannel)

  @PostConstruct
  private fun populateFromRedis() {
    Stream.ofAll(redisTemplate.keys("channel-*"))
        .forEach {
          assignGuildForChannel(
              channelId = it.removePrefix("channel-"),
              swgohGgUrl = redisTemplate.boundValueOps(it).get())
        }
  }
}
