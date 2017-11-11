package com.fingolfintek.bot.handler

import com.fingolfintek.swgohgg.unit.Unit
import com.fingolfintek.swgohgg.unit.CharacterRepository
import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import io.vavr.Tuple
import io.vavr.control.Try
import net.dv8tion.jda.core.entities.Message
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component
open class PlatoonHandler(
    private val characterRepository: CharacterRepository,
    private val guildChannelRepository: GuildChannelRepository) : MessageHandler {

  private val messageRegex = Regex(
      "!tb\\s+platoon\\s+p([1-6])\\s+(.+)\\s+(\\d+)",
      RegexOption.IGNORE_CASE
  )

  override fun isApplicableTo(message: Message): Boolean =
      message.content.matches(messageRegex)

  override fun processMessage(message: Message) {
    Try.ofSupplier { messageRegex.matchEntire(message.content)!! }
        .andThen(Consumer {
          val rarity = 1 + it.groupValues[1].toInt()
          val limit = it.groupValues[3].toInt()

          characterRepository.searchByName(it.groupValues[2])
              .map { toPriorityListOfMembersHavingToonAt(message.channel.id, it, rarity, limit) }
              .peek { message.channel.sendMessage(it).queue() }
              .onEmpty {
                message.channel
                    .sendMessage("No members found that have ${characterRepository.searchByName(it.groupValues[2]).get()} at $rarity*")
                    .queue()
              }
        })

  }

  private fun toPriorityListOfMembersHavingToonAt(
      channelId: String, toon: Unit, rarity: Int, limit: Int): String {

    return guildChannelRepository.getRosterForChannel(channelId)
        .flatMap { entry ->
          entry.value.characters
              .filter { it.character == toon }
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
