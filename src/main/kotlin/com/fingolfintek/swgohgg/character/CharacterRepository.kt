package com.fingolfintek.swgohgg.character

import com.fingolfintek.util.htmlOf
import io.vavr.Tuple
import io.vavr.collection.HashSet
import io.vavr.collection.List
import io.vavr.control.Option
import io.vavr.control.Try
import org.apache.commons.lang3.StringUtils
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
open class CharacterRepository {
  private lateinit var characters: List<Character>

  @Cacheable(cacheNames = arrayOf("characters"), key = "#name")
  open fun searchByName(name: String): Option<Character> {
    return Try.ofSupplier { findByExactName(name) }
        .orElse { Try.ofSupplier { findByAbbreviation(name) } }
        .orElse { Try.ofSupplier { findByPartialName(name) } }
        .toOption()
  }

  private fun findByExactName(name: String): Character =
      characters.first { it.name.equals(name, true) }

  private fun findByPartialName(name: String): Character =
      characters.first { it.name.contains(name, true) }

  private fun findByAbbreviation(name: String): Character =
      characters.first {
        it.name.split(Regex("[\\s()]+"))
            .filter { StringUtils.isNotBlank(it) }
            .map { it.trim()[0] }
            .joinToString("")
            .equals(name, true)
      }

  @PostConstruct
  private fun populate() {
    characters = List.ofAll(
        htmlOf("https://swgoh.gg/")
            .select("a.media-body.character > div.media-heading")
            .map {
              Tuple.of(
                  it.select("h5").text(),

                  it.select("small").text()
                      .split("·")
                      .map { it.trim() }
                      .toSet())
            }
            .map { Character(it._1, HashSet.ofAll(it._2)) }
    )
  }
}