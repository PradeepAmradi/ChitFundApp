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
        
        get("/health") {
            call.respond(mapOf(
                "status" to "healthy",
                "service" to "ChitFund Backend API",
                "version" to "1.0",
                "timestamp" to System.currentTimeMillis()
            ))
        }
        
        // API routes
        route("/api/v1") {
            authRoutes()
            chitRoutes()
            userRoutes()
        }
    }
}