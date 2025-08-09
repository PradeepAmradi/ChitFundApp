#!/bin/bash

echo "🚀 Starting ChitFund Development Environment"
echo "============================================="

# Start web development server on port 8080
echo "🌐 Starting Web Development Server on port 8080..."
cd web && python3 server.py &
WEB_PID=$!

# Wait for web server to start
sleep 2

# Test web server health
echo "🔍 Testing web server health..."
curl -f http://localhost:8080/health > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Web server is running and healthy!"
else
    echo "❌ Web server health check failed"
fi

# Optional: Start backend API server on port 8081 if available
echo "🔧 Attempting to start Backend API Server on port 8081..."
cd ../
./gradlew :backend:run -Dktor.deployment.port=8081 > backend.log 2>&1 &
BACKEND_PID=$!

# Wait a moment for backend to start (if it can)
sleep 5

# Test backend health (optional)
curl -f http://localhost:8081/health > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Backend API server is also running on port 8081!"
    echo "📱 You can switch to Live Data mode in the app settings"
else
    echo "⚠️  Backend API server not available (this is okay)"
    echo "📱 The app will work in Mock Data mode"
    # Kill the backend process if it's not working
    kill $BACKEND_PID 2>/dev/null
fi

echo ""
echo "🎉 Development environment is ready!"
echo "🌐 Web Application: http://localhost:8080"
echo "📚 Application works in Mock Data mode by default"
echo "⚙️  You can switch between Mock/Live data in the app settings"
echo ""
echo "Press Ctrl+C to stop all servers"

# Function to cleanup on exit
cleanup() {
    echo ""
    echo "🛑 Stopping servers..."
    kill $WEB_PID 2>/dev/null
    kill $BACKEND_PID 2>/dev/null
    echo "✅ All servers stopped"
    exit 0
}

# Trap Ctrl+C
trap cleanup INT

# Wait for user to stop
wait