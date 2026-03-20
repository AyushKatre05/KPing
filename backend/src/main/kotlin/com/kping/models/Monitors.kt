package com.kping.models

import com.kping.utils.LocalDateTimeSerializer
import com.kping.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object Monitors : UUIDTable("monitors") {
    val name = varchar("name", 255)
    val url = varchar("url", 1024)
    val checkInterval = integer("check_interval")
    val expectedStatusCode = integer("expected_status_code").default(200)
    val expectedKeyword = varchar("expected_keyword", 255).nullable()
    val timeoutMs = integer("timeout_ms").default(10000)
    val httpMethod = varchar("http_method", 10).default("GET")
    val headers = text("headers").nullable()
    val requestBody = text("request_body").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}

object MonitorLogs : UUIDTable("monitor_logs") {
    val monitorId = reference("monitor_id", Monitors).index()
    val status = integer("status") // 1=UP, 0=DOWN, 2=MAINTENANCE
    val responseTime = integer("response_time")
    val sslExpiryDays = integer("ssl_expiry_days").nullable()
    val checkedAt = datetime("checked_at").default(LocalDateTime.now())
}

object MaintenanceWindows : UUIDTable("maintenance_windows") {
    val monitorId = reference("monitor_id", Monitors).index()
    val dayOfWeek = integer("day_of_week") // 1=Monday...7=Sunday
    val startHour = integer("start_hour")
    val startMinute = integer("start_minute")
    val endHour = integer("end_hour")
    val endMinute = integer("end_minute")
}

@Serializable
data class MonitorDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val name: String,
    val url: String,
    val checkInterval: Int,
    val expectedStatusCode: Int,
    val expectedKeyword: String?,
    val timeoutMs: Int,
    val httpMethod: String,
    val headers: String?,
    val requestBody: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime
)

@Serializable
data class MonitorLogDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val monitorId: UUID,
    val status: Int,
    val responseTime: Int,
    val sslExpiryDays: Int?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val checkedAt: LocalDateTime
)

@Serializable
data class CreateMonitorRequest(
    val name: String,
    val url: String,
    val checkInterval: Int,
    val expectedStatusCode: Int = 200,
    val expectedKeyword: String? = null,
    val timeoutMs: Int = 10000,
    val httpMethod: String = "GET",
    val headers: String? = null,
    val requestBody: String? = null
)

@Serializable
data class MonitorAnalyticsDto(
    val uptimePercentage: Double,
    val averageLatencyMs: Double,
    val p95LatencyMs: Double,
    val p99LatencyMs: Double,
    val apdexScore: Double,
    val totalOutages: Long
)

@Serializable
data class MaintenanceWindowDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val monitorId: UUID,
    val dayOfWeek: Int,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
)

fun ResultRow.toMonitorDto() = MonitorDto(
    id = this[Monitors.id].value,
    name = this[Monitors.name],
    url = this[Monitors.url],
    checkInterval = this[Monitors.checkInterval],
    expectedStatusCode = this[Monitors.expectedStatusCode],
    expectedKeyword = this[Monitors.expectedKeyword],
    timeoutMs = this[Monitors.timeoutMs],
    httpMethod = this[Monitors.httpMethod],
    headers = this[Monitors.headers],
    requestBody = this[Monitors.requestBody],
    createdAt = this[Monitors.createdAt]
)

fun ResultRow.toMonitorLogDto() = MonitorLogDto(
    id = this[MonitorLogs.id].value,
    monitorId = this[MonitorLogs.monitorId].value,
    status = this[MonitorLogs.status],
    responseTime = this[MonitorLogs.responseTime],
    sslExpiryDays = this[MonitorLogs.sslExpiryDays],
    checkedAt = this[MonitorLogs.checkedAt]
)

fun ResultRow.toMaintenanceWindowDto() = MaintenanceWindowDto(
    id = this[MaintenanceWindows.id].value,
    monitorId = this[MaintenanceWindows.monitorId].value,
    dayOfWeek = this[MaintenanceWindows.dayOfWeek],
    startHour = this[MaintenanceWindows.startHour],
    startMinute = this[MaintenanceWindows.startMinute],
    endHour = this[MaintenanceWindows.endHour],
    endMinute = this[MaintenanceWindows.endMinute]
)
