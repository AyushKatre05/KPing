package com.kping.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import java.util.concurrent.ConcurrentHashMap

class RateLimiterConfig {
    var maxRequests: Int = 100
    var refillIntervalMs: Long = 60000 // 1 minute
}

class RateLimitTokenBucket(private val maxTokens: Int, private val refillIntervalMs: Long) {
    private var tokens = maxTokens
    private var lastRefill = System.currentTimeMillis()

    @Synchronized
    fun tryConsume(): Boolean {
        val now = System.currentTimeMillis()
        val timePassed = now - lastRefill
        val tokensToAdd = (timePassed / refillIntervalMs).toInt() * maxTokens
        if (tokensToAdd > 0) {
            tokens = minOf(maxTokens, tokens + tokensToAdd)
            lastRefill = now
        }

        return if (tokens > 0) {
            tokens--
            true
        } else {
            false
        }
    }
}

val RateLimitingPlugin = createApplicationPlugin("RateLimitingPlugin", ::RateLimiterConfig) {
    val buckets = ConcurrentHashMap<String, RateLimitTokenBucket>()
    val maxRequests = pluginConfig.maxRequests
    val refillIntervalMs = pluginConfig.refillIntervalMs

    onCall { call ->
        val ip = call.request.origin.remoteHost
        val bucket = buckets.getOrPut(ip) { RateLimitTokenBucket(maxRequests, refillIntervalMs) }
        
        if (!bucket.tryConsume()) {
            call.respond(HttpStatusCode.TooManyRequests, "Rate Limit Exceeded. Try again later.")
            return@onCall
        }
    }
}
