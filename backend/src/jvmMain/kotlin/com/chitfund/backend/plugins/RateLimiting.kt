package com.chitfund.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.time.*

data class RateLimitInfo(
    val requests: Int,
    val lastReset: Instant
)

object RateLimiter {
    private val storage = ConcurrentHashMap<String, RateLimitInfo>()
    private val mutex = Mutex()
    
    suspend fun isAllowed(key: String, limit: Int, windowMinutes: Int): Boolean = mutex.withLock {
        val now = Instant.now()
        val windowStart = now.minus(Duration.ofMinutes(windowMinutes.toLong()))
        
        val current = storage[key]
        
        if (current == null || current.lastReset.isBefore(windowStart)) {
            // First request or window expired, reset
            storage[key] = RateLimitInfo(1, now)
            return true
        }
        
        if (current.requests >= limit) {
            return false
        }
        
        // Increment counter
        storage[key] = current.copy(requests = current.requests + 1)
        return true
    }
}

class RateLimitingPlugin(private val config: Configuration) {
    class Configuration {
        var globalLimit = 100
        var globalWindowMinutes = 24 * 60 // 24 hours for daily limit
        var authLimit = 5
        var authWindowMinutes = 60 // 1 hour
        var apiLimit = 50
        var apiWindowMinutes = 60 // 1 hour
        var sensitiveLimit = 3
        var sensitiveWindowMinutes = 60 // 1 hour
    }
    
    companion object Plugin : BaseApplicationPlugin<ApplicationCallPipeline, Configuration, RateLimitingPlugin> {
        override val key = AttributeKey<RateLimitingPlugin>("RateLimitingPlugin")
        
        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): RateLimitingPlugin {
            val configuration = Configuration().apply(configure)
            val plugin = RateLimitingPlugin(configuration)
            
            pipeline.intercept(ApplicationCallPipeline.Plugins) {
                val clientIP = call.request.headers["X-Forwarded-For"] 
                    ?: call.request.local.remoteHost
                
                val path = call.request.path()
                val method = call.request.httpMethod.value
                
                // Skip rate limiting for health checks and basic info endpoints
                if (path == "/health" || path == "/api" || path.startsWith("/static")) {
                    proceed()
                    return@intercept
                }
                
                val (limit, window) = when {
                    path.startsWith("/api/v1/auth") -> configuration.authLimit to configuration.authWindowMinutes
                    path.startsWith("/api/v1/chits") && method == "POST" -> configuration.sensitiveLimit to configuration.sensitiveWindowMinutes
                    path.startsWith("/api/v1") -> configuration.apiLimit to configuration.apiWindowMinutes
                    else -> configuration.globalLimit to configuration.globalWindowMinutes
                }
                
                val key = "$clientIP:$path"
                
                if (!RateLimiter.isAllowed(key, limit, window)) {
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        mapOf(
                            "error" to "Rate limit exceeded",
                            "message" to "Too many requests. Please try again later."
                        )
                    )
                    return@intercept
                }
                
                proceed()
            }
            
            return plugin
        }
    }
}

fun Application.configureRateLimit() {
    install(RateLimitingPlugin) {
        // Aggressive daily rate limiting as requested - 100 requests per user per day
        globalLimit = System.getenv("RATE_LIMIT_GLOBAL")?.toIntOrNull() ?: 100
        globalWindowMinutes = 24 * 60 // 24 hours for daily limit
        
        // Auth endpoints - very strict to prevent abuse
        authLimit = System.getenv("RATE_LIMIT_AUTH")?.toIntOrNull() ?: 5
        authWindowMinutes = 60 // 1 hour
        
        // API endpoints - moderate limits
        apiLimit = System.getenv("RATE_LIMIT_API")?.toIntOrNull() ?: 50
        apiWindowMinutes = 60 // 1 hour
        
        // Sensitive operations - very strict
        sensitiveLimit = System.getenv("RATE_LIMIT_SENSITIVE")?.toIntOrNull() ?: 3
        sensitiveWindowMinutes = 60 // 1 hour
    }
}