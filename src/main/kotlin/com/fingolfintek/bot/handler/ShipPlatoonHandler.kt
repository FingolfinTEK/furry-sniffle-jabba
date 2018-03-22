package com.fingolfintek.bot.handler

import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import com.fingolfintek.swgohgg.unit.Unit
import com.fingolfintek.swgohgg.unit.UnitRepository
import io.vavr.Tuple
import io.vavr.control.Try
import net.dv8tion.jda.core.entities.Message
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component
open class ShipPlatoonHandler(
    private val unitRepository: UnitRepository,
    private val guildChannelRepository: GuildChannelRepository) : MessageHandler {

  private val messageRegex = Regex(
      "!tb\\s+ship-platoon\\s+p([1-6])\\s+(.+)\\s+(\\d+)",
      RegexOption.IGNORE_CASE
  )

  override fun isApplicableTo(message: Message): Boolean =
      message.content.matches(messageRegex)

  override fun processMessage(message: Message) {
    Try.ofSupplier { messageRegex.matchEntire(message.content)!! }
        .andThen(Consumer {
          val rarity = 1 + it.groupValues[1].toInt()
          val limit = it.groupValues[3].toInt()
          val name = it.groupValues[2]

          unitRepository.searchByName(name)
              .map { toPriorityListOfMembersHavingShipAt(message.channel.id, it, rarity, limit) }
              .peek { message.respondWithEmbed("Squadron report", it) }
              .onEmpty {
                message.respondWithEmbed("Squadron report", "No members found that have $name at $rarity*")
              }
        })

  }

  private fun toPriorityListOfMembersHavingShipAt(
      channelId: String, ship: Unit, rarity: Int, limit: Int): String {

    return guildChannelRepository.getRosterForChannel(channelId)
        .flatMap { entry ->
          entry.value.units
              .filter { it.unit == ship }
              .map { Tuple.of(entry.key, it) }
        }
        .filter { it._2.unit.combat_type == 2 }
        .filter { it._2.rarity >= rarity }
        .sortedBy { it._2.power }
        .map { "${it._1} (${it._2.toPowerString()})" }
        .distinct()
        .take(limit)
        .withIndex()
        .joinToString(
            prefix = "__**${ship.name}**__\n\n",
            separator = "\n",
            transform = { "${it.index + 1}. ${it.value}" })
  }

}
