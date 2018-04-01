package com.fingolfintek.bot.handler

import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import com.fingolfintek.swgohgg.player.PlayerCollection
import com.fingolfintek.teams.OptimalTeamsResolver
import io.vavr.control.Option
import io.vavr.control.Try
import net.dv8tion.jda.core.entities.Message
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
open class PlayerTwDefenseHandler(
    private val guildChannelRepository: GuildChannelRepository,
    private val teamsResolver: OptimalTeamsResolver
) : MessageHandler {

  private val logger = LoggerFactory.getLogger(javaClass)

  private val messageRegex = Regex(
      "!tw\\s+(verbose\\s+)?defense\\s+(.+)",
      setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
  )

  override fun isApplicableTo(message: Message): Boolean =
      message.content.trim().matches(messageRegex)

  override fun processMessage(message: Message) {
    Try.ofSupplier { messageRegex.matchEntire(message.content)!! }
        .andThenTry { match ->
          val playerName = match.groupValues[2].trim()

          val guildRoster = guildChannelRepository
              .getRosterForChannel(message.channel.id)
              .sortedBy { it.name }

          message.channel.sendTyping().queue()

          Option.of(guildRoster.find { playerName == it.name })
              .peek { processTeamsFor(it!!, match.groupValues[1].isNotBlank(), message) }
              .onEmpty { message.respondWith("Player $playerName not found") }
        }
        .onFailure {
          sendErrorMessageFor(it, message)
        }
  }

  private fun processTeamsFor(roster: PlayerCollection, verbose: Boolean, message: Message) {
    if (verbose) {
      val compatibleTeams = teamsResolver.compatibleTeamsFor(roster).teams
      message.respondWithEmbed("${roster.name}'s compatible teams", compatibleTeams) { "$it\n\n" }
    }

    val optimalTeams = teamsResolver.resolveOptimalTeamsFor(roster)
    message.respondWithEmbed("${roster.name}'s optimal teams", optimalTeams) { "$it\n\n" }
  }

  private fun sendErrorMessageFor(it: Throwable, message: Message) {
    logger.error("Encountered error", it)
    message.respondWithEmbed("Territory War", "Error processing message: ${it.message}")
  }
}
