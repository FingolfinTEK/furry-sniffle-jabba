package com.fingolfintek.util

import net.jodah.failsafe.Failsafe
import net.jodah.failsafe.RetryPolicy
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

val htmlPolicy = RetryPolicy()
    .withMaxRetries(3)
    .withJitter(0.3)
    .withBackoff(5, 30, TimeUnit.SECONDS, 3.0)

fun htmlOf(url: String): Document =
    Failsafe.with<Document>(htmlPolicy)
        .get(Callable {
          Jsoup.connect(url)
              .timeout(TimeUnit.SECONDS.toMillis(30).toInt())
              .execute()
              .parse()
        })