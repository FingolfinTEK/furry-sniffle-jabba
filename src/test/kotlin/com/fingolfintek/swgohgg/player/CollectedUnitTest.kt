package com.fingolfintek.swgohgg.player

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.Test
import kotlin.test.assertTrue

class CollectedUnitTest {

  @Test
  fun deserialize() {
    val json = this::class.java.classLoader
        .getResource("units.json")
        .readText()

    val map: Map<String, List<CollectedUnit>> = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
        .readValue(json)

    assertTrue { map.size == 174 }
  }
}
