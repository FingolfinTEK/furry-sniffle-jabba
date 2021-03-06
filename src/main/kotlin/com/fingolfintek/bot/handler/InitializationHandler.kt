package com.fingolfintek.bot.handler

import com.fingolfintek.bot.BotProperties
import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import io.vavr.control.Try
import net.dv8tion.jda.core.entities.Message
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component
open class InitializationHandler(
    private val properties: BotProperties,
    private val guildChannelRepository: GuildChannelRepository) : MessageHandler {

  private val messageRegex = Regex("!init\\s+(http(s)?://.*swgoh.gg.+)")

  override fun isApplicableTo(message: Message): Boolean =
      properties.isAuthorAnOfficer(message) && message.content.matches(messageRegex)

  override fun processMessage(message: Message) {
    Try.ofSupplier { messageRegex.matchEntire(message.content)!! }
        .andThen(Consumer {
          val swgohGgUrl = it.groupValues[1]
          guildChannelRepository.assignGuildForChannel(
              message.channel.id, swgohGgUrl,
              { sendSuccessMessageFor(message, swgohGgUrl) })
        })
        .onFailure { sendFailureMessageFor(message, it) }
  }

  private fun sendSuccessMessageFor(message: Message, swgohGgUrl: String) =
      message.respondWith("Assigned guild $swgohGgUrl to this channel")

  private fun sendFailureMessageFor(message: Message, it: Throwable) {
    message.respondWithEmbed("Initialization", "Error processing message: ${it.message}")
  }

}
