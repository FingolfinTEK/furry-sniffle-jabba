package com.fingolfintek.bot.handler

import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import com.fingolfintek.swgohgg.player.PlayerCollection
import com.fingolfintek.teams.OptimalTeamsResolver
import com.fingolfintek.teams.Teams
import io.vavr.control.Try
import net.dv8tion.jda.core.entities.Message
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component
open class PlayerTwDefenseHandler(
    private val teamDefinitions: Teams,
    private val guildChannelRepository: GuildChannelRepository,
    private val optimalTeamsResolver: OptimalTeamsResolver
) : MessageHandler {

  private val messageRegex = Regex(
      "!tw\\s+defense\\s*(.+)?",
      RegexOption.IGNORE_CASE
  )

  override fun isApplicableTo(message: Message): Boolean =
      message.content.matches(messageRegex)

  override fun processMessage(message: Message) {
    Try.ofSupplier { messageRegex.matchEntire(message.content)!! }
        .andThen(Consumer {
          val playerName = it.groupValues[1]

          val guildRoster = guildChannelRepository
              .getRosterForChannel(message.channel.id)

          if (playerName.isEmpty()) {
            guildRoster
                .map { }
          } else
            processTeamsFor(guildRoster[playerName]!!, message)
        })
        .onFailure { message.respondWithEmbed("Territory War", "Error processing message: ${it.message}") }
  }

  private fun processTeamsFor(roster: PlayerCollection, message: Message) {
    val compatibleTeams = compatibleTeamsFor(roster)
    message.respondWithEmbed(
        "${roster.name}'s compatible teams",
        compatibleTeams.teams.joinToString("\n\n") { "$it" }
    )

    val optimalTeams = optimalTeamsResolver.resolveOptimalTeamsFor(compatibleTeams)
    message.respondWithEmbed(
        "${roster.name}'s optimal teams",
        optimalTeams.joinToString("\n\n") { "$it" }
    )
  }

  private fun compatibleTeamsFor(roster: PlayerCollection) =
      teamDefinitions.tw.defense.compatibleTeamsFor(roster)

}
