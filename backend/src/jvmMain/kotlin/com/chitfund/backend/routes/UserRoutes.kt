package com.chitfund.backend.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.chitfund.shared.data.*
import com.chitfund.shared.utils.Result
import com.chitfund.backend.services.AuthService
import com.chitfund.backend.services.UserService

/**
 * Extract authenticated user ID from the Authorization header JWT.
 * Returns null if missing/invalid.
 */
private fun ApplicationCall.authenticatedUserId(): String? {
    val authHeader = request.headers["Authorization"] ?: return null
    val token = authHeader.removePrefix("Bearer ").trim()
    if (token.isBlank()) return null
    return AuthService().verifyToken(token)
}

fun Route.userRoutes() {
    val userService = UserService()
    
    route("/users") {
        get("/profile") {
            val userId = call.authenticatedUserId()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse<String>(success = false, message = "Authentication required"))
                return@get
            }
            
            when (val result = userService.getUserProfile(userId)) {
                is Result.Success -> {
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result.data))
                }
                is Result.Error -> {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<User>(success = false, message = result.message))
                }
            }
        }
    }
}