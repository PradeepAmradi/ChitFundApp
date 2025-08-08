package com.chitfund.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import com.chitfund.shared.data.*

fun Application.configureValidation() {
    install(RequestValidation) {
        validate<LoginRequest> { request ->
            when {
                request.email.isNullOrBlank() && request.mobile.isNullOrBlank() -> 
                    ValidationResult.Invalid("Email or mobile is required")
                request.email != null && !isValidEmail(request.email!!) -> 
                    ValidationResult.Invalid("Invalid email format")
                request.email != null && request.email!!.length > 255 -> 
                    ValidationResult.Invalid("Email too long")
                request.email != null && containsSqlInjectionPattern(request.email!!) -> 
                    ValidationResult.Invalid("Invalid characters in email")
                request.mobile != null && !isValidMobile(request.mobile!!) -> 
                    ValidationResult.Invalid("Invalid mobile number format")
                request.mobile != null && containsSqlInjectionPattern(request.mobile!!) -> 
                    ValidationResult.Invalid("Invalid characters in mobile")
                else -> ValidationResult.Valid
            }
        }
        
        validate<VerifyOtpRequest> { request ->
            when {
                request.email.isNullOrBlank() && request.mobile.isNullOrBlank() -> 
                    ValidationResult.Invalid("Email or mobile is required")
                request.otp.isBlank() -> 
                    ValidationResult.Invalid("OTP is required")
                request.otp.length != 6 -> 
                    ValidationResult.Invalid("OTP must be 6 digits")
                !request.otp.all { it.isDigit() } -> 
                    ValidationResult.Invalid("OTP must contain only digits")
                request.email != null && !isValidEmail(request.email!!) -> 
                    ValidationResult.Invalid("Invalid email format")
                request.mobile != null && !isValidMobile(request.mobile!!) -> 
                    ValidationResult.Invalid("Invalid mobile number format")
                else -> ValidationResult.Valid
            }
        }
        
        validate<CreateChitRequest> { request ->
            when {
                request.name.isBlank() -> 
                    ValidationResult.Invalid("Chit name is required")
                request.name.length > 100 -> 
                    ValidationResult.Invalid("Name too long (max 100 characters)")
                containsXssPattern(request.name) -> 
                    ValidationResult.Invalid("Invalid characters in name")
                containsSqlInjectionPattern(request.name) -> 
                    ValidationResult.Invalid("Invalid characters in name")
                request.fundAmount < 10000000L -> // 1 Lakh in paise
                    ValidationResult.Invalid("Minimum amount is ₹1 Lakh")
                request.fundAmount > 5000000000L -> // 50 Lakh in paise  
                    ValidationResult.Invalid("Maximum amount is ₹50 Lakh")
                request.tenure < 6 -> 
                    ValidationResult.Invalid("Minimum tenure is 6 months")
                request.tenure > 120 -> 
                    ValidationResult.Invalid("Maximum tenure is 120 months")
                request.memberCount < 5 -> 
                    ValidationResult.Invalid("Minimum 5 members required")
                request.memberCount > 50 -> 
                    ValidationResult.Invalid("Maximum 50 members allowed")
                request.startMonth.isBlank() ->
                    ValidationResult.Invalid("Start month is required")
                else -> ValidationResult.Valid
            }
        }
    }
}

private fun isValidEmail(email: String): Boolean {
    return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
}

private fun isValidMobile(mobile: String): Boolean {
    // Indian mobile number validation (10 digits starting with 6-9)
    return mobile.matches(Regex("^[6-9]\\d{9}$"))
}

private fun containsSqlInjectionPattern(input: String): Boolean {
    val sqlPatterns = listOf(
        "'", "\"", ";", "--", "/*", "*/", "xp_", "sp_", 
        "DROP", "SELECT", "INSERT", "UPDATE", "DELETE", "UNION",
        "CREATE", "ALTER", "EXEC", "EXECUTE"
    )
    val upperInput = input.uppercase()
    return sqlPatterns.any { pattern -> upperInput.contains(pattern.uppercase()) }
}

private fun containsXssPattern(input: String): Boolean {
    val xssPatterns = listOf(
        "<script", "</script>", "<iframe", "javascript:", "onload=", 
        "onerror=", "onclick=", "onmouseover=", "<object", "<embed"
    )
    val lowerInput = input.lowercase()
    return xssPatterns.any { pattern -> lowerInput.contains(pattern.lowercase()) }
}