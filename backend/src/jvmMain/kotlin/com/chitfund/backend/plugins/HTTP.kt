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
        
        // Development origins
        allowHost("localhost:3000") // Web dev server
        allowHost("localhost:8080") // Backend-served frontend
        allowHost("127.0.0.1:8080")
        allowHost("127.0.0.1:3000")
        
        // Production
        allowHost("chitfund-webapp.azurewebsites.net", schemes = listOf("https"))
        allowHost("app.chitfund.com", schemes = listOf("https"))
        allowHost("chitfund-app.com", schemes = listOf("https"))
    }
}