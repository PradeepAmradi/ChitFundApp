package com.chitfund.backend.services

import com.chitfund.shared.data.*
import com.chitfund.shared.utils.Result
import com.chitfund.backend.db.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*
import java.time.LocalDateTime
import kotlin.random.Random

class AuthService {
    
    private val otpStore = mutableMapOf<String, OtpData>()
    private val refreshTokenStore = mutableMapOf<String, RefreshTokenData>()
    
    companion object {
        // JWT Configuration - in production these should be from environment variables
        private const val JWT_SECRET = "secure-jwt-secret-key-change-in-production-256-bits-minimum"
        private const val JWT_ISSUER = "chitfund-app"
        private const val JWT_AUDIENCE = "chitfund-users"
        private const val ACCESS_TOKEN_EXPIRY = 15 * 60 * 1000L // 15 minutes
        private const val REFRESH_TOKEN_EXPIRY = 7 * 24 * 60 * 60 * 1000L // 7 days
        
        private val algorithm = Algorithm.HMAC256(JWT_SECRET)
    }
    
    data class OtpData(
        val otp: String,
        val createdAt: LocalDateTime,
        val expiresAt: LocalDateTime
    )
    
    data class RefreshTokenData(
        val userId: String,
        val createdAt: LocalDateTime,
        val expiresAt: LocalDateTime
    )
    
    data class TokenPair(
        val accessToken: String,
        val refreshToken: String,
        val accessTokenExpiresAt: Long,
        val refreshTokenExpiresAt: Long
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
                
                // Generate JWT tokens with refresh capability
                val tokenPair = generateTokenPair(user)
                
                Result.Success(
                    AuthResponse(
                        success = true,
                        token = tokenPair.accessToken,
                        refreshToken = tokenPair.refreshToken,
                        user = user,
                        message = "Login successful",
                        accessTokenExpiresAt = tokenPair.accessTokenExpiresAt,
                        refreshTokenExpiresAt = tokenPair.refreshTokenExpiresAt
                    )
                )
            }
        } catch (e: Exception) {
            Result.Error("Failed to verify OTP: ${e.message}")
        }
    }
    
    fun refreshToken(refreshToken: String): Result<TokenPair> {
        return try {
            // Verify refresh token
            val tokenData = refreshTokenStore[refreshToken] ?: return Result.Error("Invalid refresh token")
            
            if (tokenData.expiresAt.isBefore(LocalDateTime.now())) {
                refreshTokenStore.remove(refreshToken)
                return Result.Error("Refresh token has expired")
            }
            
            transaction {
                // Find user by ID
                val user = Users.select { Users.id eq UUID.fromString(tokenData.userId) }.firstOrNull()
                    ?: return@transaction Result.Error("User not found")
                
                val userObj = User(
                    id = user[Users.id].toString(),
                    email = user[Users.email],
                    mobile = user[Users.mobile],
                    name = user[Users.name],
                    isEmailVerified = user[Users.isEmailVerified],
                    isMobileVerified = user[Users.isMobileVerified],
                    createdAt = user[Users.createdAt].toString()
                )
                
                // Remove old refresh token
                refreshTokenStore.remove(refreshToken)
                
                // Generate new token pair
                val newTokenPair = generateTokenPair(userObj)
                
                Result.Success(newTokenPair)
            }
        } catch (e: Exception) {
            Result.Error("Failed to refresh token: ${e.message}")
        }
    }
    
    private fun generateTokenPair(user: User): TokenPair {
        val now = System.currentTimeMillis()
        val accessTokenExp = now + ACCESS_TOKEN_EXPIRY
        val refreshTokenExp = now + REFRESH_TOKEN_EXPIRY
        
        // Generate access token (short-lived)
        val accessToken = JWT.create()
            .withSubject(user.id)
            .withClaim("email", user.email)
            .withClaim("mobile", user.mobile)
            .withClaim("name", user.name)
            .withClaim("emailVerified", user.isEmailVerified)
            .withClaim("mobileVerified", user.isMobileVerified)
            .withIssuer(JWT_ISSUER)
            .withAudience(JWT_AUDIENCE)
            .withExpiresAt(Date(accessTokenExp))
            .withIssuedAt(Date(now))
            .withNotBefore(Date(now))
            .sign(algorithm)
        
        // Generate refresh token (long-lived)
        val refreshToken = JWT.create()
            .withSubject(user.id)
            .withIssuer(JWT_ISSUER)
            .withAudience(JWT_AUDIENCE)
            .withExpiresAt(Date(refreshTokenExp))
            .withIssuedAt(Date(now))
            .withClaim("type", "refresh")
            .sign(algorithm)
        
        // Store refresh token
        refreshTokenStore[refreshToken] = RefreshTokenData(
            userId = user.id,
            createdAt = LocalDateTime.now(),
            expiresAt = LocalDateTime.now().plusDays(7)
        )
        
        return TokenPair(accessToken, refreshToken, accessTokenExp, refreshTokenExp)
    }
    
    fun validateAccessToken(token: String): Result<User> {
        return try {
            val jwt = JWT.require(algorithm)
                .withIssuer(JWT_ISSUER)
                .withAudience(JWT_AUDIENCE)
                .build()
                .verify(token)
            
            val userId = jwt.subject
            val email = jwt.getClaim("email").asString()
            val mobile = jwt.getClaim("mobile").asString()
            val name = jwt.getClaim("name").asString()
            val emailVerified = jwt.getClaim("emailVerified").asBoolean()
            val mobileVerified = jwt.getClaim("mobileVerified").asBoolean()
            
            val user = User(
                id = userId,
                email = email,
                mobile = mobile,
                name = name,
                isEmailVerified = emailVerified,
                isMobileVerified = mobileVerified,
                createdAt = ""
            )
            
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error("Invalid or expired access token: ${e.message}")
        }
    }
    
    fun revokeRefreshToken(refreshToken: String): Result<String> {
        return if (refreshTokenStore.remove(refreshToken) != null) {
            Result.Success("Token revoked successfully")
        } else {
            Result.Error("Token not found")
        }
    }
    
    private fun generateOtp(): String {
        return Random.nextInt(100000, 999999).toString()
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