package com.chitfund.backend.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.chitfund.shared.data.*
import com.chitfund.shared.utils.Result
import com.chitfund.backend.db.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.time.LocalDateTime
import kotlin.random.Random

class AuthService {
    
    companion object {
        private const val ACCESS_TOKEN_EXPIRY = 15 * 60 * 1000L // 15 minutes
        private const val REFRESH_TOKEN_EXPIRY = 7 * 24 * 60 * 60 * 1000L // 7 days
        private const val JWT_ISSUER = "chitfund-app"
        private const val JWT_AUDIENCE = "chitfund-users"
        
        // In production, this should come from environment variable
        private val JWT_SECRET = System.getenv("JWT_SECRET") ?: "chitfund-default-secret-key-please-change-in-production"
        private val algorithm = Algorithm.HMAC256(JWT_SECRET)
    }
    
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
                
                // Generate JWT tokens
                val tokens = generateTokens(user.id)
                
                Result.Success(
                    AuthResponse(
                        success = true,
                        token = tokens.accessToken,
                        refreshToken = tokens.refreshToken,
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
    
    fun generateTokens(userId: String): TokenPair {
        val now = System.currentTimeMillis()
        
        val accessToken = JWT.create()
            .withSubject(userId)
            .withIssuer(JWT_ISSUER)
            .withAudience(JWT_AUDIENCE)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + ACCESS_TOKEN_EXPIRY))
            .withClaim("type", "access")
            .sign(algorithm)
            
        val refreshToken = JWT.create()
            .withSubject(userId)
            .withIssuer(JWT_ISSUER)
            .withAudience(JWT_AUDIENCE)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + REFRESH_TOKEN_EXPIRY))
            .withClaim("type", "refresh")
            .sign(algorithm)
            
        return TokenPair(accessToken, refreshToken)
    }
    
    fun verifyToken(token: String): String? {
        return try {
            val decodedJWT = JWT.require(algorithm)
                .withIssuer(JWT_ISSUER)
                .withAudience(JWT_AUDIENCE)
                .build()
                .verify(token)
            decodedJWT.subject
        } catch (e: Exception) {
            null
        }
    }
    
    fun refreshAccessToken(refreshToken: String): Result<TokenPair> {
        return try {
            val decodedJWT = JWT.require(algorithm)
                .withIssuer(JWT_ISSUER)
                .withAudience(JWT_AUDIENCE)
                .withClaim("type", "refresh")
                .build()
                .verify(refreshToken)
                
            val userId = decodedJWT.subject
            val tokens = generateTokens(userId)
            Result.Success(tokens)
        } catch (e: Exception) {
            Result.Error("Invalid refresh token")
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

data class TokenPair(
    val accessToken: String,
    val refreshToken: String
)