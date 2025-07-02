#!/bin/bash

echo "🚀 Starting ChitFund Backend API Server..."
echo "📊 Initializing database and services..."

# Start the backend server
cd /home/runner/work/ChitFundApp/ChitFundApp
./gradlew :backend:run &

# Wait a moment for server to start
sleep 3

echo "🔗 Backend API available at: http://localhost:8080"
echo "📱 API endpoints:"
echo "  - POST /api/v1/auth/login"
echo "  - POST /api/v1/auth/verify-otp"
echo "  - GET /api/v1/chits"
echo "  - POST /api/v1/chits"
echo "  - GET /api/v1/users/profile"
echo ""
echo "📱 Test the API:"
echo "curl -X GET http://localhost:8080"

# Keep script running
wait