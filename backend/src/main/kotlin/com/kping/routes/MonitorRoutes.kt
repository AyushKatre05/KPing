package com.kping.routes

import com.kping.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.time.LocalDateTime
import java.util.UUID

fun Route.monitorRouting() {
    route("/monitors") {
        get {
            val monitors = transaction {
                Monitors.selectAll().map { it.toMonitorDto() }
            }
            call.respond(monitors)
        }

        post {
            val request = call.receive<CreateMonitorRequest>()
            val newMonitorId = transaction {
                Monitors.insertAndGetId {
                    it[name] = request.name
                    it[url] = request.url
                    it[checkInterval] = request.checkInterval
                    it[expectedStatusCode] = request.expectedStatusCode
                    it[expectedKeyword] = request.expectedKeyword
                    it[timeoutMs] = request.timeoutMs
                    it[httpMethod] = request.httpMethod
                    it[headers] = request.headers
                    it[requestBody] = request.requestBody
                }.value
            }
            val monitor = transaction {
                Monitors.select { Monitors.id eq newMonitorId }.single().toMonitorDto()
            }
            call.respond(HttpStatusCode.Created, monitor)
        }

        delete("{id}") {
            val idParam = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val id = UUID.fromString(idParam)
            
            val deleted = transaction {
                Monitors.deleteWhere { Monitors.id eq id } > 0
            }
            if (deleted) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        get("{id}/status") {
            val idParam = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val id = UUID.fromString(idParam)
            
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
            val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0L
            
            val exists = transaction {
                Monitors.select { Monitors.id eq id }.count() > 0
            }
            if (!exists) {
                return@get call.respond(HttpStatusCode.NotFound)
            }

            val logs = transaction {
                MonitorLogs.select { MonitorLogs.monitorId eq id }
                    .orderBy(MonitorLogs.checkedAt to SortOrder.DESC)
                    .limit(limit, offset)
                    .map { it.toMonitorLogDto() }
            }
            call.respond(logs)
        }

        get("{id}/analytics") {
            val idParam = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val id = UUID.fromString(idParam)

            val rangeParam = call.request.queryParameters["range"] ?: "24h"
            val hoursToSubtract = when(rangeParam) {
                "1h" -> 1L
                "7d" -> 168L
                "30d" -> 720L
                else -> 24L // default 24h
            }
            val startTime = LocalDateTime.now().minusHours(hoursToSubtract)
            
            val exists = transaction {
                Monitors.select { Monitors.id eq id }.count() > 0
            }
            if (!exists) {
                return@get call.respond(HttpStatusCode.NotFound)
            }

            val stats = transaction {
                val upCount = MonitorLogs
                    .select { (MonitorLogs.monitorId eq id) and (MonitorLogs.status eq 1) and (MonitorLogs.checkedAt greaterEq startTime) }
                    .count()
                val downCount = MonitorLogs
                    .select { (MonitorLogs.monitorId eq id) and (MonitorLogs.status eq 0) and (MonitorLogs.checkedAt greaterEq startTime) }
                    .count()
                
                val total = upCount + downCount
                val uptimePercentage = if (total > 0) (upCount.toDouble() / total) * 100.0 else 100.0

                var avgLat = 0.0
                if (upCount > 0) {
                    val sumLat = MonitorLogs
                        .slice(MonitorLogs.responseTime.sum())
                        .select { (MonitorLogs.monitorId eq id) and (MonitorLogs.status eq 1) and (MonitorLogs.checkedAt greaterEq startTime) }
                        .singleOrNull()?.getOrNull(MonitorLogs.responseTime.sum()) ?: 0
                    avgLat = sumLat.toDouble() / upCount
                }
                
                val outages = Incidents
                    .select { (Incidents.monitorId eq id) and (Incidents.startedAt greaterEq startTime) }
                    .count()

                var p95 = 0.0
                var p99 = 0.0
                val percentileQuery = """
                    SELECT 
                        percentile_cont(0.95) WITHIN GROUP (ORDER BY response_time) as p95,
                        percentile_cont(0.99) WITHIN GROUP (ORDER BY response_time) as p99
                    FROM monitor_logs 
                    WHERE monitor_id = '${id}' AND status = 1 AND checked_at >= '${startTime}'
                """.trimIndent()
                
                TransactionManager.current().exec(percentileQuery) { rs ->
                    if (rs.next()) {
                        p95 = rs.getDouble("p95")
                        p99 = rs.getDouble("p99")
                    }
                }

                var apdex = 0.0
                val t = 500 // target satisfaction ms
                val apdexQuery = """
                    SELECT 
                        SUM(CASE WHEN status = 1 AND response_time <= ${t} THEN 1 ELSE 0 END) as satisfied,
                        SUM(CASE WHEN status = 1 AND response_time > ${t} AND response_time <= ${t * 4} THEN 1 ELSE 0 END) as tolerating,
                        COUNT(*) as total_checks
                    FROM monitor_logs
                    WHERE monitor_id = '${id}' AND checked_at >= '${startTime}' AND status != 2
                """.trimIndent()

                TransactionManager.current().exec(apdexQuery) { rs ->
                    if (rs.next()) {
                        val satisfied = rs.getInt("satisfied")
                        val tolerating = rs.getInt("tolerating")
                        val totalChecks = rs.getInt("total_checks")
                        if (totalChecks > 0) {
                            apdex = (satisfied.toDouble() + (tolerating.toDouble() / 2.0)) / totalChecks.toDouble()
                        }
                    }
                }

                MonitorAnalyticsDto(
                    uptimePercentage = uptimePercentage,
                    averageLatencyMs = avgLat,
                    p95LatencyMs = p95,
                    p99LatencyMs = p99,
                    apdexScore = apdex,
                    totalOutages = outages
                )
            }
            call.respond(stats)
        }
    }
}
