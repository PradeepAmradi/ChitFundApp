package com.chitfund.backend.services

import com.chitfund.shared.data.*
import com.chitfund.backend.db.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MockDataService {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    
    fun initializeMockData() {
        transaction {
            // Check if mock data already exists
            if (Users.selectAll().count() > 0) {
                return@transaction
            }
            
            println("Initializing mock data...")
            
            // Create mock users
            val users = createMockUsers()
            
            // Create mock chits following business rules
            createMockChits(users)
            
            println("Mock data initialization completed successfully!")
        }
    }
    
    private fun createMockUsers(): List<UUID> {
        val userIds = mutableListOf<UUID>()
        
        // Create moderator users
        val moderator1 = UUID.randomUUID()
        Users.insert {
            it[id] = moderator1
            it[email] = "moderator1@chitfund.com"
            it[mobile] = "+919876543210"
            it[name] = "Ramesh Kumar"
            it[isEmailVerified] = true
            it[isMobileVerified] = true
            it[createdAt] = LocalDateTime.now()
        }
        userIds.add(moderator1)
        
        val moderator2 = UUID.randomUUID()
        Users.insert {
            it[id] = moderator2
            it[email] = "moderator2@chitfund.com"
            it[mobile] = "+919876543211"
            it[name] = "Priya Sharma"
            it[isEmailVerified] = true
            it[isMobileVerified] = true
            it[createdAt] = LocalDateTime.now()
        }
        userIds.add(moderator2)
        
        val moderator3 = UUID.randomUUID()
        Users.insert {
            it[id] = moderator3
            it[email] = "moderator3@chitfund.com"
            it[mobile] = "+919876543212"
            it[name] = "Suresh Reddy"
            it[isEmailVerified] = true
            it[isMobileVerified] = true
            it[createdAt] = LocalDateTime.now()
        }
        userIds.add(moderator3)
        
        // Create member users
        val memberNames = listOf(
            "Amit Singh", "Sunita Devi", "Rajesh Gupta", "Meera Joshi", "Vikram Patel",
            "Kavya Nair", "Ravi Kumar", "Deepika Singh", "Anish Tiwari", "Pooja Sharma",
            "Arjun Rao", "Neha Verma", "Kiran Jha", "Sushma Pillai", "Manoj Agarwal",
            "Rekha Devi", "Sachin Yadav", "Anjali Mishra", "Rohit Khanna", "Shanti Bai",
            "Nitin Jain", "Preeti Gupta", "Ashok Kumar", "Madhuri Das", "Sanjay Mehta"
        )
        
        memberNames.forEachIndexed { index, name ->
            val memberId = UUID.randomUUID()
            Users.insert {
                it[id] = memberId
                it[email] = "member${index + 1}@chitfund.com"
                it[mobile] = "+91987654${(3213 + index).toString().padStart(4, '0')}"
                it[Users.name] = name
                it[isEmailVerified] = true
                it[isMobileVerified] = true
                it[createdAt] = LocalDateTime.now()
            }
            userIds.add(memberId)
        }
        
        return userIds
    }
    
    private fun createMockChits(userIds: List<UUID>) {
        // Business Rule Calculations:
        // Fund Amount = Monthly Contribution × Member Count × Tenure
        // Monthly Contribution = Fund Amount / (Member Count × Tenure)
        
        createOpenChit(userIds[0], userIds.drop(3).take(15)) // Moderator 1 with 15 members
        createActiveChit(userIds[1], userIds.drop(3).take(20)) // Moderator 2 with 20 members  
        createClosedChit(userIds[2], userIds.drop(3).take(10)) // Moderator 3 with 10 members
    }
    
    private fun createOpenChit(moderatorId: UUID, memberIds: List<UUID>) {
        val chitId = UUID.randomUUID()
        val memberCount = 20 // Target member count
        val tenure = 20 // months
        val fundAmount = 500000000000L // ₹5L in paisa (5 × 100,000,000,000)
        val monthlyContribution = fundAmount / (memberCount * tenure) // ₹12,500 per member per month
        
        // Start month is next month (open for joining)
        val startMonth = "2025-09"
        val endMonth = "2027-04" // Calculate: 2025-09 + 20 months = 2027-04
        
        Chits.insert {
            it[id] = chitId
            it[name] = "Business Expansion Fund"
            it[Chits.fundAmount] = fundAmount
            it[Chits.tenure] = tenure
            it[Chits.memberCount] = memberCount
            it[Chits.startMonth] = startMonth
            it[Chits.endMonth] = endMonth
            it[payoutMethod] = PayoutMethodDb.RANDOM
            it[Chits.moderatorId] = moderatorId
            it[status] = ChitStatusDb.OPEN
            it[createdAt] = LocalDateTime.now()
        }
        
        // Add only 3 members out of 20 (as requested)
        memberIds.take(3).forEach { memberId ->
            ChitMembers.insert {
                it[ChitMembers.chitId] = chitId
                it[ChitMembers.userId] = memberId
                it[ChitMembers.status] = MemberStatusDb.JOINED
                it[ChitMembers.joinedAt] = LocalDateTime.now().minusDays((memberIds.indexOf(memberId) + 1).toLong())
            }
        }
        
        println("Created OPEN chit: Fund ₹${fundAmount/100000000000}L, Monthly ₹${monthlyContribution/100}")
    }
    
    private fun createActiveChit(moderatorId: UUID, memberIds: List<UUID>) {
        val chitId = UUID.randomUUID()
        val memberCount = 10 // Full member count achieved
        val tenure = 12 // months
        val fundAmount = 200000000000L // ₹2L in paisa (2 × 100,000,000,000) - adjusted from ₹1.2L to comply with validation rules
        val monthlyContribution = fundAmount / (memberCount * tenure) // ₹16,666.67 per member per month
        
        // Started 6 months ago, currently in month 7
        val startMonth = "2025-02" 
        val endMonth = "2026-01" // Calculate: 2025-02 + 12 months = 2026-01
        val currentActiveMonth = "2025-08" // Currently in 7th month
        
        Chits.insert {
            it[id] = chitId
            it[name] = "Family Savings Fund"
            it[Chits.fundAmount] = fundAmount
            it[Chits.tenure] = tenure
            it[Chits.memberCount] = memberCount
            it[Chits.startMonth] = startMonth
            it[Chits.endMonth] = endMonth
            it[payoutMethod] = PayoutMethodDb.VOTING
            it[Chits.moderatorId] = moderatorId
            it[status] = ChitStatusDb.ACTIVE
            it[createdAt] = LocalDateTime.now().minusMonths(7)
        }
        
        // Add all required members (full capacity - 10/10)
        memberIds.take(memberCount).forEach { memberId ->
            ChitMembers.insert {
                it[ChitMembers.chitId] = chitId
                it[ChitMembers.userId] = memberId
                it[ChitMembers.status] = MemberStatusDb.APPROVED
                it[ChitMembers.joinedAt] = LocalDateTime.now().minusMonths(7).plusDays(memberIds.indexOf(memberId).toLong())
            }
        }
        
        // Add payment records for the first 6 completed months
        val months = listOf("2025-02", "2025-03", "2025-04", "2025-05", "2025-06", "2025-07")
        memberIds.take(memberCount).forEach { memberId ->
            months.forEach { month ->
                val paymentId = UUID.randomUUID()
                Payments.insert {
                    it[id] = paymentId
                    it[Payments.chitId] = chitId
                    it[Payments.memberId] = memberId
                    it[amount] = monthlyContribution // ₹16,666.67 in paisa
                    it[Payments.month] = month
                    it[status] = PaymentStatusDb.PAID
                    it[paidAt] = LocalDateTime.parse("${month}-15T10:00:00")
                    it[createdAt] = LocalDateTime.parse("${month}-01T09:00:00")
                }
            }
        }
        
        // Add pending payments for current month (August 2025)
        memberIds.take(memberCount).forEach { memberId ->
            val paymentId = UUID.randomUUID()
            Payments.insert {
                it[id] = paymentId
                it[Payments.chitId] = chitId
                it[Payments.memberId] = memberId
                it[amount] = monthlyContribution // ₹16,666.67 in paisa
                it[Payments.month] = currentActiveMonth
                it[status] = if (memberIds.indexOf(memberId) < 8) PaymentStatusDb.PAID else PaymentStatusDb.PENDING
                it[paidAt] = if (memberIds.indexOf(memberId) < 8) LocalDateTime.now().minusDays(5) else null
                it[createdAt] = LocalDateTime.parse("${currentActiveMonth}-01T09:00:00")
            }
        }
        
        // Add payout records for completed months (first 6 months)
        val payoutMonths = listOf("2025-02", "2025-03", "2025-04", "2025-05", "2025-06", "2025-07")
        payoutMonths.forEachIndexed { index, month ->
            val recipientId = memberIds[index] // Different member gets payout each month
            val payoutId = UUID.randomUUID()
            Payouts.insert {
                it[Payouts.id] = payoutId
                it[Payouts.chitId] = chitId
                it[Payouts.recipientId] = recipientId
                it[Payouts.amount] = fundAmount // Full fund amount as payout
                it[Payouts.month] = month
                it[Payouts.status] = PayoutStatusDb.PAID
                it[Payouts.paidAt] = LocalDateTime.parse("${month}-25T14:00:00")
                it[Payouts.createdAt] = LocalDateTime.parse("${month}-20T12:00:00")
            }
        }
        
        println("Created ACTIVE chit: Fund ₹${fundAmount/100000000000}L, Monthly ₹${monthlyContribution/100}, Current month: $currentActiveMonth")
    }
    
    private fun createClosedChit(moderatorId: UUID, memberIds: List<UUID>) {
        val chitId = UUID.randomUUID()
        val memberCount = 10
        val tenure = 15 // months - completed
        val fundAmount = 300000000000L // ₹3L in paisa (3 × 100,000,000,000)
        val monthlyContribution = fundAmount / (memberCount * tenure) // ₹20,000 per member per month
        
        // Started and completed in 2024
        val startMonth = "2023-10"
        val endMonth = "2024-12" // Calculate: 2023-10 + 15 months = 2024-12
        
        Chits.insert {
            it[id] = chitId
            it[name] = "Vacation Planning Fund"
            it[Chits.fundAmount] = fundAmount
            it[Chits.tenure] = tenure
            it[Chits.memberCount] = memberCount
            it[Chits.startMonth] = startMonth
            it[Chits.endMonth] = endMonth
            it[payoutMethod] = PayoutMethodDb.RANDOM
            it[Chits.moderatorId] = moderatorId
            it[status] = ChitStatusDb.CLOSED
            it[createdAt] = LocalDateTime.now().minusMonths(18)
        }
        
        // Add all members
        memberIds.take(memberCount).forEach { memberId ->
            ChitMembers.insert {
                it[ChitMembers.chitId] = chitId
                it[ChitMembers.userId] = memberId
                it[ChitMembers.status] = MemberStatusDb.APPROVED
                it[ChitMembers.joinedAt] = LocalDateTime.now().minusMonths(18).plusDays(memberIds.indexOf(memberId).toLong())
            }
        }
        
        // Add payment records for all 15 months (completed)
        val allMonths = listOf(
            "2023-10", "2023-11", "2023-12",
            "2024-01", "2024-02", "2024-03", "2024-04", "2024-05", "2024-06",
            "2024-07", "2024-08", "2024-09", "2024-10", "2024-11", "2024-12"
        )
        
        memberIds.take(memberCount).forEach { memberId ->
            allMonths.forEach { month ->
                val paymentId = UUID.randomUUID()
                Payments.insert {
                    it[id] = paymentId
                    it[Payments.chitId] = chitId
                    it[Payments.memberId] = memberId
                    it[amount] = monthlyContribution // ₹20,000 in paisa
                    it[Payments.month] = month
                    it[status] = PaymentStatusDb.PAID
                    it[paidAt] = LocalDateTime.parse("${month}-15T10:00:00")
                    it[createdAt] = LocalDateTime.parse("${month}-01T09:00:00")
                }
            }
        }
        
        // Add payout records for all 15 months (completed)
        allMonths.forEachIndexed { index, month ->
            val recipientId = memberIds[index % memberCount] // Cycle through members
            val payoutId = UUID.randomUUID()
            Payouts.insert {
                it[Payouts.id] = payoutId
                it[Payouts.chitId] = chitId
                it[Payouts.recipientId] = recipientId
                it[Payouts.amount] = fundAmount // Full fund amount as payout
                it[Payouts.month] = month
                it[Payouts.status] = PayoutStatusDb.PAID
                it[Payouts.paidAt] = LocalDateTime.parse("${month}-25T14:00:00")
                it[Payouts.createdAt] = LocalDateTime.parse("${month}-20T12:00:00")
            }
        }
        
        println("Created CLOSED chit: Fund ₹${fundAmount/100000000000}L, Monthly ₹${monthlyContribution/100}, Completed: $endMonth")
    }
}