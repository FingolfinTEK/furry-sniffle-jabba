package com.fingolfintek.goh

import Protos.AuthGuestResponseProto.AuthGuestResponse
import Protos.GetGuildResponseProto.GetGuildResponse
import Protos.PlayerProfileRequestProto.PlayerProfileRequest
import Protos.PlayerProfileResponseProto.PlayerProfileResponse
import com.google.common.cache.CacheBuilder
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@Component
open class HeroesApi(
    private val requestBuilder: RequestBuilder,
    private val requestSender: RequestSender) {

  private val cachedAuth = CacheBuilder.newBuilder()
      .concurrencyLevel(16)
      .expireAfterWrite(30, TimeUnit.MINUTES)
      .build<String, AuthGuestResponse>()

  private fun authenticate(): AuthGuestResponse =
      requestSender.execute(
          requestBuilder.authRequest(),
          { AuthGuestResponse.parseFrom(it) }
      )


  open fun getGuild(): GetGuildResponse {
    val request = requestBuilder.toRpcRequest(
        "GuildRpc", "GetGuild",
        cachedAuth.get("auth", { authenticate() }))

    return requestSender.execute(request, { GetGuildResponse.parseFrom(it) })
  }

  open fun getPlayer(): PlayerProfileResponse {
    val request = requestBuilder.toRpcRequest(
        "PlayerRpc", "PlayerProfile",
        cachedAuth.get("auth", { authenticate() }),
        PlayerProfileRequest.newBuilder()
            .setAllyCode("924-698-129")
            .build())

    return requestSender.execute(request, { PlayerProfileResponse.parseFrom(it) })
  }

  @PostConstruct
  private fun test() {
    getPlayer()
    getGuild()
  }

}
