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
    val name: String,
    val url: String,
    val checkInterval: Int,
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
    val checkInterval: Int
)

fun ResultRow.toMonitorDto() = MonitorDto(
    id = this[Monitors.id].value,
    name = this[Monitors.name],
    url = this[Monitors.url],
    checkInterval = this[Monitors.checkInterval],
    createdAt = this[Monitors.createdAt]
)

fun ResultRow.toMonitorLogDto() = MonitorLogDto(
    id = this[MonitorLogs.id].value,
    monitorId = this[MonitorLogs.monitorId].value,
    status = this[MonitorLogs.status],
    responseTime = this[MonitorLogs.responseTime],
    checkedAt = this[MonitorLogs.checkedAt]
)
