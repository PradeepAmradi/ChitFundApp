package com.chitfund.backend.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.ratelimit.*
import com.chitfund.shared.data.*
import com.chitfund.shared.utils.Result
import com.chitfund.backend.services.AuthService

fun Route.authRoutes() {
    val authService = AuthService()
    
    // Apply daily rate limiting to all auth routes
    rateLimit(RateLimitName("daily")) {
        route("/auth") {
            // Apply stricter auth-specific rate limiting for OTP endpoints
            rateLimit(RateLimitName("auth")) {
                post("/login") {
                    val request = call.receive<LoginRequest>()
                    
                    when (val result = authService.initiateLogin(request)) {
                        is Result.Success -> {
                            call.respond(HttpStatusCode.OK, ApiResponse<String>(success = true, message = result.data))
                        }
                        is Result.Error -> {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, message = result.message))
                        }
                    }
                }
                
                post("/verify-otp") {
                    val request = call.receive<VerifyOtpRequest>()
                    
                    when (val result = authService.verifyOtp(request)) {
                        is Result.Success -> {
                            call.respond(HttpStatusCode.OK, result.data)
                        }
                        is Result.Error -> {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, message = result.message))
                        }
                    }
                }
            }
        }
    }
}