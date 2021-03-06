package com.fingolfintek.bot

import com.fingolfintek.bot.handler.MessageHandler
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
open class DelegatingEventListener(
    private val jda: JDA,
    private val handlers: List<MessageHandler>) : ListenerAdapter() {

  override fun onMessageReceived(event: MessageReceivedEvent) {
    handlers.forEach { it.handle(event.message) }
  }

  @PostConstruct
  private fun registerWithJDA() {
    jda.addEventListener(this)
  }
}
