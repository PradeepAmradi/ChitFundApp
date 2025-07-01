package com.chitfund.shared.domain

/**
 * Use cases for authentication operations
 */
interface AuthUseCase {
    suspend fun login(email: String?, mobile: String?): Result<String>
    suspend fun verifyOtp(email: String?, mobile: String?, otp: String): Result<AuthResult>
}

/**
 * Use cases for chit management operations
 */
interface ChitUseCase {
    suspend fun createChit(request: CreateChitParams): Result<ChitResult>
    suspend fun getChits(userId: String): Result<List<ChitResult>>
    suspend fun inviteMember(chitId: String, email: String?, mobile: String?): Result<Boolean>
    suspend fun joinChit(chitId: String, userId: String): Result<Boolean>
}

/**
 * Data classes for use case parameters and results
 */
data class CreateChitParams(
    val name: String,
    val fundAmount: Long,
    val tenure: Int,
    val memberCount: Int,
    val startMonth: String,
    val payoutMethod: String
)

data class AuthResult(
    val token: String,
    val userId: String
)

data class ChitResult(
    val id: String,
    val name: String,
    val fundAmount: Long,
    val status: String,
    val memberCount: Int,
    val currentMembers: Int
)