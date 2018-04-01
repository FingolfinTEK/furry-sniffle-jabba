package com.fingolfintek.bot.handler

import com.google.common.base.Splitter
import io.vavr.collection.Stream
import io.vavr.control.Option
import io.vavr.control.Try
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageEmbed
import org.slf4j.LoggerFactory
import java.awt.Color

val logger = LoggerFactory.getLogger(MessageHandler::class.java)

interface MessageHandler {

  fun isApplicableTo(message: Message): Boolean

  fun processMessage(message: Message)

  fun handle(message: Message): Try<Message> =
      Option.of(message)
          .filter(this::isApplicableTo)
          .peek(this::processMessage)
          .toTry()

  fun Message.respondWith(response: String) =
      Splitter.fixedLength(2000)
          .trimResults()
          .omitEmptyStrings()
          .splitToList(response)
          .forEach { messagePart ->
            channel.sendMessage(messagePart)
                .queue(
                    { logger.debug("Responded to $content with #${it.id}") },
                    { logger.error("Could not send message $messagePart", it) }
                )
          }

  fun Message.respondWithEmbed(title: String, response: CharSequence) =
      Splitter.fixedLength(2000)
          .trimResults()
          .omitEmptyStrings()
          .splitToList(response)
          .forEach { messagePart ->
            respondWith(EmbedBuilder()
                .setColor(Color.decode("0x2a9690"))
                .setTitle(title)
                .setDescription(messagePart)
                .build())
          }

  fun <T> Message.respondWithEmbed(
      title: String, responseData: Iterable<T>,
      transform: ((T) -> CharSequence) = { it.toString() }) {

    val buffer = StringBuilder()

    Stream.ofAll(responseData)
        .map { transform.invoke(it) }
        .forEach { messagePart ->
          if (buffer.length + messagePart.length > 2000) {
            respondWithEmbed(title, buffer)
            buffer.setLength(0)
          } else buffer.append(messagePart)
        }

    if (buffer.isNotEmpty())
      respondWithEmbed(title, buffer)
  }

  fun Message.respondWith(embed: MessageEmbed) =
      channel.sendMessage(embed)
          .queue(
              { logger.debug("Responded to $content with #${it.id}") },
              { logger.error("Could not send message ${embed.description}", it) }
          )
}
