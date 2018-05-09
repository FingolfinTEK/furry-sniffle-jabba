package com.fingolfintek.bot.handler

import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import com.fingolfintek.swgohgg.unit.UnitRepository
import io.vavr.control.Try
import net.dv8tion.jda.core.entities.Message
import org.springframework.stereotype.Component

@Component
open class ZetasHandler(
    private val unitRepository: UnitRepository,
    private val guildChannelRepository: GuildChannelRepository
) : MessageHandler {

  private val messageRegex = Regex(
      "!zetas\\s+(.+)",
      setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
  )

  override fun isApplicableTo(message: Message): Boolean =
      message.content.trim().matches(messageRegex)

  override fun processMessage(message: Message) {
    Try.ofSupplier { messageRegex.matchEntire(message.content)!! }
        .andThenTry { match ->
          val playerName = match.groupValues[1].trim()

          guildChannelRepository
              .getRosterForChannel(message.channel.id)
              .filter { it.name == playerName }
              .map { it.units.filter { it.zetas.isNotEmpty() } }
              .forEach { zetadUnits ->
                zetadUnits
                    .sortBy { it.unit.name }
                    .let {
                      val title = "$playerName (${it.size()} units, ${it.map { it.zetas.size }.sum()} zetas)"
                      val text = it.joinToString("\n\n") {
                        "__${it.unit.name}__\n\t${it.zetas.joinToString("\n\t")}"
                      }
                      message.respondWithEmbed(title, text)
                    }
              }
        }
        .onFailure {
          sendErrorMessageFor(it, message)
        }
  }
}
