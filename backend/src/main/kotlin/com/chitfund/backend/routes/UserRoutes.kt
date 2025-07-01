package com.chitfund.backend.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.chitfund.shared.data.*

fun Route.userRoutes() {
    route("/users") {
        get("/profile") {
            // TODO: Get authenticated user profile
            val user = User(
                id = "user-123",
                email = "john@example.com",
                mobile = "+1234567890",
                name = "John Doe",
                isEmailVerified = true,
                isMobileVerified = true,
                createdAt = "2024-01-01T00:00:00Z"
            )
            
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = user))
        }
    }
}