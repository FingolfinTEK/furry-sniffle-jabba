package com.fingolfintek.goh

import Protos.AuthGuestRequestProto.AuthGuestRequest
import Protos.AuthGuestResponseProto.AuthGuestResponse
import Protos.RequestEnvelopeProto
import Protos.RequestEnvelopeProto.RequestEnvelope
import com.google.protobuf.Message
import io.vavr.control.Option
import org.springframework.stereotype.Component
import java.util.*

@Component
open class RequestBuilder {

  private val guestAuth = AuthGuestRequest.newBuilder()
      .setUid("9d3d228b13795a0cbc68a2510b722060")
      .setDevicePlatform("Android")
      .setLanguage("en")
      .setPlayerName("")
      .setBundleId("com.ea.game.starwarscapital_row")
      .setRegion("NA")
      .setLocalTimeZoneOffsetMinutes(480)
      .build()

  fun authRequest(): RequestEnvelope {
    return toRpcRequest("AuthRpc", "DoAuthGuest", payload = guestAuth)
  }

  fun toRpcRequest(
      serviceName: String, methodName: String,
      auth: AuthGuestResponse? = null,
      payload: Message? = null): RequestEnvelope {

    val builder = RequestEnvelope
        .newBuilder()
        .setCorrelationId(0)
        .setServiceName(serviceName)
        .setMethodName(methodName)
        .setClientVersion(309129)
        .setClientStartupTimestamp(System.currentTimeMillis() / 1000)
        .setPlatform("Android")
        .setRegion("NA")
        .setClientExternalVersion("0.11.1")
        .setClientInternalVersion("0.11.309129")
        .setRequestId(UUID.randomUUID().toString())
        .setAcceptEncoding(RequestEnvelopeProto.AcceptEncoding.GZIPACCEPTENCODING)
        .setCurrentClientTime(System.currentTimeMillis() / 1000)
        .setNimbleSessionId("201701141659074633725979")
        .setTimezone("CST")
        .setCarrier("46000")
        .setNetworkAccess("W")
        .setHardwareId("14480")
        .setAndroidId("9001048633645127")
        .setSynergyId("10552419550")
        .setDeviceModel("samsung GT-P5210")
        .setDeviceId("9d29641dc261454239456122f13de042b3a0cc3f45d4c27e7ddc97b300eb57ae")

    Option.of(payload)
        .peek { builder.payload = it?.toByteString() }

    Option.of(auth)
        .peek {
          builder.authId = it?.authId
          builder.authToken = it?.authToken
        }

    return builder.build()
  }

}
