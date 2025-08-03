package com.chitfund.backend.services

import com.chitfund.shared.data.*
import com.chitfund.shared.utils.Result
import com.chitfund.backend.db.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class UserService {
    
    fun getUserProfile(userId: String): Result<User> {
        return try {
            transaction {
                val userRow = Users.select { Users.id eq UUID.fromString(userId) }.firstOrNull()
                    ?: return@transaction Result.Error("User not found")
                
                val user = User(
                    id = userRow[Users.id].toString(),
                    email = userRow[Users.email],
                    mobile = userRow[Users.mobile],
                    name = userRow[Users.name],
                    isEmailVerified = userRow[Users.isEmailVerified],
                    isMobileVerified = userRow[Users.isMobileVerified],
                    createdAt = userRow[Users.createdAt].toString()
                )
                
                Result.Success(user)
            }
        } catch (e: Exception) {
            Result.Error("Failed to get user profile: ${e.message}")
        }
    }
    
    fun updateUserProfile(userId: String, name: String? = null, email: String? = null, mobile: String? = null): Result<User> {
        return try {
            transaction {
                // Check if user exists
                val existingUser = Users.select { Users.id eq UUID.fromString(userId) }.firstOrNull()
                    ?: return@transaction Result.Error("User not found")
                
                // Update fields if provided
                Users.update({ Users.id eq UUID.fromString(userId) }) {
                    name?.let { name -> it[Users.name] = name }
                    email?.let { email -> it[Users.email] = email }
                    mobile?.let { mobile -> it[Users.mobile] = mobile }
                }
                
                // Get updated user
                getUserProfile(userId)
            }
        } catch (e: Exception) {
            Result.Error("Failed to update user profile: ${e.message}")
        }
    }
}