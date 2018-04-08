package com.fingolfintek.bot.handler

import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import com.fingolfintek.swgohgg.player.PlayerCollection
import com.fingolfintek.teams.OptimalTeamsResolver
import com.fingolfintek.teams.Teams
import io.vavr.control.Option
import io.vavr.control.Try
import net.dv8tion.jda.core.entities.Message
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
open class TeamHandler(
    private val teamDefinitions: Teams,
    private val guildChannelRepository: GuildChannelRepository,
    private val teamsResolver: OptimalTeamsResolver
) : MessageHandler {

  private val logger = LoggerFactory.getLogger(javaClass)

  private val messageRegex = Regex(
      "!team\\s+(.+)\\s*",
      setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
  )

  override fun isApplicableTo(message: Message): Boolean =
      message.content.trim().matches(messageRegex)

  override fun processMessage(message: Message) {
    Try.ofSupplier { messageRegex.matchEntire(message.content)!! }
        .andThenTry { match ->
          val teamName = match.groupValues[1].trim()

          val guildRoster = guildChannelRepository
              .getRosterForChannel(message.channel.id)
              .sortedBy { it.name }

          message.channel.sendTyping().queue()

          Option.of(teamDefinitions.templates.getValue(teamName))
              .peek { processTeamsFor(teamName, guildRoster, message) }
              .onEmpty { message.respondWith("Team $teamName not found") }
        }
        .onFailure {
          sendErrorMessageFor(it, message)
        }
  }

  private fun processTeamsFor(
      teamName: String, rosters: List<PlayerCollection>, message: Message) {

    val playersWithTeam = rosters
        .map { teamsResolver.compatibleTeamsFor(it) }
        .filter { it.teams.any { it.name == teamName } }
        .joinToString("\n") {
          val team = it.teams.find { it.name == teamName }
          "${it.playerName} ${team!!.power() / 1000}k"
        }

    message.respondWithEmbed("Players with $teamName", playersWithTeam)
  }

  private fun sendErrorMessageFor(it: Throwable, message: Message) {
    logger.error("Encountered error", it)
    message.respondWithEmbed("Territory War", "Error processing message: ${it.message}")
  }
}
