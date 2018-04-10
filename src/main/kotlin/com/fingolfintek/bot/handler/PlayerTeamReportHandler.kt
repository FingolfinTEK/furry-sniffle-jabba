package com.fingolfintek.bot.handler

import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import com.fingolfintek.swgohgg.player.PlayerCollection
import com.fingolfintek.teams.Teams
import io.vavr.control.Option
import io.vavr.control.Try
import net.dv8tion.jda.core.entities.Message
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
open class PlayerTeamReportHandler(
    private val teamDefinitions: Teams,
    private val guildChannelRepository: GuildChannelRepository
) : MessageHandler {

  private val logger = LoggerFactory.getLogger(javaClass)

  private val messageRegex = Regex(
      "!team-report\\s+player\\s+(.+)\\s+team\\s+(.+)",
      setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
  )

  override fun isApplicableTo(message: Message): Boolean =
      message.content.trim().matches(messageRegex)

  override fun processMessage(message: Message) {
    Try.ofSupplier { messageRegex.matchEntire(message.content)!! }
        .andThenTry { match ->
          message.channel.sendTyping().queue()

          val playerName = match.groupValues[1].trim()
          val teamName = match.groupValues[2].trim()

          val playerRoster = guildChannelRepository
              .getRosterForChannel(message.channel.id)
              .find { it.name == playerName }
              .let { Option.of(it!!) }
              .onEmpty { message.respondWith("No $") }

          Option.of(teamDefinitions.templates.getValue(teamName))
              .onEmpty { message.respondWith("Team $teamName not found") }
              .peek { team ->
                playerRoster.peek { processTeamsFor(team, it, message) }
              }
        }
        .onFailure { sendErrorMessageFor(it, message) }
  }

  private fun processTeamsFor(
      team: Teams.SquadTemplate, playerRoster: PlayerCollection, message: Message) {

    val teamReport = team
        .stepsToFulfilmentFor(playerRoster.unitsByName())
        .filterValues { it.isNotEmpty() }
        .entries
        .joinToString("\n\n") {
          "${it.key}\n\t${it.value.joinToString("\n\t")}"
        }

    message.respondWithEmbed("${playerRoster.name}'s report", teamReport)
  }

}
