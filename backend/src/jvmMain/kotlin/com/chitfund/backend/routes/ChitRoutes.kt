package com.chitfund.backend.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.ratelimit.*
import com.chitfund.shared.data.*
import com.chitfund.shared.utils.Result
import com.chitfund.backend.services.ChitService

fun Route.chitRoutes() {
    val chitService = ChitService()
    
    // Apply daily rate limiting to all chit routes
    rateLimit(RateLimitName("daily")) {
        route("/chits") {
            // Read operations use standard API rate limiting
            rateLimit(RateLimitName("api")) {
                get {
                    // For now, return empty list - implement proper auth later
                    val userId = "user-123" // TODO: Get from auth token
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
            }
            
            // Critical operations (creating chits, financial operations) use stricter rate limiting
            rateLimit(RateLimitName("critical")) {
                post {
                    val request = call.receive<CreateChitRequest>()
                    val moderatorId = "user-123" // TODO: Get from auth token
                    
                    when (val result = chitService.createChit(request, moderatorId)) {
                        is Result.Success -> {
                            call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = result.data))
                        }
                        is Result.Error -> {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<Chit>(success = false, message = result.message))
                        }
                    }
                }
                
                post("/{id}/invite") {
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
                    val chitId = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(success = false, message = "Chit ID required")
                    )
                    val request = call.receive<JoinChitRequest>()
                    val userId = "user-123" // TODO: Get from auth token
                    
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
    }
}