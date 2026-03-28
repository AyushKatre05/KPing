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

fun Route.statusPageRouting() {
    route("/status-pages") {
        get {
            val pages = transaction {
                StatusPages.selectAll().map { it.toStatusPageDto() }
            }
            call.respond(pages)
        }

        get("{slug}") {
            val slugParam = call.parameters["slug"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            
            val page = transaction {
                val row = StatusPages.select { StatusPages.slug eq slugParam }.singleOrNull()
                if (row == null) return@transaction null
                
                val pageDto = row.toStatusPageDto()
                
                // Fetch monitors attached to this page via junction proxy
                val monitors = (StatusPageMonitors innerJoin Monitors)
                    .select { StatusPageMonitors.statusPageId eq pageDto.id }
                    .map { it.toMonitorDto() }
                
                pageDto.copy(monitors = monitors)
            }
            
            if (page == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(page)
            }
        }

        post {
            val req = call.receive<CreateStatusPageRequest>()
            
            val newId = transaction {
                val existing = StatusPages.select { StatusPages.slug eq req.slug }.singleOrNull()
                if (existing != null) return@transaction null
                
                val id = StatusPages.insertAndGetId {
                    it[name] = req.name
                    it[slug] = req.slug
                    it[description] = req.description
                }.value

                req.monitorIds.forEach { mId ->
                    StatusPageMonitors.insert {
                        it[statusPageId] = id
                        it[monitorId] = mId
                    }
                }
                id
            }
            
            if (newId == null) {
                call.respond(HttpStatusCode.Conflict, "Slug already exists")
            } else {
                call.respond(HttpStatusCode.Created, mapOf("id" to newId.toString()))
            }
        }
    }
}
