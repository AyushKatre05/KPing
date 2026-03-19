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
    val userId = reference("user_id", Users).index()
    val name = varchar("name", 255)
    val url = varchar("url", 1024)
    val checkInterval = integer("check_interval")
    val expectedStatusCode = integer("expected_status_code").default(200)
    val expectedKeyword = varchar("expected_keyword", 255).nullable()
    val timeoutMs = integer("timeout_ms").default(10000)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}

object MonitorLogs : UUIDTable("monitor_logs") {
    val monitorId = reference("monitor_id", Monitors).index()
    val status = integer("status")
    val responseTime = integer("response_time")
    val checkedAt = datetime("checked_at").default(LocalDateTime.now())
}

@Serializable
data class MonitorDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val userId: UUID,
    val name: String,
    val url: String,
    val checkInterval: Int,
    val expectedStatusCode: Int,
    val expectedKeyword: String?,
    val timeoutMs: Int,
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
    val timeoutMs: Int = 10000
)

@Serializable
data class MonitorAnalyticsDto(
    val uptimePercentage: Double,
    val averageLatencyMs: Double,
    val totalOutages: Long
)

fun ResultRow.toMonitorDto() = MonitorDto(
    id = this[Monitors.id].value,
    userId = this[Monitors.userId].value,
    name = this[Monitors.name],
    url = this[Monitors.url],
    checkInterval = this[Monitors.checkInterval],
    expectedStatusCode = this[Monitors.expectedStatusCode],
    expectedKeyword = this[Monitors.expectedKeyword],
    timeoutMs = this[Monitors.timeoutMs],
    createdAt = this[Monitors.createdAt]
)

fun ResultRow.toMonitorLogDto() = MonitorLogDto(
    id = this[MonitorLogs.id].value,
    monitorId = this[MonitorLogs.monitorId].value,
    status = this[MonitorLogs.status],
    responseTime = this[MonitorLogs.responseTime],
    checkedAt = this[MonitorLogs.checkedAt]
)
