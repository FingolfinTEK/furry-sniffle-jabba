package com.fingolfintek.bot.handler

import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import com.fingolfintek.swgohgg.unit.UnitRepository
import io.vavr.control.Try
import net.dv8tion.jda.core.entities.Message
import org.springframework.stereotype.Component

@Component
open class ZetaHandler(
    private val unitRepository: UnitRepository,
    private val guildChannelRepository: GuildChannelRepository
) : MessageHandler {

  private val messageRegex = Regex(
      "!zeta\\s+(.+)",
      setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
  )

  override fun isApplicableTo(message: Message): Boolean =
      message.content.trim().matches(messageRegex)

  override fun processMessage(message: Message) {
    Try.ofSupplier { messageRegex.matchEntire(message.content)!! }
        .andThenTry { match ->
          val unitOrZeta = match.groupValues[1].trim()
          val guildRoster = guildChannelRepository
              .getRosterForChannel(message.channel.id)

          unitRepository.searchByName(unitOrZeta)
              .map { unit ->
                guildRoster.filter { it.units.any { it.unit == unit && it.zetas.isNotEmpty() } }
              }
              .getOrElse { guildRoster.filter { it.units.any { it.zetas.contains(unitOrZeta) } } }
              .sortedBy { it.name }
              .withIndex()
              .joinToString(
                  prefix = "__**$unitOrZeta**__\n\n",
                  separator = "\n",
                  transform = { "${it.index + 1}. ${it.value.name}" })
              .let { message.respondWithEmbed("Players with $unitOrZeta zeta", it) }
        }
        .onFailure {
          sendErrorMessageFor(it, message)
        }
  }
}
