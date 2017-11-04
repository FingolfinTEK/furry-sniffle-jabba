package com.fingolfintek.util

import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

fun htmlOf(url: String) = Jsoup
        .connect(url)
        .timeout(TimeUnit.SECONDS.toMillis(30).toInt())
        .execute()
        .parse()