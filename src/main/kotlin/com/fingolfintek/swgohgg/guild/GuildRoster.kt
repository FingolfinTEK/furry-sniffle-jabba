package com.fingolfintek.swgohgg.guild

import com.fingolfintek.swgohgg.player.PlayerCollection
import io.vavr.collection.Map

data class GuildRoster(
    val guildUrl: String,
    val roster: Map<String, PlayerCollection>
)