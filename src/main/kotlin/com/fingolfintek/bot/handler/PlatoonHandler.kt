package com.fingolfintek.bot.handler

import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import com.fingolfintek.swgohgg.unit.CharacterRepository
import com.fingolfintek.swgohgg.unit.Unit
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
          val name = it.groupValues[2]

          characterRepository.searchByName(name)
              .map { toPriorityListOfMembersHavingToonAt(message.channel.id, it, rarity, limit) }
              .peek { message.respondWith(it) }
              .onEmpty {
                message.respondWith("No members found that have $name at $rarity*")
              }
        })
        .onFailure { message.respondWith("Error processing message: ${it.message}") }
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
        .map { "${it._1} (${it._2.toPowerString()})" }
        .distinct()
        .take(limit)
        .withIndex()
        .joinToString(
            prefix = "__**${toon.name}**__\n\n",
            separator = "\n",
            transform = { "${it.index + 1}. ${it.value}" }
        )
  }

}
