package com.kping.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.kping.routes.monitorRouting

fun Application.configureRouting() {
    routing {
        get("/api/health") {
            call.respondText("OK")
        }
        route("/api") {
            monitorRouting()
        }
    }
}
