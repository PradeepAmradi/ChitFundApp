#!/bin/bash

echo "🚀 Starting ChitFund Development Environment"
echo "============================================="

# Start backend API server on port 8080
echo "📡 Starting Backend API Server on port 8080..."
cd web && python3 server.py &
BACKEND_PID=$!

# Wait for backend to start
sleep 2

# Test backend health
echo "🔍 Testing backend health..."
curl -f http://localhost:8080/health > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Backend API is running and healthy!"
else
    echo "❌ Backend API health check failed"
fi

# Start frontend web server on port 3000
echo "🌐 Starting Frontend Web Server on port 3000..."
cd ../
python3 -m http.server 3000 -d web &
FRONTEND_PID=$!

echo ""
echo "🎉 Development environment is ready!"
echo "📱 Frontend: http://localhost:3000"
echo "🔧 Backend API: http://localhost:8080"
echo "🏥 Health Check: http://localhost:8080/health"
echo ""
echo "Press Ctrl+C to stop all servers"

# Function to cleanup on exit
cleanup() {
    echo ""
    echo "🛑 Stopping servers..."
    kill $BACKEND_PID 2>/dev/null
    kill $FRONTEND_PID 2>/dev/null
    echo "✅ All servers stopped"
    exit 0
}

# Trap Ctrl+C
trap cleanup INT

# Wait for user to stop
wait