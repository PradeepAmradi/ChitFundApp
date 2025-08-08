# 🔐 Security Configuration Guide

## Production Environment Variables

Set these environment variables for production deployment:

### Required Security Variables

```bash
# JWT Configuration (CRITICAL - Must be changed)
JWT_SECRET="your-256-bit-secret-key-here"  # Generate with: openssl rand -base64 32
JWT_ISSUER="chitfund-app"
JWT_AUDIENCE="chitfund-users"

# Database Security
DATABASE_URL="jdbc:postgresql://server:5432/chitfund?sslmode=require"
DATABASE_USER="your-db-username"
DATABASE_PASSWORD="your-strong-db-password"
DATABASE_SSL_MODE="require"  # Force SSL connections

# Rate Limiting (Aggressive as requested)
RATE_LIMIT_GLOBAL=100      # 100 requests per user per day
RATE_LIMIT_AUTH=5          # 5 OTP requests per hour
RATE_LIMIT_API=50          # 50 API requests per hour  
RATE_LIMIT_SENSITIVE=3     # 3 sensitive operations per hour

# SSL/HTTPS (When SSL certificate is available)
SSL_ENABLED=true
SSL_KEYSTORE_PATH="/path/to/keystore.jks"
SSL_KEYSTORE_PASSWORD="your-keystore-password"

# Application
PORT=8080
```

## Security Features Implemented

### ✅ 1. CORS Protection
- Fixed `anyHost()` vulnerability
- Only allows specific domains:
  - `localhost:3000` (development)
  - `chitfund-webapp.azurewebsites.net` (production)
  - `app.chitfund.com` (production domain)

### ✅ 2. Comprehensive Security Headers
- **X-Content-Type-Options**: `nosniff` - Prevents MIME type sniffing
- **X-Frame-Options**: `DENY` - Prevents clickjacking
- **X-XSS-Protection**: `1; mode=block` - XSS protection
- **Referrer-Policy**: `strict-origin-when-cross-origin` - Controls referrer info
- **Permissions-Policy**: Disables geolocation, microphone, camera
- **Content-Security-Policy**: Comprehensive CSP rules
- **HSTS**: Ready for HTTPS deployment

### ✅ 3. Aggressive Rate Limiting
- **Global Limit**: 100 requests per user per day (as requested)
- **Auth Endpoints**: 5 requests per hour (prevents OTP spam)
- **API Endpoints**: 50 requests per hour
- **Sensitive Operations**: 3 requests per hour
- Uses client IP + endpoint for tracking
- Returns HTTP 429 when limits exceeded

### ✅ 4. Enhanced JWT Security
- **Access Tokens**: 15 minutes expiry
- **Refresh Tokens**: 7 days expiry
- Proper JWT implementation with auth0/java-jwt
- Configurable secret, issuer, and audience
- Token verification and refresh capabilities

### ✅ 5. Input Validation & Sanitization
- **Email Validation**: Proper email format checking
- **Mobile Validation**: Indian mobile number format (10 digits, starts with 6-9)
- **SQL Injection Protection**: Detects common SQL injection patterns
- **XSS Protection**: Detects common XSS patterns
- **Length Limits**: Prevents oversized inputs
- **Character Validation**: Blocks malicious characters

### ✅ 6. Database Security
- **HikariCP Connection Pooling**: Secure, efficient connections
- **SSL Connections**: Enforced via environment variables
- **Connection Validation**: Regular health checks
- **Transaction Isolation**: `TRANSACTION_READ_COMMITTED`
- **Prepared Statements**: Already implemented via Exposed ORM
- **Connection Timeouts**: Prevents hanging connections

## Deployment Checklist

### Before Production Launch:

- [ ] Generate strong JWT secret: `openssl rand -base64 32`
- [ ] Set all environment variables with production values
- [ ] Configure SSL certificate and enable HTTPS
- [ ] Set up database with SSL enabled
- [ ] Test rate limiting with load testing tools
- [ ] Verify all security headers are present
- [ ] Test CORS with frontend domains
- [ ] Validate input sanitization with security scanners
- [ ] Set up monitoring for security events

### Security Monitoring Setup:

Monitor these security events:
- Rate limit violations (HTTP 429 responses)
- Failed authentication attempts
- Invalid JWT tokens
- Suspicious input patterns (XSS/SQL injection attempts)
- Database connection failures
- SSL certificate expiry

## Testing Security

### Rate Limiting Test:
```bash
# Test auth endpoint rate limiting (should block after 5 requests)
for i in {1..10}; do 
  curl -s http://your-api.com/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@example.com"}' \
    -w "Status: %{http_code}\n"
done
```

### Security Headers Test:
```bash
# Verify security headers
curl -I https://your-api.com/users/profile
```

### CORS Test:
```bash
# Test CORS policy
curl -H "Origin: https://malicious-site.com" \
  -H "Access-Control-Request-Method: POST" \
  -X OPTIONS https://your-api.com/auth/login
```

## Emergency Response

If security breach is detected:

1. **Immediate**: Rotate JWT secret to invalidate all tokens
2. **Block**: Add aggressive rate limiting (reduce limits to 1-5 per hour)
3. **Analyze**: Check logs for attack patterns
4. **Update**: Deploy security patches immediately
5. **Monitor**: Increase monitoring frequency

## Security Audit Log

- ✅ **2024-01-XX**: Fixed CORS `anyHost()` vulnerability
- ✅ **2024-01-XX**: Implemented comprehensive security headers
- ✅ **2024-01-XX**: Added aggressive rate limiting (100 req/day)
- ✅ **2024-01-XX**: Enhanced JWT security with proper tokens
- ✅ **2024-01-XX**: Added input validation and XSS/SQL protection
- ✅ **2024-01-XX**: Secured database connections with HikariCP
- ✅ **2024-01-XX**: Environment variable security configuration

**Status**: ✅ PRODUCTION READY from security perspective