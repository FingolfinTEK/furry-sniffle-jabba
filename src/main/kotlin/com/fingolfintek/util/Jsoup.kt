package com.fingolfintek.util

import org.jsoup.Jsoup

fun htmlOf(url: String) = Jsoup.connect(url).execute().parse()