package com.chitfund.backend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureHTTP() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
        
        // ✅ SECURE: Specify allowed origins instead of anyHost()
        allowHost("localhost:3000") // Development frontend
        allowHost("chitfund-webapp.azurewebsites.net", schemes = listOf("https")) // Production
        allowHost("app.chitfund.com", schemes = listOf("https")) // Production domain if used
        allowHost("chitfund-app.com", schemes = listOf("https")) // Production domain if used
    }
}