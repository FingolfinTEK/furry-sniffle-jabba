package com.fingolfintek.bot.handler

import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import com.fingolfintek.swgohgg.unit.ShipRepository
import com.fingolfintek.swgohgg.unit.Unit
import io.vavr.Tuple
import io.vavr.control.Try
import net.dv8tion.jda.core.entities.Message
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component
open class ShipPlatoonHandler(
    private val shipRepository: ShipRepository,
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

          shipRepository.searchByName(it.groupValues[2])
              .map { toPriorityListOfMembersHavingToonAt(message.channel.id, it, rarity, limit) }
              .peek { message.channel.sendMessage(it).queue() }
              .onEmpty {
                message.channel
                    .sendMessage("No members found that have ${shipRepository.searchByName(it.groupValues[2]).get()} at $rarity*")
                    .queue()
              }
        })

  }

  private fun toPriorityListOfMembersHavingToonAt(
      channelId: String, toon: Unit, rarity: Int, limit: Int): String {

    return guildChannelRepository.getRosterForChannel(channelId)
        .flatMap { entry ->
          entry.value.ships
              .filter { it.ship == toon }
              .map { Tuple.of(entry.key, it) }
        }
        .filter { it._2.rarity >= rarity }
        .sortedBy { it._2.power }
        .map { "${it._1} (${it._2})" }
        .distinct()
        .take(limit)
        .withIndex()
        .joinToString(separator = "\n", transform = { "${it.index + 1}. ${it.value}" })
  }

}