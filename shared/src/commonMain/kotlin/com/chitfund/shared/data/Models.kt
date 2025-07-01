package com.chitfund.shared.data

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val mobile: String,
    val name: String,
    val isEmailVerified: Boolean = false,
    val isMobileVerified: Boolean = false,
    val createdAt: String
)

@Serializable
data class Chit(
    val id: String,
    val name: String,
    val fundAmount: Long, // in paisa (₹1L = 10,000,000 paisa)
    val tenure: Int, // in months (12-24)
    val memberCount: Int, // 10-25, multiples of 5
    val startMonth: String, // YYYY-MM format
    val endMonth: String, // YYYY-MM format
    val payoutMethod: PayoutMethod,
    val moderatorId: String,
    val status: ChitStatus,
    val members: List<ChitMember> = emptyList(),
    val createdAt: String
)

@Serializable
enum class PayoutMethod {
    RANDOM, VOTING
}

@Serializable
enum class ChitStatus {
    OPEN, ACTIVE, CLOSED
}

@Serializable
data class ChitMember(
    val userId: String,
    val userName: String,
    val joinedAt: String,
    val status: MemberStatus,
    val payments: List<Payment> = emptyList()
)

@Serializable
enum class MemberStatus {
    INVITED, JOINED, APPROVED, REJECTED
}

@Serializable
data class Payment(
    val id: String,
    val chitId: String,
    val memberId: String,
    val amount: Long, // in paisa
    val month: String, // YYYY-MM format
    val status: PaymentStatus,
    val paidAt: String?
)

@Serializable
enum class PaymentStatus {
    PENDING, PAID, OVERDUE
}

@Serializable
data class Payout(
    val id: String,
    val chitId: String,
    val recipientId: String,
    val amount: Long, // in paisa
    val month: String, // YYYY-MM format
    val status: PayoutStatus,
    val paidAt: String?
)

@Serializable
enum class PayoutStatus {
    PENDING, PAID
}