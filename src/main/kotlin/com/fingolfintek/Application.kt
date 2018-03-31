package com.fingolfintek

import Protos.AuthGuestRequestProto.AuthGuestRequest
import Protos.AuthGuestResponseProto.AuthGuestResponse
import Protos.RequestEnvelopeProto.AcceptEncoding
import Protos.RequestEnvelopeProto.RequestEnvelope
import Protos.ResponseEnvelopeProto.ResponseEnvelope
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fingolfintek.bot.BotProperties
import com.google.common.collect.ImmutableMap
import io.vavr.jackson.datatype.VavrModule
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
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
import java.io.ByteArrayInputStream
import java.util.*
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
  val request = AuthGuestRequest
      .newBuilder()
      .setUid("9d3d228b13795a0cbc68a2510b722060")
      .setDevicePlatform("Android")
      .setLanguage("en")
      .setPlayerName("")
      .setBundleId("com.ea.game.starwarscapital_row")
      .setRegion("NA")
      .setLocalTimeZoneOffsetMinutes(480)
      .build()

  val serviceName = "AuthRpc"
  val methodName = "DoAuthGuest"
  val payload = request.toByteString()
  val clientStartupTime = (System.currentTimeMillis() / 1000) - 10

  val envelope = RequestEnvelope.newBuilder()
      .setCorrelationId(0)
      .setServiceName(serviceName)
      .setMethodName(methodName)
      .setPayload(payload)
      .setClientVersion(309129)
      .setClientStartupTimestamp(clientStartupTime)
      .setPlatform("Android")
      .setRegion("NA")
      .setClientExternalVersion("0.11.3")
      .setClientInternalVersion("0.11.309129")
      .setRequestId(UUID.randomUUID().toString())
      .setAcceptEncoding(AcceptEncoding.GZIPACCEPTENCODING)
      .setCurrentClientTime(clientStartupTime + 8)
      .setNimbleSessionId("2018031311659074633725979")
      .setTimezone("CST")
      .setCarrier("46000")
      .setNetworkAccess("W")
      .setHardwareId("14480")
      .setAndroidId("9001048633645127")
      .setSynergyId("10552419550")
      .setDeviceModel("samsung GT-P5210")
      .setDeviceId("9d29641dc261454239456122f13de042b3a0cc3f45d4c27e7ddc97b300eb57ae")
      .build()
      .toByteArray()

  val httpResponse = khttp.post(
      url = "https://swprod.capitalgames.com/rpc",
      headers = ImmutableMap.of(
          "Content-Type", "application/x-protobuf",
          "X-Unity-Version", "5.3.5p8",
          "User-Agent", "Dalvik/1.6.0 (Linux; U; Android 4.2.2; GT-P5210 Build/JDQ39E,",
          "Connection", "Keep-Alive",
          "Accept-Encoding", "gzip"),
      data = ByteArrayInputStream(envelope)
  )

  println("${httpResponse.statusCode}: ${httpResponse.headers}\n${httpResponse.text}")

  val responsePayload = ResponseEnvelope.parseFrom(httpResponse.content).payload
  val response = AuthGuestResponse.parseFrom(responsePayload)

  println(response.toString())
//  SpringApplication.run(Application::class.java, *args)
}
