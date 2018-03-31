package com.fingolfintek.util

import io.vavr.Tuple2
import io.vavr.collection.Map
import io.vavr.collection.Stream

fun <T> Collection<T>.toVavrStream(): Stream<T> = io.vavr.collection.Stream.ofAll(this)

fun <T, K, V> Collection<T>.toVavrMap(f: (T) -> Tuple2<K, V>): Map<K, V> = toVavrStream().toMap(f)
