package com.chitfund.backend.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.chitfund.shared.data.*

fun Route.chitRoutes() {
    route("/chits") {
        get {
            // TODO: Get chits for authenticated user
            val chits = listOf<Chit>()
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = chits))
        }
        
        post {
            val request = call.receive<CreateChitRequest>()
            
            // TODO: Create new chit
            val chit = Chit(
                id = "chit-123",
                name = request.name,
                fundAmount = request.fundAmount,
                tenure = request.tenure,
                memberCount = request.memberCount,
                startMonth = request.startMonth,
                endMonth = calculateEndMonth(request.startMonth, request.tenure),
                payoutMethod = request.payoutMethod,
                moderatorId = "user-123", // TODO: Get from auth
                status = ChitStatus.OPEN,
                createdAt = "2024-01-01T00:00:00Z"
            )
            
            call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = chit))
        }
        
        get("/{id}") {
            val chitId = call.parameters["id"]
            
            // TODO: Get chit by ID
            val chit = Chit(
                id = chitId ?: "",
                name = "Sample Chit",
                fundAmount = 1000000000L, // ₹10L
                tenure = 12,
                memberCount = 10,
                startMonth = "2024-01",
                endMonth = "2024-12",
                payoutMethod = PayoutMethod.RANDOM,
                moderatorId = "user-123",
                status = ChitStatus.OPEN,
                createdAt = "2024-01-01T00:00:00Z"
            )
            
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = chit))
        }
        
        post("/{id}/invite") {
            val chitId = call.parameters["id"]
            val request = call.receive<InviteMemberRequest>()
            
            // TODO: Send invitation
            call.respond(HttpStatusCode.OK, ApiResponse<String>(success = true, message = "Invitation sent"))
        }
        
        post("/{id}/join") {
            val chitId = call.parameters["id"]
            val request = call.receive<JoinChitRequest>()
            
            // TODO: Add member to chit
            call.respond(HttpStatusCode.OK, ApiResponse<String>(success = true, message = "Joined chit successfully"))
        }
    }
}

private fun calculateEndMonth(startMonth: String, tenure: Int): String {
    // Simple calculation - in real implementation, use proper date handling
    val parts = startMonth.split("-")
    val year = parts[0].toInt()
    val month = parts[1].toInt()
    
    val endMonth = month + tenure - 1
    val endYear = if (endMonth > 12) year + (endMonth - 1) / 12 else year
    val finalMonth = if (endMonth > 12) ((endMonth - 1) % 12) + 1 else endMonth
    
    return "$endYear-${finalMonth.toString().padStart(2, '0')}"
}