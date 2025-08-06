package com.chitfund.backend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.defaultheaders.*

fun Application.configureSecurity() {
    install(DefaultHeaders) {
        // Security headers for production readiness
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "DENY")
        header("X-XSS-Protection", "1; mode=block")
        header("Referrer-Policy", "strict-origin-when-cross-origin")
        header("Permissions-Policy", "geolocation=(), microphone=(), camera=()")
        
        // Content Security Policy - restrict resource loading
        header("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' cdnjs.cloudflare.com; " +
            "style-src 'self' 'unsafe-inline' cdnjs.cloudflare.com fonts.googleapis.com; " +
            "img-src 'self' data: https:; " +
            "connect-src 'self' chitfund-webapp.azurewebsites.net; " +
            "font-src 'self' cdnjs.cloudflare.com fonts.gstatic.com; " +
            "object-src 'none'; " +
            "base-uri 'self'"
        )
        
        // HSTS for HTTPS - check if HTTPS is enabled
        val isHttpsEnabled = this@configureSecurity.environment.config.propertyOrNull("ktor.deployment.ssl")?.getString()?.toBoolean() ?: false
        if (isHttpsEnabled) {
            header("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
        }
        
        // Additional security headers
        header("X-Permitted-Cross-Domain-Policies", "none")
        header("Cross-Origin-Embedder-Policy", "require-corp")
        header("Cross-Origin-Opener-Policy", "same-origin")
        header("Cross-Origin-Resource-Policy", "same-site")
    }
}