package com.fingolfintek.bot.handler

import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import com.fingolfintek.swgohgg.player.PlayerCollection
import com.fingolfintek.teams.OptimalTeamsResolver
import com.fingolfintek.teams.Team
import io.vavr.Tuple2
import io.vavr.collection.Stream
import io.vavr.control.Try
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.Message
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.util.function.Consumer

@Component
open class GuildTwDefenseHandler(
    private val guildChannelRepository: GuildChannelRepository,
    private val teamsResolver: OptimalTeamsResolver
) : MessageHandler {

  private val messageRegex = Regex(
      "!tw\\s+defense\\s*",
      setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
  )

  override fun isApplicableTo(message: Message): Boolean =
      message.content.trim().matches(messageRegex)

  override fun processMessage(message: Message) {
    Try.ofSupplier { messageRegex.matchEntire(message.content)!! }
        .andThen(Consumer {
          message.channel.sendTyping().queue()

          val os = writeDefenseToXlsxFor(message)

          message.channel.sendFile(
              os.toByteArray().inputStream(),
              "tw_defense.xlsx",
              MessageBuilder().append("Here are your guild's TW teams").build()
          ).queue()
        })
        .onFailure {
          message.respondWithEmbed("Territory War", "Error processing message: ${it.message}")
        }
  }

  private fun writeDefenseToXlsxFor(message: Message): ByteArrayOutputStream {
    val guildRoster = guildChannelRepository
        .getRosterForChannel(message.channel.id)
        .toSortedMap()

    val wb = XSSFWorkbook()
    guildRoster.forEach {
      wb.writePlayerData(it)
    }

    val os = ByteArrayOutputStream()
    os.use { wb.write(it) }
    return os
  }

  private fun XSSFWorkbook.writePlayerData(it: Map.Entry<String, PlayerCollection>) {
    val sheet = createSheet(it.key.safe())
    val header = sheet.createRow(1)
    header.createCell(1).setCellValue("Compatible teams")
    header.createCell(5).setCellValue("Optimal teams")

    val compatibleTeams = teamsResolver.compatibleTeamsFor(it.value)
    val optimalTeams = teamsResolver.resolveOptimalTeamsFor(it.value)

    Stream.ofAll(compatibleTeams.teams)
        .zipWithIndex()
        .forEach { writeTeamDataTo(sheet, it, optimalTeams.getOrNull(it._2)) }

    sheet.autoSizeColumn(1)
    sheet.autoSizeColumn(5)
  }

  private fun writeTeamDataTo(
      sheet: XSSFSheet, compatibleTeam: Tuple2<Team, Int>, optimalTeam: Team?) {

    val dataRow = sheet.createRow(2 + compatibleTeam._2)
    dataRow.height = 1800

    val style = sheet.workbook.createCellStyle()
    style.wrapText = true

    val compatible = dataRow.createCell(1)
    compatible.cellStyle = style
    compatible.setCellValue(compatibleTeam._1.toString())

    val optimal = dataRow.createCell(5)
    optimal.setCellValue(optimalTeam?.toString() ?: "")
    optimal.cellStyle = style
  }

  private fun String.safe() = WorkbookUtil.createSafeSheetName(this)

}
