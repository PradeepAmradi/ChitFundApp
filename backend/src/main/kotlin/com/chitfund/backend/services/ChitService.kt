package com.chitfund.backend.services

import com.chitfund.shared.data.*
import com.chitfund.shared.utils.Result
import com.chitfund.backend.db.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.time.LocalDateTime

class ChitService {
    
    fun createChit(request: CreateChitRequest, moderatorId: String): Result<Chit> {
        return try {
            transaction {
                val chitId = UUID.randomUUID()
                val endMonth = calculateEndMonth(request.startMonth, request.tenure)
                
                Chits.insert {
                    it[id] = chitId
                    it[name] = request.name
                    it[fundAmount] = request.fundAmount
                    it[tenure] = request.tenure
                    it[memberCount] = request.memberCount
                    it[startMonth] = request.startMonth
                    it[this.endMonth] = endMonth
                    it[payoutMethod] = when (request.payoutMethod) {
                        PayoutMethod.RANDOM -> PayoutMethodDb.RANDOM
                        PayoutMethod.VOTING -> PayoutMethodDb.VOTING
                    }
                    it[this.moderatorId] = UUID.fromString(moderatorId)
                    it[status] = ChitStatusDb.OPEN
                }
                
                val chit = Chit(
                    id = chitId.toString(),
                    name = request.name,
                    fundAmount = request.fundAmount,
                    tenure = request.tenure,
                    memberCount = request.memberCount,
                    startMonth = request.startMonth,
                    endMonth = endMonth,
                    payoutMethod = request.payoutMethod,
                    moderatorId = moderatorId,
                    status = ChitStatus.OPEN,
                    createdAt = LocalDateTime.now().toString()
                )
                
                Result.Success(chit)
            }
        } catch (e: Exception) {
            Result.Error("Failed to create chit: ${e.message}")
        }
    }
    
    fun getChitsByUser(userId: String): Result<List<Chit>> {
        return try {
            transaction {
                val chits = Chits.select { 
                    (Chits.moderatorId eq UUID.fromString(userId)) or 
                    (Chits.id inSubQuery ChitMembers.select { ChitMembers.userId eq UUID.fromString(userId) }.map { it[ChitMembers.chitId] })
                }.map { row ->
                    Chit(
                        id = row[Chits.id].toString(),
                        name = row[Chits.name],
                        fundAmount = row[Chits.fundAmount],
                        tenure = row[Chits.tenure],
                        memberCount = row[Chits.memberCount],
                        startMonth = row[Chits.startMonth],
                        endMonth = row[Chits.endMonth],
                        payoutMethod = when (row[Chits.payoutMethod]) {
                            PayoutMethodDb.RANDOM -> PayoutMethod.RANDOM
                            PayoutMethodDb.VOTING -> PayoutMethod.VOTING
                        },
                        moderatorId = row[Chits.moderatorId].toString(),
                        status = when (row[Chits.status]) {
                            ChitStatusDb.OPEN -> ChitStatus.OPEN
                            ChitStatusDb.ACTIVE -> ChitStatus.ACTIVE
                            ChitStatusDb.CLOSED -> ChitStatus.CLOSED
                        },
                        createdAt = row[Chits.createdAt].toString()
                    )
                }
                
                Result.Success(chits)
            }
        } catch (e: Exception) {
            Result.Error("Failed to get chits: ${e.message}")
        }
    }
    
    fun getChitById(chitId: String): Result<Chit> {
        return try {
            transaction {
                val row = Chits.select { Chits.id eq UUID.fromString(chitId) }.firstOrNull()
                    ?: return@transaction Result.Error("Chit not found")
                
                val chit = Chit(
                    id = row[Chits.id].toString(),
                    name = row[Chits.name],
                    fundAmount = row[Chits.fundAmount],
                    tenure = row[Chits.tenure],
                    memberCount = row[Chits.memberCount],
                    startMonth = row[Chits.startMonth],
                    endMonth = row[Chits.endMonth],
                    payoutMethod = when (row[Chits.payoutMethod]) {
                        PayoutMethodDb.RANDOM -> PayoutMethod.RANDOM
                        PayoutMethodDb.VOTING -> PayoutMethod.VOTING
                    },
                    moderatorId = row[Chits.moderatorId].toString(),
                    status = when (row[Chits.status]) {
                        ChitStatusDb.OPEN -> ChitStatus.OPEN
                        ChitStatusDb.ACTIVE -> ChitStatus.ACTIVE
                        ChitStatusDb.CLOSED -> ChitStatus.CLOSED
                    },
                    createdAt = row[Chits.createdAt].toString()
                )
                
                Result.Success(chit)
            }
        } catch (e: Exception) {
            Result.Error("Failed to get chit: ${e.message}")
        }
    }
    
    fun inviteMember(chitId: String, request: InviteMemberRequest): Result<String> {
        return try {
            transaction {
                // Verify chit exists and is in OPEN status
                val chit = Chits.select { Chits.id eq UUID.fromString(chitId) }.firstOrNull()
                    ?: return@transaction Result.Error("Chit not found")
                
                if (chit[Chits.status] != ChitStatusDb.OPEN) {
                    return@transaction Result.Error("Chit is not open for new members")
                }
                
                // Check if member limit reached
                val currentMemberCount = ChitMembers.select { ChitMembers.chitId eq UUID.fromString(chitId) }.count()
                if (currentMemberCount >= chit[Chits.memberCount]) {
                    return@transaction Result.Error("Chit member limit reached")
                }
                
                // Find user by email or mobile
                val identifier = request.email ?: request.mobile ?: return@transaction Result.Error("Email or mobile required")
                val user = if (request.email != null) {
                    Users.select { Users.email eq request.email }.firstOrNull()
                } else {
                    Users.select { Users.mobile eq request.mobile!! }.firstOrNull()
                } ?: return@transaction Result.Error("User not found")
                
                val userId = user[Users.id]
                
                // Check if user already invited/joined
                val existingMember = ChitMembers.select { 
                    (ChitMembers.chitId eq UUID.fromString(chitId)) and (ChitMembers.userId eq userId)
                }.firstOrNull()
                
                if (existingMember != null) {
                    return@transaction Result.Error("User already invited to this chit")
                }
                
                // Add member with INVITED status
                ChitMembers.insert {
                    it[this.chitId] = UUID.fromString(chitId)
                    it[this.userId] = userId
                    it[status] = MemberStatusDb.INVITED
                }
                
                Result.Success("Invitation sent successfully")
            }
        } catch (e: Exception) {
            Result.Error("Failed to send invitation: ${e.message}")
        }
    }
    
    fun joinChit(chitId: String, userId: String): Result<String> {
        return try {
            transaction {
                // Check if user was invited
                val invitation = ChitMembers.select {
                    (ChitMembers.chitId eq UUID.fromString(chitId)) and 
                    (ChitMembers.userId eq UUID.fromString(userId))
                }.firstOrNull() ?: return@transaction Result.Error("No invitation found")
                
                when (invitation[ChitMembers.status]) {
                    MemberStatusDb.INVITED -> {
                        // Update status to JOINED
                        ChitMembers.update({
                            (ChitMembers.chitId eq UUID.fromString(chitId)) and 
                            (ChitMembers.userId eq UUID.fromString(userId))
                        }) {
                            it[status] = MemberStatusDb.JOINED
                        }
                        Result.Success("Successfully joined chit")
                    }
                    MemberStatusDb.JOINED -> Result.Error("Already joined this chit")
                    MemberStatusDb.APPROVED -> Result.Error("Already approved for this chit")
                    MemberStatusDb.REJECTED -> Result.Error("Invitation was rejected")
                }
            }
        } catch (e: Exception) {
            Result.Error("Failed to join chit: ${e.message}")
        }
    }
    
    private fun calculateEndMonth(startMonth: String, tenure: Int): String {
        val parts = startMonth.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        
        val endMonth = month + tenure - 1
        val endYear = if (endMonth > 12) year + (endMonth - 1) / 12 else year
        val finalMonth = if (endMonth > 12) ((endMonth - 1) % 12) + 1 else endMonth
        
        return "$endYear-${finalMonth.toString().padStart(2, '0')}"
    }
}