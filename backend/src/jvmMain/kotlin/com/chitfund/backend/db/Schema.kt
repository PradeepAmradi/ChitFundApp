package com.chitfund.backend.db

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Users : UUIDTable() {
    val email = varchar("email", 255).uniqueIndex()
    val mobile = varchar("mobile", 20).uniqueIndex()
    val name = varchar("name", 255)
    val isEmailVerified = bool("is_email_verified").default(false)
    val isMobileVerified = bool("is_mobile_verified").default(false)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}

object Chits : UUIDTable() {
    val name = varchar("name", 255)
    val fundAmount = long("fund_amount") // in paisa
    val tenure = integer("tenure") // months
    val memberCount = integer("member_count")
    val startMonth = varchar("start_month", 7) // YYYY-MM
    val endMonth = varchar("end_month", 7) // YYYY-MM
    val payoutMethod = enumeration("payout_method", PayoutMethodDb::class)
    val moderatorId = uuid("moderator_id").references(Users.id)
    val status = enumeration("status", ChitStatusDb::class).default(ChitStatusDb.OPEN)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}

object ChitMembers : Table() {
    val chitId = uuid("chit_id").references(Chits.id)
    val userId = uuid("user_id").references(Users.id)
    val status = enumeration("status", MemberStatusDb::class).default(MemberStatusDb.INVITED)
    val joinedAt = datetime("joined_at").default(LocalDateTime.now())
    
    override val primaryKey = PrimaryKey(chitId, userId)
}

object Payments : UUIDTable() {
    val chitId = uuid("chit_id").references(Chits.id)
    val memberId = uuid("member_id").references(Users.id)
    val amount = long("amount") // in paisa
    val month = varchar("month", 7) // YYYY-MM
    val status = enumeration("status", PaymentStatusDb::class).default(PaymentStatusDb.PENDING)
    val paidAt = datetime("paid_at").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}

object Payouts : UUIDTable() {
    val chitId = uuid("chit_id").references(Chits.id)
    val recipientId = uuid("recipient_id").references(Users.id)
    val amount = long("amount") // in paisa
    val month = varchar("month", 7) // YYYY-MM
    val status = enumeration("status", PayoutStatusDb::class).default(PayoutStatusDb.PENDING)
    val paidAt = datetime("paid_at").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}

// Database enum classes
enum class PayoutMethodDb { RANDOM, VOTING }
enum class ChitStatusDb { OPEN, ACTIVE, CLOSED }
enum class MemberStatusDb { INVITED, JOINED, APPROVED, REJECTED }
enum class PaymentStatusDb { PENDING, PAID, OVERDUE }
enum class PayoutStatusDb { PENDING, PAID }