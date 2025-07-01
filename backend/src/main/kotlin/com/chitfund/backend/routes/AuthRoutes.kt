package com.chitfund.backend.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.chitfund.shared.data.*

fun Route.authRoutes() {
    route("/auth") {
        post("/login") {
            val request = call.receive<LoginRequest>()
            
            // TODO: Implement OTP generation and sending
            val response = AuthResponse(
                success = true,
                message = "OTP sent successfully"
            )
            
            call.respond(HttpStatusCode.OK, response)
        }
        
        post("/verify-otp") {
            val request = call.receive<VerifyOtpRequest>()
            
            // TODO: Implement OTP verification
            val response = AuthResponse(
                success = true,
                token = "dummy-jwt-token",
                user = User(
                    id = "user-123",
                    email = request.email ?: "",
                    mobile = request.mobile ?: "",
                    name = "John Doe",
                    isEmailVerified = true,
                    isMobileVerified = true,
                    createdAt = "2024-01-01T00:00:00Z"
                )
            )
            
            call.respond(HttpStatusCode.OK, response)
        }
    }
}