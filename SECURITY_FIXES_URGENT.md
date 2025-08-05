# 🔐 URGENT: Security Fixes Required for Production

## ⚠️ **CRITICAL SECURITY ISSUES** - Fix Before Production Launch

### 1. **CORS Configuration Vulnerability** - `IMMEDIATE ACTION REQUIRED`

**File**: `backend/src/jvmMain/kotlin/com/chitfund/backend/plugins/HTTP.kt`

**Current Code (INSECURE)**:
```kotlin
fun Application.configureHTTP() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
        anyHost() // ⚠️ CRITICAL SECURITY VULNERABILITY
    }
}
```

**Required Fix (SECURE)**:
```kotlin
fun Application.configureHTTP() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
        
        // ✅ SECURE: Specify allowed origins
        allowHost("localhost:3000") // Development
        allowHost("chitfund-app.com", schemes = listOf("https"))
        allowHost("app.chitfund.com", schemes = listOf("https"))
        allowHost("chitfund-webapp.azurewebsites.net", schemes = listOf("https"))
    }
}
```

**Risk**: `anyHost()` allows ALL domains to make requests, enabling CSRF attacks and unauthorized access.

---

### 2. **Missing Rate Limiting** - `HIGH PRIORITY`

**Current State**: No rate limiting implemented
**Risk**: API abuse, DDoS attacks, OTP spam

**Required Implementation**:

**File**: `backend/src/jvmMain/kotlin/com/chitfund/backend/plugins/RateLimiting.kt` (NEW FILE)
```kotlin
package com.chitfund.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun Application.configureRateLimit() {
    install(RateLimit) {
        // Global rate limit
        register(RateLimitName("global")) {
            rateLimiter(limit = 1000, refillPeriod = 1.minutes)
        }
        
        // Authentication endpoints (OTP) - stricter limits
        register(RateLimitName("auth")) {
            rateLimiter(limit = 5, refillPeriod = 1.minutes)
        }
        
        // API endpoints - moderate limits
        register(RateLimitName("api")) {
            rateLimiter(limit = 100, refillPeriod = 1.minutes)
        }
    }
}
```

**Update Routes with Rate Limiting**:
```kotlin
// backend/src/jvmMain/kotlin/com/chitfund/backend/routes/AuthRoutes.kt
fun Route.authRoutes() {
    rateLimit(RateLimitName("auth")) {
        route("/auth") {
            post("/login") {
                // ... existing code
            }
            post("/verify-otp") {
                // ... existing code
            }
        }
    }
}
```

---

### 3. **Missing Security Headers** - `HIGH PRIORITY`

**Required Security Headers**:

**File**: `backend/src/jvmMain/kotlin/com/chitfund/backend/plugins/Security.kt` (NEW FILE)
```kotlin
package com.chitfund.backend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.defaultheaders.*

fun Application.configureSecurity() {
    install(DefaultHeaders) {
        // Security headers
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "DENY")
        header("X-XSS-Protection", "1; mode=block")
        header("Referrer-Policy", "strict-origin-when-cross-origin")
        header("Permissions-Policy", "geolocation=(), microphone=(), camera=()")
        
        // Content Security Policy
        header("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' cdnjs.cloudflare.com; " +
            "style-src 'self' 'unsafe-inline' cdnjs.cloudflare.com; " +
            "img-src 'self' data: https:; " +
            "connect-src 'self' chitfund-webapp.azurewebsites.net; " +
            "font-src 'self' cdnjs.cloudflare.com"
        )
        
        // HSTS for HTTPS
        if (environment.config.property("ktor.deployment.ssl").getString() == "true") {
            header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        }
    }
}
```

---

### 4. **Input Validation Hardening** - `HIGH PRIORITY`

**Current State**: Basic validation exists but needs strengthening

**Enhanced Validation Required**:

**File**: `backend/src/jvmMain/kotlin/com/chitfund/backend/plugins/Validation.kt` (NEW FILE)
```kotlin
package com.chitfund.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import com.chitfund.shared.data.*

fun Application.configureValidation() {
    install(RequestValidation) {
        validate<LoginRequest> { request ->
            when {
                request.email.isBlank() -> ValidationResult.Invalid("Email is required")
                !isValidEmail(request.email) -> ValidationResult.Invalid("Invalid email format")
                request.email.length > 255 -> ValidationResult.Invalid("Email too long")
                containsSqlInjectionPattern(request.email) -> ValidationResult.Invalid("Invalid characters")
                else -> ValidationResult.Valid
            }
        }
        
        validate<CreateChitRequest> { request ->
            when {
                request.name.isBlank() -> ValidationResult.Invalid("Chit name is required")
                request.name.length > 100 -> ValidationResult.Invalid("Name too long")
                containsXssPattern(request.name) -> ValidationResult.Invalid("Invalid characters in name")
                request.fundAmount < 10000000000L -> ValidationResult.Invalid("Minimum amount is ₹1 Lakh")
                request.fundAmount > 5000000000000L -> ValidationResult.Invalid("Maximum amount is ₹50 Lakh")
                else -> ValidationResult.Valid
            }
        }
    }
}

private fun isValidEmail(email: String): Boolean {
    return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
}

private fun containsSqlInjectionPattern(input: String): Boolean {
    val sqlPatterns = listOf("'", "\"", ";", "--", "/*", "*/", "xp_", "sp_", "DROP", "SELECT", "INSERT", "UPDATE", "DELETE")
    return sqlPatterns.any { pattern -> input.uppercase().contains(pattern.uppercase()) }
}

private fun containsXssPattern(input: String): Boolean {
    val xssPatterns = listOf("<script", "</script>", "<iframe", "javascript:", "onload=", "onerror=")
    return xssPatterns.any { pattern -> input.lowercase().contains(pattern.lowercase()) }
}
```

---

### 5. **JWT Token Security Enhancement** - `MEDIUM PRIORITY`

**Current Implementation**: Basic JWT tokens
**Improvements Needed**: Token expiration, refresh tokens, secure storage

**Enhanced JWT Configuration**:

**File**: `backend/src/jvmMain/kotlin/com/chitfund/backend/services/AuthService.kt` (UPDATE)
```kotlin
// Add to existing AuthService
class AuthService {
    companion object {
        private const val ACCESS_TOKEN_EXPIRY = 15 * 60 * 1000L // 15 minutes
        private const val REFRESH_TOKEN_EXPIRY = 7 * 24 * 60 * 60 * 1000L // 7 days
    }
    
    fun generateTokens(user: User): TokenPair {
        val accessToken = JWT.create()
            .withSubject(user.id)
            .withClaim("email", user.email)
            .withExpiresAt(Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY))
            .withIssuedAt(Date())
            .sign(algorithm)
            
        val refreshToken = JWT.create()
            .withSubject(user.id)
            .withExpiresAt(Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY))
            .withIssuedAt(Date())
            .sign(algorithm)
            
        return TokenPair(accessToken, refreshToken)
    }
}

data class TokenPair(
    val accessToken: String,
    val refreshToken: String
)
```

---

### 6. **Environment Configuration Security** - `HIGH PRIORITY`

**Current State**: Configuration in plain text
**Required**: Secure environment variable management

**File**: `backend/src/jvmMain/resources/application.conf` (UPDATE)
```hocon
ktor {
    application {
        modules = [ com.chitfund.backend.ApplicationKt.module ]
    }
    
    deployment {
        port = ${?PORT}
        host = 0.0.0.0
        
        # SSL Configuration for production
        ssl {
            keyStore = ${?SSL_KEYSTORE_PATH}
            keyAlias = ${?SSL_KEY_ALIAS}
            keyStorePassword = ${?SSL_KEYSTORE_PASSWORD}
            privateKeyPassword = ${?SSL_PRIVATE_KEY_PASSWORD}
        }
    }
}

# Database configuration
database {
    url = ${?DATABASE_URL}
    user = ${?DATABASE_USER}
    password = ${?DATABASE_PASSWORD}
    maxPoolSize = ${?DATABASE_POOL_SIZE}
}

# JWT configuration
jwt {
    secret = ${?JWT_SECRET}
    issuer = ${?JWT_ISSUER}
    audience = ${?JWT_AUDIENCE}
}

# OTP configuration
otp {
    expiry = ${?OTP_EXPIRY_MINUTES}
    length = ${?OTP_LENGTH}
}
```

**Required Environment Variables**:
```bash
# Production environment variables (NEVER commit these)
JWT_SECRET=<strong-random-secret-256-bits>
DATABASE_URL=<encrypted-database-connection>
DATABASE_USER=<database-username>
DATABASE_PASSWORD=<strong-database-password>
SSL_KEYSTORE_PATH=/path/to/keystore.jks
SSL_KEYSTORE_PASSWORD=<keystore-password>
OTP_EXPIRY_MINUTES=10
```

---

### 7. **Database Security** - `HIGH PRIORITY`

**Required Database Security Measures**:

1. **Connection Security**:
```kotlin
// Use SSL connections
val config = HikariConfig().apply {
    jdbcUrl = "jdbc:postgresql://server:5432/chitfund?sslmode=require"
    username = System.getenv("DATABASE_USER")
    password = System.getenv("DATABASE_PASSWORD")
    isAutoCommit = false
    transactionIsolation = "TRANSACTION_READ_COMMITTED"
    maximumPoolSize = 10
}
```

2. **Prepared Statements** (Already implemented, verify):
```kotlin
// Ensure all queries use prepared statements
// ✅ Good: Using Exposed ORM with prepared statements
Users.select { Users.email eq email }

// ❌ Bad: Raw SQL with string concatenation (avoid)
// "SELECT * FROM users WHERE email = '$email'"
```

---

## 🚨 **IMMEDIATE ACTION PLAN**

### **Day 1**: Critical Security Fixes
1. Fix CORS configuration (30 minutes)
2. Add security headers (1 hour)
3. Deploy to staging for testing (30 minutes)

### **Day 2**: Input Validation & Rate Limiting  
1. Implement enhanced input validation (2 hours)
2. Add rate limiting (2 hours)
3. Test security measures (1 hour)

### **Day 3**: JWT & Environment Security
1. Enhance JWT token security (2 hours)
2. Secure environment configuration (1 hour)
3. Update deployment scripts (1 hour)

### **Day 4**: Testing & Deployment
1. Security testing (2 hours)
2. Production deployment (1 hour)  
3. Monitoring setup (1 hour)

---

## 🔍 **Security Testing Checklist**

Before production deployment, verify:

- [ ] CORS allows only specified domains
- [ ] Rate limiting blocks excessive requests
- [ ] Security headers are present in responses
- [ ] Input validation rejects malicious inputs
- [ ] JWT tokens expire correctly
- [ ] HTTPS is enforced
- [ ] Database connections are encrypted
- [ ] Environment variables are secured
- [ ] No sensitive data in logs
- [ ] Error messages don't leak information

---

## 📊 **Security Monitoring**

Set up monitoring for:
- Failed authentication attempts
- Rate limit violations  
- Suspicious input patterns
- Token expiration issues
- Database connection failures
- SSL certificate expiry

---

**⚠️ CRITICAL**: These security fixes must be implemented before any production launch. The current CORS configuration alone makes the application vulnerable to serious attacks.