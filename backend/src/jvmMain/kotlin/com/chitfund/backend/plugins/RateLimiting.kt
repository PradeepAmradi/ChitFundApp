package com.chitfund.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

fun Application.configureRateLimit() {
    install(RateLimit) {
        // Global daily rate limit - aggressive as requested (100 requests per user per day)
        register(RateLimitName("daily")) {
            rateLimiter(limit = 100, refillPeriod = 1.days)
        }
        
        // Authentication endpoints (OTP) - very strict limits to prevent abuse
        register(RateLimitName("auth")) {
            rateLimiter(limit = 5, refillPeriod = 1.minutes)
        }
        
        // High-frequency operations - moderate limits
        register(RateLimitName("api")) {
            rateLimiter(limit = 50, refillPeriod = 1.minutes)
        }
        
        // Critical operations (payments, transactions) - strictest limits
        register(RateLimitName("critical")) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
        }
    }
}