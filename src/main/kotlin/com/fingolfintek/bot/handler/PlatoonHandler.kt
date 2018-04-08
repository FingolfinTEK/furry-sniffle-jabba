package com.fingolfintek.bot.handler

import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import com.fingolfintek.swgohgg.unit.Unit
import com.fingolfintek.swgohgg.unit.UnitRepository
import io.vavr.Tuple
import io.vavr.control.Try
import net.dv8tion.jda.core.entities.Message
import org.springframework.stereotype.Component

@Component
open class PlatoonHandler(
    private val unitRepository: UnitRepository,
    private val guildChannelRepository: GuildChannelRepository) : MessageHandler {

  private val messageRegex = Regex(
      "!tb\\s+(ship-)?platoon\\s+([1-7])\\*\\s+(.+)\\s+(\\d+)",
      RegexOption.IGNORE_CASE
  )

  override fun isApplicableTo(message: Message): Boolean =
      message.content.matches(messageRegex)

  override fun processMessage(message: Message) {
    Try.ofSupplier { messageRegex.matchEntire(message.content)!! }
        .andThen { matched ->
          val squadrons = matched.groupValues[1].isNotBlank()
          val combatType = if (squadrons) 2 else 1
          val rarity = matched.groupValues[2].toInt()
          val limit = matched.groupValues[4].toInt()
          val name = matched.groupValues[3]

          unitRepository.searchByName(name)
              .filter { it.combat_type == combatType }
              .map { toListOfMembersHavingUnitAt(message.channel.id, it, rarity, limit) }
              .peek { message.respondWithEmbed("Platoon report", it) }
              .onEmpty { sendNoMatchesMessageFor(message, name, rarity) }
        }
        .onFailure { sendFailureMessageFor(message, it) }
  }

  private fun toListOfMembersHavingUnitAt(
      channelId: String, unit: Unit, rarity: Int, limit: Int): String {

    return guildChannelRepository.getRosterForChannel(channelId)
        .flatMap { playerCollection ->
          playerCollection.units
              .filter { it.unit == unit }
              .map { Tuple.of(playerCollection.name, it) }
        }
        .filter { it._2.rarity >= rarity }
        .sortedBy { it._2.power }
        .map { "${it._1} (${it._2.toPowerString()})" }
        .distinct()
        .take(limit)
        .withIndex()
        .joinToString(
            prefix = "__**${unit.name}**__\n\n",
            separator = "\n",
            transform = { "${it.index + 1}. ${it.value}" }
        )
  }

  private fun sendNoMatchesMessageFor(message: Message, name: String, rarity: Int) {
    message.respondWithEmbed("Platoon report", "No members found that have $name at $rarity*")
  }

  private fun sendFailureMessageFor(message: Message, it: Throwable) {
    message.respondWithEmbed("Platoon report", "Error processing message: ${it.message}")
  }

}
