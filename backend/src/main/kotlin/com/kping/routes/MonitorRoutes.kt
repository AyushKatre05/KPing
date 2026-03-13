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
            
            // Check if monitor exists
            val exists = transaction {
                Monitors.select { Monitors.id eq id }.count() > 0
            }
            if (!exists) {
                return@get call.respond(HttpStatusCode.NotFound)
            }

            val logs = transaction {
                MonitorLogs.select { MonitorLogs.monitorId eq id }
                    .orderBy(MonitorLogs.checkedAt to SortOrder.DESC)
                    .limit(100)
                    .map { it.toMonitorLogDto() }
            }
            call.respond(logs)
        }
    }
}
