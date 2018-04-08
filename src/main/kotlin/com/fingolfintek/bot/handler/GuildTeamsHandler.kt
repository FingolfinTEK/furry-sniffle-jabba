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

@Component
open class GuildTeamsHandler(
    private val guildChannelRepository: GuildChannelRepository,
    private val teamsResolver: OptimalTeamsResolver
) : MessageHandler {

  private val messageRegex = Regex(
      "!teams\\s+(\\w+)\\s*",
      setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
  )

  override fun isApplicableTo(message: Message): Boolean =
      message.content.trim().matches(messageRegex)

  override fun processMessage(message: Message) {
    Try.ofSupplier { messageRegex.matchEntire(message.content)!! }
        .andThen { match ->
          message.channel.sendTyping().queue()
          val tag = match.groupValues[1].trim().toUpperCase()
          val os = writeDefenseToXlsxFor(message, tag)
          sendExcelSpreadsheetMessageFor(message, os)
        }
        .onFailure { sendFailureMessageFor(message, it) }
  }

  private fun writeDefenseToXlsxFor(message: Message, tag: String): ByteArrayOutputStream {
    val guildRoster = guildChannelRepository
        .getRosterForChannel(message.channel.id)
        .groupBy { it.name }
        .flatMap { toIndexedNamesWhereDuplicatesExist(it) }
        .sortedBy { it.name }

    val wb = XSSFWorkbook()
    guildRoster.forEach {
      wb.writePlayerData(it, tag)
    }

    val os = ByteArrayOutputStream()
    os.use { wb.write(it) }
    return os
  }

  private fun toIndexedNamesWhereDuplicatesExist(
      it: Map.Entry<String, List<PlayerCollection>>): List<PlayerCollection> {

    return if (it.value.size == 1) it.value
    else it.value
        .mapIndexed { i, collection ->
          collection.copy(name = "${collection.name}$i")
        }
  }

  private fun XSSFWorkbook.writePlayerData(it: PlayerCollection, tag: String) {
    val sheet = createSheet(it.name.safe())
    val header = sheet.createRow(1)
    header.createCell(1).setCellValue("Compatible teams")
    header.createCell(5).setCellValue("Optimal teams")

    val compatibleTeams = teamsResolver.compatibleTeamsFor(it, tag)
    val optimalTeams = teamsResolver.resolveOptimalTeamsFor(it, tag)

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

  private fun sendExcelSpreadsheetMessageFor(message: Message, os: ByteArrayOutputStream) {
    message.channel.sendFile(
        os.toByteArray().inputStream(),
        "tw_defense.xlsx",
        MessageBuilder().append("Here are your guild's TW teams").build()
    ).queue()
  }

  private fun sendFailureMessageFor(message: Message, it: Throwable) {
    message.respondWithEmbed("Territory War", "Error processing message: ${it.message}")
  }

}
