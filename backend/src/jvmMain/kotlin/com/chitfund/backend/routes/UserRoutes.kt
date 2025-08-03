package com.chitfund.backend.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.chitfund.shared.data.*
import com.chitfund.shared.utils.Result
import com.chitfund.backend.services.UserService

fun Route.userRoutes() {
    val userService = UserService()
    
    route("/users") {
        get("/profile") {
            val userId = "user-123" // TODO: Get from auth token
            
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