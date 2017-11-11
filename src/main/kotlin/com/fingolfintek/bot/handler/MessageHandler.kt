package com.fingolfintek.bot.handler

import com.google.common.base.Splitter
import io.vavr.control.Option
import io.vavr.control.Try
import net.dv8tion.jda.core.entities.Message
import org.slf4j.LoggerFactory

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


}