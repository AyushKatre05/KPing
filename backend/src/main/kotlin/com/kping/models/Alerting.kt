package com.kping.models

import com.kping.utils.LocalDateTimeSerializer
import com.kping.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object Incidents : UUIDTable("incidents") {
    val monitorId = reference("monitor_id", Monitors).index()
    val startedAt = datetime("started_at").default(LocalDateTime.now())
    val resolvedAt = datetime("resolved_at").nullable()
    val errorCause = varchar("error_cause", 1024).nullable()
}

object AlertContacts : UUIDTable("alert_contacts") {
    val userId = reference("user_id", Users).index()
    val type = varchar("type", 50) // e.g. "WEBHOOK"
    val destination = varchar("destination", 1024)
}

object MonitorAlerts : Table("monitor_alerts") {
    val monitorId = reference("monitor_id", Monitors)
    val contactId = reference("contact_id", AlertContacts)
    override val primaryKey = PrimaryKey(monitorId, contactId)
}

@Serializable
data class IncidentDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val monitorId: UUID,
    @Serializable(with = LocalDateTimeSerializer::class)
    val startedAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val resolvedAt: LocalDateTime?,
    val errorCause: String?
)

fun ResultRow.toIncidentDto() = IncidentDto(
    id = this[Incidents.id].value,
    monitorId = this[Incidents.monitorId].value,
    startedAt = this[Incidents.startedAt],
    resolvedAt = this[Incidents.resolvedAt],
    errorCause = this[Incidents.errorCause]
)
