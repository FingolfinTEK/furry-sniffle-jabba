package com.fingolfintek.bot.handler

import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import com.fingolfintek.swgohgg.player.PlayerCollection
import com.fingolfintek.teams.Teams
import com.fingolfintek.util.createCell
import com.fingolfintek.util.safe
import com.fingolfintek.util.wrappableTextStyle
import io.vavr.Tuple
import io.vavr.Tuple2
import io.vavr.control.Try
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.Message
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Component
import java.awt.Color
import java.io.ByteArrayOutputStream

@Component
open class GuildTeamReportHandler(
    private val teamDefinitions: Teams,
    private val guildChannelRepository: GuildChannelRepository
) : MessageHandler {

  private val messageRegex = Regex(
      "!team-report\\s+(tag\\s+(.+))|(team\\s+(.+))",
      setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
  )

  override fun isApplicableTo(message: Message): Boolean =
      message.content.trim().matches(messageRegex)

  override fun processMessage(message: Message) {
    Try.ofSupplier { messageRegex.matchEntire(message.content)!! }
        .andThenTry { match ->
          message.channel.sendTyping().queue()

          val tag = match.groupValues[2].trim()
          val teamName = match.groupValues[4].trim()

          val teams = teamDefinitions.templates
              .filter {
                if (tag.isNotBlank()) it.value.tags.contains(tag)
                else it.key == teamName
              }

          createReportFor(teams, message)
        }
        .onFailure { sendErrorMessageFor(it, message) }
  }

  private fun createReportFor(
      teams: Map<String, Teams.SquadTemplate>, message: Message) {

    val guildReports = guildChannelRepository
        .getRosterForChannel(message.channel.id)
        .map { roster ->
          Tuple.of(
              roster,
              teams.mapValues { teamEntry ->
                teamEntry.value
                    .stepsToFulfilmentFor(roster.unitsByName())
                    .filterValues { it.isNotEmpty() }
                    .entries
                    .joinToString("\n\n") {
                      "${it.key}\n\t${it.value.joinToString("\n\t")}"
                    }
              }
          )
        }

    val xlsx = createXlsxFor(guildReports)
    sendExcelSpreadsheetMessageFor(message, xlsx)
  }

  private fun createXlsxFor(
      guildReports: List<Tuple2<PlayerCollection, Map<String, String>>>): ByteArrayOutputStream {

    val wb = XSSFWorkbook()
    guildReports.forEach {
      wb.writePlayerData(it)
    }

    val os = ByteArrayOutputStream()
    os.use { wb.write(it) }
    return os
  }

  private fun XSSFWorkbook.writePlayerData(report: Tuple2<PlayerCollection, Map<String, String>>) {
    val sheet = createSheet(report._1.name.safe())

    report._2.map { it }
        .forEachIndexed { index, value -> writeTeamDataTo(sheet, index, value) }

    sheet.autoSizeColumn(1)
    sheet.autoSizeColumn(5)
  }

  private fun writeTeamDataTo(
      sheet: XSSFSheet, index: Int, report: Map.Entry<String, String>) {

    val dataRow = sheet.createRow(1 + index)
    val text = "${report.key}\n${report.value}"
    val cell = dataRow.createCell(1, text)
    dataRow.height = (600 + 310 * text.count { it == '\n' }).toShort()

    if (report.value.isBlank()) {
      val style = sheet.wrappableTextStyle()
      style.setFillPattern(FillPatternType.SOLID_FOREGROUND)
      style.setFillForegroundColor(XSSFColor(Color.decode("0xccffcc")))
      cell.cellStyle = style
    }
  }

  private fun sendExcelSpreadsheetMessageFor(message: Message, os: ByteArrayOutputStream) {
    message.channel.sendFile(
        os.toByteArray().inputStream(),
        "teams_report.xlsx",
        MessageBuilder().append("Here is your guild's team report").build()
    ).queue()
  }
}
