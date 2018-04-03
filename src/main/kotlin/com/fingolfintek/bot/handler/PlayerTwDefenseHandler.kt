package com.fingolfintek.bot.handler

import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import com.fingolfintek.swgohgg.player.PlayerCollection
import com.fingolfintek.teams.OptimalTeamsResolver
import com.fingolfintek.teams.Team
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
      "!tw\\s+(verbose\\s+)?defense\\s*(.+)",
      setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
  )

  override fun isApplicableTo(message: Message): Boolean =
      message.content.trim().matches(messageRegex)

  override fun processMessage(message: Message) {
    Try.ofSupplier { messageRegex.matchEntire(message.content)!! }
        .andThen { match ->
          val playerName = match.groupValues[2].trim()

          val guildRoster = guildChannelRepository
              .getRosterForChannel(message.channel.id)
              .toSortedMap()

          message.channel.sendTyping().queue()

          Option.of(guildRoster[playerName])
              .peek { processTeamsFor(it!!, match.groupValues[1].isNotBlank(), message) }
              .onEmpty { message.respondWith("Player $playerName not found") }
        }
        .onFailure {
          logger.error("Encountered error", it)
          message.respondWithEmbed("Territory War", "Error processing message: ${it.message}")
        }
  }

  private fun processTeamsFor(roster: PlayerCollection, verbose: Boolean, message: Message) {
    val optimalTeams = teamsResolver.resolveOptimalTeamsFor(roster)

    if (verbose) {
      message.respondWithEmbed(
          "${roster.name}'s compatible teams",
          prettyPrint(teamsResolver.compatibleTeamsFor(roster).teams))
    }

    message.respondWithEmbed(
        "${roster.name}'s optimal teams",
        prettyPrint(optimalTeams))
  }

  private fun prettyPrint(optimalTeams: List<Team>) =
      optimalTeams.joinToString("\n\n") { "$it" }
}
