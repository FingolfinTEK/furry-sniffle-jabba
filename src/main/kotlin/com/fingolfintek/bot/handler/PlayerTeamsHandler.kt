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
open class PlayerTeamsHandler(
    private val guildChannelRepository: GuildChannelRepository,
    private val teamsResolver: OptimalTeamsResolver
) : MessageHandler {

  private val logger = LoggerFactory.getLogger(javaClass)

  private val messageRegex = Regex(
      "!teams\\s+(verbose\\s+)?(\\w+)\\s+(.+)",
      setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
  )

  override fun isApplicableTo(message: Message): Boolean =
      message.content.trim().matches(messageRegex)

  override fun processMessage(message: Message) {
    Try.ofSupplier { messageRegex.matchEntire(message.content)!! }
        .peek { message.channel.sendTyping().queue() }
        .andThenTry { match ->
          val guildRoster = guildChannelRepository
              .getRosterForChannel(message.channel.id)
              .sortedBy { it.name }

          val verbose = match.groupValues[1].isNotBlank()
          val tag = match.groupValues[2].trim().toUpperCase()
          val playerName = match.groupValues[3].trim()

          Option.of(guildRoster.find { playerName == it.name })
              .peek { processTeamsFor(it!!, tag, verbose, message) }
              .onEmpty { message.respondWith("Player $playerName not found") }
        }
        .onFailure {
          sendErrorMessageFor(it, message)
        }
  }

  private fun processTeamsFor(
      roster: PlayerCollection, tag: String, verbose: Boolean, message: Message) {
    if (verbose) {
      val compatibleTeams = teamsResolver.compatibleTeamsFor(roster, tag).teams
      message.respondWithEmbed("${roster.name}'s compatible teams", compatibleTeams) { "$it\n\n" }
    }

    val optimalTeams = teamsResolver.resolveOptimalTeamsFor(roster, tag)
    message.respondWithEmbed("${roster.name}'s optimal teams", optimalTeams) { "$it\n\n" }
  }

  private fun sendErrorMessageFor(it: Throwable, message: Message) {
    logger.error("Encountered error", it)
    message.respondWithEmbed("Territory War", "Error processing message: ${it.message}")
  }
}
