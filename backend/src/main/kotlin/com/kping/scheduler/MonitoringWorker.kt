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
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.security.cert.X509Certificate

object MonitoringWorker {
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

    private fun getSslExpiryDays(urlStr: String): Int? {
        if (!urlStr.startsWith("https://")) return null
        try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpsURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.connect()
            val certs = conn.serverCertificates
            if (certs.isNotEmpty()) {
                val cert = certs[0] as X509Certificate
                val expiryDate = cert.notAfter
                val diff = expiryDate.time - System.currentTimeMillis()
                return (diff / (1000L * 60 * 60 * 24)).toInt()
            }
        } catch (e: Exception) {
            // Unresponsive or invalid SSL
        }
        return null
    }

    private fun isUnderMaintenance(monitorId: UUID): Boolean {
        return transaction {
            val now = LocalDateTime.now()
            val day = now.dayOfWeek.value
            val hour = now.hour
            val minute = now.minute

            val activeWindows = MaintenanceWindows.select {
                (MaintenanceWindows.monitorId eq monitorId) and
                (MaintenanceWindows.dayOfWeek eq day)
            }.map { it.toMaintenanceWindowDto() }

            activeWindows.any { w ->
                val currentTime = hour * 60 + minute
                val startTime = w.startHour * 60 + w.startMinute
                val endTime = w.endHour * 60 + w.endMinute
                currentTime in startTime..endTime
            }
        }
    }

    private suspend fun performCheck(monitor: MonitorDto) {
        var statusCode = 0
        var isUp = false
        var errorCause: String? = null
        var bodyString = ""
        var sslExpiryDays: Int? = null

        withContext(Dispatchers.IO) {
            sslExpiryDays = getSslExpiryDays(monitor.url)
        }

        val timeTaken = try {
            val start = System.currentTimeMillis()
            val response = client.request(monitor.url) {
                method = HttpMethod.parse(monitor.httpMethod)
                timeout {
                    requestTimeoutMillis = monitor.timeoutMs.toLong()
                }
                if (!monitor.headers.isNullOrBlank()) {
                    try {
                        val headersMap = Json.decodeFromString<Map<String, String>>(monitor.headers)
                        headersMap.forEach { (k, v) -> header(k, v) }
                    } catch (e: Exception) {
                        println("Failed to parse headers for monitor ${monitor.id}: ${e.message}")
                    }
                }
                if (!monitor.requestBody.isNullOrBlank()) {
                    setBody(monitor.requestBody)
                }
            }
            statusCode = response.status.value
            bodyString = response.bodyAsText()
            val elapsed = System.currentTimeMillis() - start
            elapsed
        } catch (e: Exception) {
            errorCause = e.localizedMessage ?: "Connection Failed"
            System.currentTimeMillis()
            0L
        }

        // Configuration validations
        if (errorCause == null) {
            if (statusCode != monitor.expectedStatusCode) {
                errorCause = "Expected HTTP ${monitor.expectedStatusCode}, got $statusCode"
            } else if (!monitor.expectedKeyword.isNullOrBlank() && !bodyString.contains(monitor.expectedKeyword)) {
                errorCause = "Keyword '${monitor.expectedKeyword}' not found in response body"
            } else if (sslExpiryDays != null && sslExpiryDays!! < 7) {
                errorCause = "SSL Certificate expiring in $sslExpiryDays days"
                isUp = false // Enforce downtime penalty for decaying certs
            } else {
                isUp = true
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
