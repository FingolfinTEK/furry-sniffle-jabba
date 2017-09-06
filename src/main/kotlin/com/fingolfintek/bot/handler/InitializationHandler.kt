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

  private val messageRegex = Regex("!tb\\s+init\\s+(http://.*swgoh.gg.+)")

  override fun isApplicableTo(message: Message): Boolean =
      properties.isAuthorAnOfficer(message) && message.content.matches(messageRegex)

  override fun processMessage(message: Message) {
    Try.ofSupplier { messageRegex.matchEntire(message.content)!! }
        .andThen(Consumer {
          guildChannelRepository.assignGuildForChannel(
              message.channel.id, it.groupValues[1])

          message.channel
              .sendMessage("Assigned guild ${it.groupValues[0]} to this channel")
              .queue()
        })
  }

}