package com.chitfund.backend.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.chitfund.shared.data.*
import com.chitfund.shared.utils.Result
import com.chitfund.backend.services.AuthService
import com.chitfund.backend.services.ChitService

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

fun Route.chitRoutes() {
    val chitService = ChitService()
    
    route("/chits") {
        get {
            val userId = call.authenticatedUserId()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse<String>(success = false, message = "Authentication required"))
                return@get
            }
            when (val result = chitService.getChitsByUser(userId)) {
                is Result.Success -> {
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result.data))
                }
                is Result.Error -> {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<List<Chit>>(success = false, message = result.message))
                }
            }
        }
        
        get("/{id}") {
            val chitId = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest, 
                ApiResponse<String>(success = false, message = "Chit ID required")
            )
            
            when (val result = chitService.getChitById(chitId)) {
                is Result.Success -> {
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result.data))
                }
                is Result.Error -> {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Chit>(success = false, message = result.message))
                }
            }
        }
        
        post {
            val userId = call.authenticatedUserId()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse<String>(success = false, message = "Authentication required"))
                return@post
            }
            val request = call.receive<CreateChitRequest>()
            
            when (val result = chitService.createChit(request, userId)) {
                is Result.Success -> {
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = result.data))
                }
                is Result.Error -> {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Chit>(success = false, message = result.message))
                }
            }
        }
        
        post("/{id}/invite") {
            val userId = call.authenticatedUserId()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse<String>(success = false, message = "Authentication required"))
                return@post
            }
            val chitId = call.parameters["id"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<String>(success = false, message = "Chit ID required")
            )
            val request = call.receive<InviteMemberRequest>()
            
            when (val result = chitService.inviteMember(chitId, request)) {
                is Result.Success -> {
                    call.respond(HttpStatusCode.OK, ApiResponse<String>(success = true, message = "Invitation sent"))
                }
                is Result.Error -> {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, message = result.message))
                }
            }
        }
        
        post("/{id}/join") {
            val userId = call.authenticatedUserId()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse<String>(success = false, message = "Authentication required"))
                return@post
            }
            val chitId = call.parameters["id"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<String>(success = false, message = "Chit ID required")
            )
            val request = call.receive<JoinChitRequest>()
            
            when (val result = chitService.joinChit(chitId, userId)) {
                is Result.Success -> {
                    call.respond(HttpStatusCode.OK, ApiResponse<String>(success = true, message = "Joined chit successfully"))
                }
                is Result.Error -> {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, message = result.message))
                }
            }
        }
    }
}