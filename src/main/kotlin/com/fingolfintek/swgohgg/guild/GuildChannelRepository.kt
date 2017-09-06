package com.fingolfintek.swgohgg.guild

import com.fingolfintek.swgohgg.player.PlayerCollection
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
open class GuildChannelRepository(
    private val guildRepository: GuildRepository) {

  private val collectionsByChannel: MutableMap<String, GuildRoster> = ConcurrentHashMap()

  open fun assignGuildForChannel(channelId: String, swgohGgUrl: String) =
      collectionsByChannel.put(channelId, rosterFor(swgohGgUrl))

  private fun rosterFor(swgohGgUrl: String) = GuildRoster(swgohGgUrl, guildRepository.getForGuildUrl(swgohGgUrl))

  open fun getRosterForChannel(channelId: String): Map<String, PlayerCollection> {
    return collectionsByChannel[channelId]?.roster?.toJavaMap() ?: emptyMap()
  }
}