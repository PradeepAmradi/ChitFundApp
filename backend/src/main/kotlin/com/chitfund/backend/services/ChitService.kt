package com.chitfund.backend.services

import com.chitfund.shared.data.*
import com.chitfund.shared.utils.Result
import com.chitfund.backend.db.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

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
                    createdAt = java.time.LocalDateTime.now().toString()
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