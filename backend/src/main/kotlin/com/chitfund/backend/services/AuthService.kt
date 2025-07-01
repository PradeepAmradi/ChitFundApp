package com.chitfund.backend.services

import com.chitfund.shared.data.*
import com.chitfund.shared.utils.Result
import com.chitfund.backend.db.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class AuthService {
    
    fun initiateLogin(request: LoginRequest): Result<String> {
        return try {
            // TODO: Implement actual OTP generation and sending
            // For now, return success
            Result.Success("OTP sent successfully")
        } catch (e: Exception) {
            Result.Error("Failed to send OTP: ${e.message}")
        }
    }
    
    fun verifyOtp(request: VerifyOtpRequest): Result<AuthResponse> {
        return try {
            transaction {
                // TODO: Implement actual OTP verification
                // For now, create or get user
                val user = if (request.email != null) {
                    findOrCreateUserByEmail(request.email)
                } else if (request.mobile != null) {
                    findOrCreateUserByMobile(request.mobile)
                } else {
                    return@transaction Result.Error("Email or mobile required")
                }
                
                // TODO: Generate actual JWT token
                val token = "dummy-jwt-token-${UUID.randomUUID()}"
                
                Result.Success(
                    AuthResponse(
                        success = true,
                        token = token,
                        user = user
                    )
                )
            }
        } catch (e: Exception) {
            Result.Error("Failed to verify OTP: ${e.message}")
        }
    }
    
    private fun findOrCreateUserByEmail(email: String): User {
        val existingUser = Users.select { Users.email eq email }.firstOrNull()
        
        return if (existingUser != null) {
            User(
                id = existingUser[Users.id].toString(),
                email = existingUser[Users.email],
                mobile = existingUser[Users.mobile],
                name = existingUser[Users.name],
                isEmailVerified = existingUser[Users.isEmailVerified],
                isMobileVerified = existingUser[Users.isMobileVerified],
                createdAt = existingUser[Users.createdAt].toString()
            )
        } else {
            val userId = UUID.randomUUID()
            Users.insert {
                it[id] = userId
                it[Users.email] = email
                it[mobile] = "" // Will be updated later
                it[name] = email.substringBefore("@") // Default name
                it[isEmailVerified] = true
                it[isMobileVerified] = false
            }
            
            User(
                id = userId.toString(),
                email = email,
                mobile = "",
                name = email.substringBefore("@"),
                isEmailVerified = true,
                isMobileVerified = false,
                createdAt = java.time.LocalDateTime.now().toString()
            )
        }
    }
    
    private fun findOrCreateUserByMobile(mobile: String): User {
        val existingUser = Users.select { Users.mobile eq mobile }.firstOrNull()
        
        return if (existingUser != null) {
            User(
                id = existingUser[Users.id].toString(),
                email = existingUser[Users.email],
                mobile = existingUser[Users.mobile],
                name = existingUser[Users.name],
                isEmailVerified = existingUser[Users.isEmailVerified],
                isMobileVerified = existingUser[Users.isMobileVerified],
                createdAt = existingUser[Users.createdAt].toString()
            )
        } else {
            val userId = UUID.randomUUID()
            Users.insert {
                it[id] = userId
                it[email] = "" // Will be updated later
                it[Users.mobile] = mobile
                it[name] = "User" // Default name
                it[isEmailVerified] = false
                it[isMobileVerified] = true
            }
            
            User(
                id = userId.toString(),
                email = "",
                mobile = mobile,
                name = "User",
                isEmailVerified = false,
                isMobileVerified = true,
                createdAt = java.time.LocalDateTime.now().toString()
            )
        }
    }
}