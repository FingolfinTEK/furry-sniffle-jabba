package com.fingolfintek.swgohgg.guild

import com.fingolfintek.swgohgg.player.PlayerCollection
import io.vavr.control.Try
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
open class GuildChannelRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    private val guildRepository: GuildRepository) {

  private val collectionsByChannel: MutableMap<String, GuildRoster> = ConcurrentHashMap()

  @Async
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

  open fun collectionsByChannel(): Map<String, GuildRoster> {
    return Collections.unmodifiableMap(collectionsByChannel)
  }
}
