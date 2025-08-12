package com.chitfund.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import io.ktor.http.*
import com.chitfund.backend.routes.*
import java.io.File

fun Application.configureRouting() {
    routing {
        // Basic API endpoints 
        get("/api") {
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
        
        // Serve static web content with proper path resolution
        try {
            val webDir = File("web")
            if (webDir.exists() && webDir.isDirectory) {
                staticFiles("/", webDir) {
                    default("index.html")
                }
            } else {
                // Fallback: try relative path from project root  
                val relativeWebDir = File("../web")
                if (relativeWebDir.exists() && relativeWebDir.isDirectory) {
                    staticFiles("/", relativeWebDir) {
                        default("index.html")
                    }
                }
            }
        } catch (e: Exception) {
            // If static file serving fails, log but don't crash the application
            application.log.warn("Failed to configure static file serving: ${e.message}")
        }
    }
}