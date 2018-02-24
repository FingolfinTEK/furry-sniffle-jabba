package com.fingolfintek.swgohgg.unit

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.Test
import kotlin.test.assertTrue

class UnitTest {

  @Test
  fun deserializeCharacters() {
    val json = this::class.java.classLoader
        .getResource("characters.json")
        .readText()

    val list: List<Unit> = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .readValue(json)

    assertTrue { list.size == 142 }
  }

  @Test
  fun deserializeShips() {
    val json = this::class.java.classLoader
        .getResource("ships.json")
        .readText()

    val list: List<Unit> = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .readValue(json)

    assertTrue { list.size == 32 }
  }
}
