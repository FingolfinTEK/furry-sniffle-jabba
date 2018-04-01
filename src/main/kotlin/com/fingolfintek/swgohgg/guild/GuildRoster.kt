package com.fingolfintek.swgohgg.guild

import com.fingolfintek.swgohgg.player.PlayerCollection
import java.io.Serializable

data class GuildRoster(
    val guildUrl: String,
    val roster: List<PlayerCollection>
): Serializable
