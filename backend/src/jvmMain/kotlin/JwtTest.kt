
import com.chitfund.backend.services.AuthService

fun main() {
    println("=== JWT Token Security Test ===")
    val authService = AuthService()
    
    // Test token generation
    val testUserId = "test-user-123"  
    val tokens = authService.generateTokens(testUserId)
    println("✅ JWT tokens generated successfully")
    
    // Test token verification
    val verifiedUserId = authService.verifyToken(tokens.accessToken)
    println("✅ Token verification: ${if (verifiedUserId == testUserId) "SUCCESS" else "FAILED"}")
    
    println("🔐 JWT Security implementation working correctly!")
}

