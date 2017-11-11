package com.fingolfintek.swgohgg.unit

import com.fingolfintek.util.htmlOf
import io.vavr.Tuple
import io.vavr.collection.HashSet
import io.vavr.collection.List
import io.vavr.control.Option
import io.vavr.control.Try
import org.apache.commons.lang3.StringUtils
import org.springframework.cache.annotation.Cacheable

abstract class UnitRepositorySupport {
  protected var units: List<Unit> = List.empty()

  @Cacheable(cacheNames = arrayOf("units"), key = "#name")
  open fun searchByName(name: String): Option<Unit> {
    return Try.ofSupplier { findByExactName(name) }
        .orElse { Try.ofSupplier { findByAbbreviation(name) } }
        .orElse { Try.ofSupplier { findByPartialName(name) } }
        .toOption()
  }

  private fun findByExactName(name: String): Unit =
      units.first { it.name.equals(name, true) }

  private fun findByPartialName(name: String): Unit =
      units.first { it.name.contains(name, true) }

  private fun findByAbbreviation(name: String): Unit =
      units.first {
        it.name.split(Regex("[\\s()]+"))
            .filter { StringUtils.isNotBlank(it) }
            .map { it.trim()[0] }
            .joinToString("")
            .equals(name, true)
      }

  protected fun extractUnitsFrom(url: String): List<Unit> {
    return List.ofAll(
        htmlOf(url)
            .select("a.media-body.character > div.media-heading")
            .map {
              Tuple.of(
                  it.select("h5").text(),

                  it.select("small").text()
                      .split("·")
                      .map { it.trim() }
                      .toSet())
            }
            .map { Unit(it._1, HashSet.ofAll(it._2)) }
    )
  }

}