package com.chitfund.backend.services

import com.chitfund.shared.data.*
import com.chitfund.shared.utils.Result
import com.chitfund.backend.db.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.time.LocalDateTime
import kotlin.random.Random

class AuthService {
    
    private val otpStore = mutableMapOf<String, OtpData>()
    
    data class OtpData(
        val otp: String,
        val createdAt: LocalDateTime,
        val expiresAt: LocalDateTime
    )
    
    fun initiateLogin(request: LoginRequest): Result<String> {
        return try {
            // Generate 6-digit OTP
            val otp = generateOtp()
            val now = LocalDateTime.now()
            val expiresAt = now.plusMinutes(10) // OTP expires in 10 minutes
            
            val key = request.email ?: request.mobile ?: return Result.Error("Email or mobile required")
            
            // Store OTP (in production, this would be stored in Redis or similar)
            otpStore[key] = OtpData(otp, now, expiresAt)
            
            // In production, send OTP via email/SMS
            println("Generated OTP for $key: $otp") // Debug log
            
            Result.Success("OTP sent successfully")
        } catch (e: Exception) {
            Result.Error("Failed to send OTP: ${e.message}")
        }
    }
    
    fun verifyOtp(request: VerifyOtpRequest): Result<AuthResponse> {
        return try {
            val key = request.email ?: request.mobile ?: return Result.Error("Email or mobile required")
            val storedOtpData = otpStore[key] ?: return Result.Error("OTP not found or expired")
            
            // Check OTP validity
            if (storedOtpData.expiresAt.isBefore(LocalDateTime.now())) {
                otpStore.remove(key)
                return Result.Error("OTP has expired")
            }
            
            if (storedOtpData.otp != request.otp) {
                return Result.Error("Invalid OTP")
            }
            
            // Remove OTP after successful verification
            otpStore.remove(key)
            
            transaction {
                val email = request.email
                val mobile = request.mobile
                
                val user = if (email != null) {
                    findOrCreateUserByEmail(email)
                } else if (mobile != null) {
                    findOrCreateUserByMobile(mobile)
                } else {
                    return@transaction Result.Error("Email or mobile required")
                }
                
                // Generate JWT token (simplified - in production use proper JWT library)
                val token = generateJwtToken(user.id)
                
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
    
    private fun generateOtp(): String {
        return Random.nextInt(100000, 999999).toString()
    }
    
    private fun generateJwtToken(userId: String): String {
        // Simplified token generation - in production, use proper JWT library like jose4j
        return "chitfund_token_${userId}_${System.currentTimeMillis()}"
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