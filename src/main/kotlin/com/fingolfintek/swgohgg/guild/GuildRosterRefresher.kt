package com.fingolfintek.swgohgg.guild

import io.vavr.collection.Stream
import org.springframework.cache.CacheManager
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
open class GuildRosterRefresher(
    private val cacheManager: CacheManager,
    private val redisTemplate: RedisTemplate<String, String>,
    private val repository: GuildChannelRepository) {

  @PostConstruct
  private fun populateFromRedis() {
    Stream.ofAll(redisTemplate.keys("guilds-*"))
        .forEach {
          repository.assignGuildForChannel(
              channelId = it.removePrefix("guilds-"),
              swgohGgUrl = redisTemplate.boundValueOps(it).get())
        }
  }

  @Scheduled(
      initialDelayString = "\${guild.refresh.delay}",
      fixedDelayString = "\${guild.refresh.delay}")
  private fun refreshRosters() {
    cacheManager.cacheNames
        .forEach { cacheManager.getCache(it).clear() }

    repository.collectionsByChannel()
        .mapValues { it.value.guildUrl }
        .forEach { repository.assignGuildForChannel(it.key, it.value) }
  }
}