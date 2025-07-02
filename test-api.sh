#!/bin/bash

echo "🧪 ChitFund API Test Suite"
echo "=========================="

# Test basic API endpoint
echo "📡 Testing basic API endpoint..."
echo "curl -X GET http://localhost:8080"

# Test authentication flow
echo ""
echo "🔐 Testing Authentication Flow..."
echo "1. Initiating login with email:"
echo "curl -X POST http://localhost:8080/api/v1/auth/login \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -d '{\"email\": \"test@example.com\"}'"

echo ""
echo "2. Verifying OTP (check server logs for generated OTP):"
echo "curl -X POST http://localhost:8080/api/v1/auth/verify-otp \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -d '{\"email\": \"test@example.com\", \"otp\": \"123456\"}'"

# Test chit management
echo ""
echo "🏦 Testing Chit Management..."
echo "1. Getting user's chits:"
echo "curl -X GET http://localhost:8080/api/v1/chits"

echo ""
echo "2. Creating a new chit:"
echo "curl -X POST http://localhost:8080/api/v1/chits \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -d '{"
echo "    \"name\": \"Family Savings Chit\","
echo "    \"fundAmount\": 100000000000,"
echo "    \"tenure\": 12,"
echo "    \"memberCount\": 10,"
echo "    \"startMonth\": \"2024-02\","
echo "    \"payoutMethod\": \"RANDOM\""
echo "  }'"

# Test user profile
echo ""
echo "👤 Testing User Profile..."
echo "curl -X GET http://localhost:8080/api/v1/users/profile"

echo ""
echo "🔍 Note: Use the actual OTP printed in server logs for authentication testing."