package com.chitfund.backend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.defaultheaders.*

fun Application.configureSecurity() {
    install(DefaultHeaders) {
        // Security headers
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "DENY")
        header("X-XSS-Protection", "1; mode=block")
        header("Referrer-Policy", "strict-origin-when-cross-origin")
        header("Permissions-Policy", "geolocation=(), microphone=(), camera=()")
        
        // Content Security Policy
        header("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' cdnjs.cloudflare.com; " +
            "style-src 'self' 'unsafe-inline' cdnjs.cloudflare.com; " +
            "img-src 'self' data: https:; " +
            "connect-src 'self' localhost:8080 chitfund-webapp.azurewebsites.net; " +
            "font-src 'self' cdnjs.cloudflare.com"
        )
        
        // HSTS for HTTPS - Enable when SSL is configured
        val sslEnabled = System.getenv("SSL_ENABLED")?.toBoolean() ?: false
        if (sslEnabled) {
            header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        }
    }
}