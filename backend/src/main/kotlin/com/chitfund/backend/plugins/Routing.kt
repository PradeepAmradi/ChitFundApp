package com.chitfund.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.chitfund.backend.routes.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Chit Fund Backend API - Version 1.0")
        }
        
        // API routes
        route("/api/v1") {
            authRoutes()
            chitRoutes()
            userRoutes()
        }
    }
}