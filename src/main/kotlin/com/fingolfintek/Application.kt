package com.fingolfintek

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fingolfintek.bot.BotProperties
import io.vavr.jackson.datatype.VavrModule
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.support.ExecutorServiceAdapter
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@EnableAsync
@EnableCaching
@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(BotProperties::class)
open class Application {

  @Bean
  @Primary
  open fun objectMapper(): ObjectMapper =
      jacksonObjectMapper()
          .registerModule(JavaTimeModule())
          .registerModule(VavrModule())
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  @Bean
  open fun taskScheduler(): TaskScheduler =
      ThreadPoolTaskScheduler()

  @Bean
  open fun executor(): ExecutorService =
      ExecutorServiceAdapter(taskExecutor())

  @Bean
  open fun taskExecutor(): TaskExecutor {
    val executor = ThreadPoolTaskExecutor()
    executor.corePoolSize = 25
    executor.maxPoolSize = 50
    return executor
  }

  @Bean
  open fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<*, *> {
    val template = RedisTemplate<String, Any>()
    template.connectionFactory = connectionFactory
    template.keySerializer = StringRedisSerializer()
    return template
  }

  @Bean
  open fun redisCacheManager(redisTemplate: RedisTemplate<*, *>): RedisCacheManager {
    val manager = RedisCacheManager(redisTemplate)
    manager.cacheNames = setOf("teams", "guilds")
    manager.setDefaultExpiration(TimeUnit.MINUTES.toSeconds(30))
    manager.setLoadRemoteCachesOnStartup(true)
    manager.setUsePrefix(true)
    return manager
  }

  @Bean
  open fun discordBotContext(properties: BotProperties): JDA =
      JDABuilder(AccountType.BOT)
          .setToken(properties.token)
          .setAutoReconnect(true)
          .buildAsync()
}

fun main(args: Array<String>) {
  SpringApplication.run(Application::class.java, *args)
}

