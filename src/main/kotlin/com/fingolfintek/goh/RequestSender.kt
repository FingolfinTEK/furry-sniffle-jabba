package com.fingolfintek.goh

import Protos.RequestEnvelopeProto.RequestEnvelope
import Protos.ResponseEnvelopeProto.ContentEncoding.GZIPCONTENTENCODING
import Protos.ResponseEnvelopeProto.ResponseEnvelope
import io.vavr.control.Option
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

@Component
open class RequestSender {

  private val client = OkHttpClient()

  fun <T> execute(envelope: RequestEnvelope, parser: (bytes: ByteArray) -> T): T {

    val httpResponse = client.newCall(Request.Builder()
        .url("https://swprod.capitalgames.com/rpc")
        .post(RequestBody.create(MediaType.parse("application/x-protobuf"), envelope.toByteArray()))
        .addHeader("Content-Type", "application/x-protobuf")
        .addHeader("X-Unity-Version", "5.3.5p8")
        .addHeader("User-Agent", "Dalvik/1.6.0 (Linux; U; Android 4.2.2; GT-P5210 Build/JDQ39E)")
        .build())
        .execute()

    val responseBytes = httpResponse.body()!!.bytes()
    val responseEnvelope = ResponseEnvelope.parseFrom(responseBytes)
    val payloadBytes = responseEnvelope.payload.toByteArray()

    return Option.of(responseEnvelope.contentEncoding)
        .filter { it == GZIPCONTENTENCODING }
        .map { payloadBytes }
        .map { GZIPInputStream(ByteArrayInputStream(it)).use { it.readBytes() } }
        .orElse { Option.of(payloadBytes) }
        .map { parser.invoke(it) }
        .get()
  }

}
