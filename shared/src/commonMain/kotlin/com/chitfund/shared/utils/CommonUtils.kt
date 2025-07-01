package com.chitfund.shared.utils

import kotlinx.serialization.Serializable

/**
 * Common validation utilities
 */
object Validators {
    
    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }
    
    fun isValidMobile(mobile: String): Boolean {
        val mobileRegex = "^\\+?[1-9]\\d{9,14}$".toRegex()
        return mobile.matches(mobileRegex)
    }
    
    fun isValidChitAmount(amount: Long): Boolean {
        // Amount should be in multiples of ₹1L (10,000,000 paisa) and max ₹50L
        return amount > 0 && 
               amount % 1000000000L == 0L && 
               amount <= 5000000000000L // ₹50L in paisa
    }
    
    fun isValidTenure(tenure: Int): Boolean {
        return tenure in 12..24
    }
    
    fun isValidMemberCount(count: Int): Boolean {
        return count in 10..25 && count % 5 == 0
    }
    
    fun isValidMonthFormat(month: String): Boolean {
        val monthRegex = "^\\d{4}-\\d{2}$".toRegex()
        return month.matches(monthRegex)
    }
}

/**
 * Date utilities
 */
object DateUtils {
    
    fun calculateEndMonth(startMonth: String, tenure: Int): String {
        if (!Validators.isValidMonthFormat(startMonth)) {
            throw IllegalArgumentException("Invalid start month format")
        }
        
        val parts = startMonth.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        
        val totalMonths = (year * 12 + month - 1) + tenure
        val endYear = totalMonths / 12
        val endMonth = (totalMonths % 12) + 1
        
        return "$endYear-${endMonth.toString().padStart(2, '0')}"
    }
    
    fun formatCurrency(amountInPaisa: Long): String {
        val amountInRupees = amountInPaisa / 100.0
        return "₹${String.format("%.2f", amountInRupees)}"
    }
    
    fun formatLargeCurrency(amountInPaisa: Long): String {
        val amountInLakhs = amountInPaisa / 10000000000.0 // Convert to lakhs
        return "₹${String.format("%.1f", amountInLakhs)}L"
    }
}

/**
 * Result wrapper for handling success/error states
 */
@Serializable
sealed class Result<out T> {
    @Serializable
    data class Success<T>(val data: T) : Result<T>()
    
    @Serializable
    data class Error(val message: String, val code: String? = null) : Result<Nothing>()
    
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw Exception(message)
    }
}