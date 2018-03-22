package com.fingolfintek.bot.handler

import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import com.fingolfintek.swgohgg.player.PlayerCollection
import com.fingolfintek.teams.OptimalTeamsResolver
import com.fingolfintek.teams.Team
import com.fingolfintek.teams.Teams
import io.vavr.collection.Stream
import io.vavr.control.Try
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.Message
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
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
              .toSortedMap()

          if (playerName.isEmpty()) {
            message.channel.sendTyping().queue()

            val wb = XSSFWorkbook()

            guildRoster.forEach {
              wb.writePlayerData(it)

            }

            val os = ByteArrayOutputStream()
            os.use { wb.write(it) }

            message.channel.sendFile(
                os.toByteArray().inputStream(),
                "tw_defense.xlsx",
                MessageBuilder().append("Here are your guild's TW teams").build()
            ).queue()
          } else {
            processTeamsFor(guildRoster[playerName]!!, message)
          }
        })
        .onFailure { message.respondWithEmbed("Territory War", "Error processing message: ${it.message}") }
  }

  private fun XSSFWorkbook.writePlayerData(it: Map.Entry<String, PlayerCollection>) {
    val sheet = createSheet(it.key.safe())
    val header = sheet.createRow(1)
    header.createCell(1).setCellValue("Compatible teams")
    header.createCell(5).setCellValue("Optimal teams")

    val compatibleTeams = compatibleTeamsFor(it.value)
    val optimalTeams = optimalTeamsResolver.resolveOptimalTeamsFor(compatibleTeams)

    Stream.ofAll(compatibleTeams.teams)
        .zipWithIndex()
        .forEach {
          val data = sheet.createRow(2 + it._2)
          data.createCell(1).setCellValue(it._1.toString())
          data.createCell(5).setCellValue(optimalTeams.getOrNull(it._2)?.toString() ?: "")
          data.height = 1500
        }

    sheet.autoSizeColumn(1)
    sheet.autoSizeColumn(5)
  }

  private fun String.safe() = WorkbookUtil.createSafeSheetName(this)

  private fun compatibleTeamsFor(roster: PlayerCollection) =
      teamDefinitions.tw.defense.compatibleTeamsFor(roster)

  private fun processTeamsFor(roster: PlayerCollection, message: Message) {
    val compatibleTeams = compatibleTeamsFor(roster)
    message.respondWithEmbed(
        "${roster.name}'s compatible teams",
        prettyPrint(compatibleTeams.teams)
    )

    val optimalTeams = optimalTeamsResolver.resolveOptimalTeamsFor(compatibleTeams)
    message.respondWithEmbed(
        "${roster.name}'s optimal teams",
        prettyPrint(optimalTeams)
    )
  }

  private fun prettyPrint(optimalTeams: List<Team>) =
      optimalTeams.joinToString("\n\n") { "$it" }
}
