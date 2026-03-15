package com.kping.scheduler

import com.kping.models.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

object MonitoringWorker {
    private val logger = LoggerFactory.getLogger(MonitoringWorker::class.java)
    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 10_000
        }
    }

    private val nextCheckTimes = mutableMapOf<UUID, Long>()

    suspend fun start() = coroutineScope {
        logger.info("Starting MonitoringWorker loop")
        while (isActive) {
            try {
                val monitors = transaction {
                    Monitors.selectAll().map { it.toMonitorDto() }
                }

                val now = System.currentTimeMillis()

                monitors.forEach { monitor ->
                    val nextCheck = nextCheckTimes[monitor.id] ?: 0L
                    if (now >= nextCheck) {
                        launch {
                            checkMonitor(monitor)
                        }
                        // Update next check time
                        nextCheckTimes[monitor.id] = now + (monitor.checkInterval * 1000L)
                    }
                }

                // Clean up deleted monitors from map
                val activeIds = monitors.map { it.id }.toSet()
                nextCheckTimes.keys.retainAll(activeIds)

                delay(1000) // tick every 1 second
            } catch (e: Exception) {
                logger.error("Error in MonitoringWorker loop", e)
                delay(5000)
            }
        }
    }

    private suspend fun checkMonitor(monitor: MonitorDto) {
        val start = System.currentTimeMillis()
        var statusCode = 500
        try {
            val response = client.get(monitor.url)
            statusCode = response.status.value
        } catch (e: Exception) {
            logger.debug("Failed to check ${monitor.url}: ${e.message}")
        }
        val duration = (System.currentTimeMillis() - start).toInt()

        transaction {
            MonitorLogs.insert {
                it[monitorId] = monitor.id
                it[status] = statusCode
                it[responseTime] = duration
                it[checkedAt] = LocalDateTime.now()
            }
        }
        logger.info("Checked monitor: ${monitor.name} [Status: $statusCode, Time: ${duration}ms]")
    }
}
