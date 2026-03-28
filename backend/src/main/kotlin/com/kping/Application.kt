package com.kping

import com.kping.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import com.kping.scheduler.MonitoringWorker
import kotlinx.coroutines.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    install(RateLimitingPlugin) {
        maxRequests = 100 // Scale to 100 requests a minute per IP
        refillIntervalMs = 60000
    }
    configureSerialization()
    configureRouting()
    configureCors()
    
    DatabaseFactory.init()

    launch {
        MonitoringWorker.start()
    }
    launch {
        com.kping.scheduler.DataGroomerWorker.start()
    }
}
