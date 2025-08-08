package com.chitfund.shared.data

import kotlinx.serialization.Serializable

// API Request/Response models
@Serializable
data class LoginRequest(
    val email: String? = null,
    val mobile: String? = null
)

@Serializable
data class VerifyOtpRequest(
    val email: String? = null,
    val mobile: String? = null,
    val otp: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val token: String? = null,
    val refreshToken: String? = null,
    val user: User? = null,
    val message: String? = null
)

@Serializable
data class CreateChitRequest(
    val name: String,
    val fundAmount: Long,
    val tenure: Int,
    val memberCount: Int,
    val startMonth: String,
    val payoutMethod: PayoutMethod
)

@Serializable
data class InviteMemberRequest(
    val chitId: String,
    val email: String? = null,
    val mobile: String? = null
)

@Serializable
data class JoinChitRequest(
    val chitId: String
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)