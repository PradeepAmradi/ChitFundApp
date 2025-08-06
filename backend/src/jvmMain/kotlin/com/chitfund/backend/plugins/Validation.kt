package com.chitfund.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import com.chitfund.shared.data.*

fun Application.configureValidation() {
    install(RequestValidation) {
        // Login request validation
        validate<LoginRequest> { request ->
            val email = request.email
            val mobile = request.mobile
            when {
                email.isNullOrBlank() && mobile.isNullOrBlank() -> 
                    ValidationResult.Invalid("Either email or mobile is required")
                email != null && !isValidEmail(email) -> 
                    ValidationResult.Invalid("Invalid email format")
                email != null && email.length > 255 -> 
                    ValidationResult.Invalid("Email too long")
                email != null && containsSqlInjectionPattern(email) -> 
                    ValidationResult.Invalid("Invalid characters in email")
                mobile != null && !isValidMobile(mobile) -> 
                    ValidationResult.Invalid("Invalid mobile format")
                mobile != null && containsSqlInjectionPattern(mobile) -> 
                    ValidationResult.Invalid("Invalid characters in mobile")
                else -> ValidationResult.Valid
            }
        }
        
        // OTP verification validation
        validate<VerifyOtpRequest> { request ->
            val email = request.email
            val mobile = request.mobile
            when {
                email.isNullOrBlank() && mobile.isNullOrBlank() -> 
                    ValidationResult.Invalid("Either email or mobile is required")
                request.otp.isBlank() -> 
                    ValidationResult.Invalid("OTP is required")
                request.otp.length != 6 -> 
                    ValidationResult.Invalid("OTP must be 6 digits")
                !request.otp.all { it.isDigit() } -> 
                    ValidationResult.Invalid("OTP must contain only numbers")
                email != null && !isValidEmail(email) -> 
                    ValidationResult.Invalid("Invalid email format")
                mobile != null && !isValidMobile(mobile) -> 
                    ValidationResult.Invalid("Invalid mobile format")
                containsXssPattern(request.otp) -> 
                    ValidationResult.Invalid("Invalid characters in OTP")
                else -> ValidationResult.Valid
            }
        }
        
        // Chit creation validation
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
                request.fundAmount < 1000000000L -> // ₹10,000 in paisa
                    ValidationResult.Invalid("Minimum fund amount is ₹10,000")
                request.fundAmount > 2500000000000L -> // ₹25 Lakh in paisa
                    ValidationResult.Invalid("Maximum fund amount is ₹25 Lakh")
                request.tenure < 12 || request.tenure > 24 -> 
                    ValidationResult.Invalid("Tenure must be between 12-24 months")
                request.memberCount < 10 || request.memberCount > 25 -> 
                    ValidationResult.Invalid("Member count must be between 10-25")
                request.memberCount % 5 != 0 -> 
                    ValidationResult.Invalid("Member count must be in multiples of 5")
                !isValidMonthFormat(request.startMonth) -> 
                    ValidationResult.Invalid("Invalid start month format (use YYYY-MM)")
                else -> ValidationResult.Valid
            }
        }
        
        // Member invitation validation
        validate<InviteMemberRequest> { request ->
            val email = request.email
            val mobile = request.mobile
            when {
                email.isNullOrBlank() && mobile.isNullOrBlank() -> 
                    ValidationResult.Invalid("Either email or mobile is required")
                request.chitId.isBlank() -> 
                    ValidationResult.Invalid("Chit ID is required")
                request.chitId.length > 50 -> 
                    ValidationResult.Invalid("Invalid chit ID")
                email != null && !isValidEmail(email) -> 
                    ValidationResult.Invalid("Invalid email format")
                mobile != null && !isValidMobile(mobile) -> 
                    ValidationResult.Invalid("Invalid mobile format")
                containsXssPattern(request.chitId) || containsSqlInjectionPattern(request.chitId) -> 
                    ValidationResult.Invalid("Invalid characters in chit ID")
                else -> ValidationResult.Valid
            }
        }
        
        // Join chit validation
        validate<JoinChitRequest> { request ->
            when {
                request.chitId.isBlank() -> 
                    ValidationResult.Invalid("Chit ID is required")
                request.chitId.length > 50 -> 
                    ValidationResult.Invalid("Invalid chit ID")
                containsXssPattern(request.chitId) || containsSqlInjectionPattern(request.chitId) -> 
                    ValidationResult.Invalid("Invalid characters in chit ID")
                else -> ValidationResult.Valid
            }
        }
    }
}

// Email validation using regex
private fun isValidEmail(email: String): Boolean {
    return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
}

// Mobile validation (Indian format)
private fun isValidMobile(mobile: String): Boolean {
    // Remove country code and spaces/dashes
    val cleanMobile = mobile.replace("+91", "").replace("-", "").replace(" ", "")
    return cleanMobile.matches(Regex("^[6-9]\\d{9}$"))
}

// Month format validation (YYYY-MM)
private fun isValidMonthFormat(month: String): Boolean {
    return month.matches(Regex("^\\d{4}-(0[1-9]|1[0-2])$"))
}

// SQL injection pattern detection
private fun containsSqlInjectionPattern(input: String): Boolean {
    val sqlPatterns = listOf(
        "'", "\"", ";", "--", "/*", "*/", "xp_", "sp_", 
        "DROP", "SELECT", "INSERT", "UPDATE", "DELETE", "UNION", 
        "OR 1=1", "AND 1=1", "EXEC", "EXECUTE", "SCRIPT"
    )
    val upperInput = input.uppercase()
    return sqlPatterns.any { pattern -> upperInput.contains(pattern.uppercase()) }
}

// XSS pattern detection
private fun containsXssPattern(input: String): Boolean {
    val xssPatterns = listOf(
        "<script", "</script>", "<iframe", "javascript:", "onload=", "onerror=", 
        "onclick=", "onmouseover=", "<img", "src=javascript:", "vbscript:",
        "expression(", "eval(", "alert(", "document.cookie"
    )
    val lowerInput = input.lowercase()
    return xssPatterns.any { pattern -> lowerInput.contains(pattern.lowercase()) }
}