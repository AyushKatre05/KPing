package com.kping.scheduler

import com.kping.models.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import com.kping.scheduler.checkers.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

object MonitoringWorker {
    private val checkers = mapOf(
        "HTTP" to HttpChecker(),
        "PING" to PingChecker(),
        "TCP" to TcpChecker(),
        "DNS" to DnsChecker()
    )

    private val client = HttpClient(CIO) {
        install(HttpTimeout)
        expectSuccess = false
    }
    
    private val lastCheckedMap = ConcurrentHashMap<UUID, Long>()

    suspend fun start() {
        withContext(Dispatchers.IO) {
            while (isActive) {
                val monitors = transaction {
                    Monitors.selectAll().map { it.toMonitorDto() }
                }

                coroutineScope {
                    monitors.forEach { monitor ->
                        launch {
                            val lastCheck = lastCheckedMap[monitor.id] ?: 0L
                            val now = System.currentTimeMillis()
                            
                            if (now - lastCheck >= monitor.checkInterval * 1000L) {
                                lastCheckedMap[monitor.id] = now
                                performCheck(monitor)
                            }
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    private suspend fun performCheck(monitor: MonitorDto) {
        val type = monitor.monitorType.uppercase()
        val checker = checkers[type] ?: checkers["HTTP"]!!
        
        val result = checker.check(monitor)
        
        val underMaintenance = isUnderMaintenance(monitor.id)
        var causeToReport = result.errorCause
        if (underMaintenance && !result.isUp) {
            causeToReport = "Maintenance Window Active"
        }

        transaction {
            MonitorLogs.insert {
                it[monitorId] = monitor.id
                it[status] = if (underMaintenance) 2 else if (result.isUp) 1 else 0
                it[responseTime] = result.responseTimeMs.toInt()
                it[this.sslExpiryDays] = result.sslExpiryDays
            }
        }

        if (!underMaintenance) {
            handleIncidentState(monitor, result.isUp, causeToReport)
        }
    }
        
        val underMaintenance = isUnderMaintenance(monitor.id)
        if (underMaintenance && !isUp) {
            errorCause = "Maintenance Window Active"
        }

        transaction {
            MonitorLogs.insert {
                it[monitorId] = monitor.id
                it[status] = if (underMaintenance) 2 else if (isUp) 1 else 0
                it[responseTime] = timeTaken.toInt()
                it[this.sslExpiryDays] = sslExpiryDays
            }
        }

        if (!underMaintenance) {
            handleIncidentState(monitor, isUp, errorCause)
        }
    }

    private suspend fun handleIncidentState(monitor: MonitorDto, isUp: Boolean, errorCause: String?) {
        val openIncidentRow = transaction {
            Incidents.select { (Incidents.monitorId eq monitor.id) and (Incidents.resolvedAt.isNull()) }.singleOrNull()
        }

        if (!isUp && openIncidentRow == null) {
            transaction {
                Incidents.insert {
                    it[monitorId] = monitor.id
                    it[this.errorCause] = errorCause
                }
            }
            fireWebhooks(monitor, false, errorCause)
            println("[ALERT] Monitor ${monitor.name} (${monitor.url}) is DOWN. Cause: $errorCause")
        } else if (isUp && openIncidentRow != null) {
            transaction {
                Incidents.update({ Incidents.id eq openIncidentRow[Incidents.id] }) {
                    it[resolvedAt] = LocalDateTime.now()
                }
            }
            fireWebhooks(monitor, true, null)
            println("[ALERT] Monitor ${monitor.name} (${monitor.url}) is UP again.")
        }
    }

    private suspend fun fireWebhooks(monitor: MonitorDto, isRecovered: Boolean, cause: String?) {
        val destinations = transaction {
            (MonitorAlerts innerJoin AlertContacts)
                .slice(AlertContacts.destination)
                .select { (MonitorAlerts.monitorId eq monitor.id) and (AlertContacts.type eq "WEBHOOK") }
                .map { it[AlertContacts.destination] }
        }

        val statusText = if (isRecovered) "UP" else "DOWN"
        val payload = """
            {
                "monitor_id": "${monitor.id}",
                "monitor_name": "${monitor.name}",
                "monitor_url": "${monitor.url}",
                "status": "$statusText",
                "cause": "${cause ?: "Recovered"}"
            }
        """.trimIndent()

        destinations.forEach { url ->
            try {
                client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
            } catch (e: Exception) {
                println("Failed to fire webhook to $url")
            }
        }
    }
}
