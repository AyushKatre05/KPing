package com.kping

import com.kping.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import com.kping.scheduler.MonitoringWorker
import kotlinx.coroutines.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureSerialization()
    configureRouting()
    configureCors()
    
    // Initialize Database
    DatabaseFactory.init()

    // Start background worker
    launch {
        MonitoringWorker.start()
    }
}
