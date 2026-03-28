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

object StatusPages : UUIDTable("status_pages") {
    val name = varchar("name", 255)
    val slug = varchar("slug", 100).uniqueIndex() // e.g. "api-status"
    val description = text("description").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}

object StatusPageMonitors : Table("status_page_monitors") {
    val statusPageId = reference("status_page_id", StatusPages)
    val monitorId = reference("monitor_id", Monitors)
    override val primaryKey = PrimaryKey(statusPageId, monitorId)
}

@Serializable
data class StatusPageDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val name: String,
    val slug: String,
    val description: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val monitors: List<MonitorDto>? = null
)

@Serializable
data class CreateStatusPageRequest(
    val name: String,
    val slug: String,
    val description: String? = null,
    val monitorIds: List<@Serializable(with = UUIDSerializer::class) UUID> = emptyList()
)

fun ResultRow.toStatusPageDto() = StatusPageDto(
    id = this[StatusPages.id].value,
    name = this[StatusPages.name],
    slug = this[StatusPages.slug],
    description = this[StatusPages.description],
    createdAt = this[StatusPages.createdAt]
)
