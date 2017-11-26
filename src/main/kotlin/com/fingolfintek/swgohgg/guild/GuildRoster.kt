package com.fingolfintek.swgohgg.guild

import com.fingolfintek.swgohgg.player.PlayerCollection
import io.vavr.collection.Map
import java.io.Serializable

data class GuildRoster(
    val guildUrl: String,
    val roster: Map<String, PlayerCollection>
): Serializable