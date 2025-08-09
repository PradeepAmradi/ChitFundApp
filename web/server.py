#!/usr/bin/env python3
"""
Simple HTTP server for serving the ChitFund web application.
This is for development purposes only.
"""

import http.server
import socketserver
import os
import sys
import json
import time
from pathlib import Path

# Configuration
PORT = 8080  # Changed to match backend API port
DIRECTORY = Path(__file__).parent

class MyHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)
    
    def end_headers(self):
        # Add CORS headers for development
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        super().end_headers()
    
    def do_OPTIONS(self):
        # Handle preflight requests
        self.send_response(200)
        self.end_headers()
    
    def do_POST(self):
        # Handle API v1 POST endpoints - redirect to mock data mode
        if self.path.startswith('/api/v1/'):
            self.send_response(503)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            error_data = {
                "success": False,
                "message": "Backend API server is not running. Please start the Ktor backend or use mock data mode.",
                "error": "SERVICE_UNAVAILABLE",
                "suggestion": "Switch to Mock Data mode or start the backend with: ./gradlew :backend:run"
            }
            self.wfile.write(json.dumps(error_data).encode())
            return
        
        # Default handling for other POST requests
        self.send_response(404)
        self.end_headers()
    
    def do_GET(self):
        # Handle health endpoint
        if self.path == '/health':
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            health_data = {
                "status": "healthy",
                "service": "ChitFund Backend API",
                "version": "1.0",
                "timestamp": int(time.time() * 1000)
            }
            self.wfile.write(json.dumps(health_data).encode())
            return
        
        # Handle API info endpoint  
        if self.path == '/api':
            self.send_response(200)
            self.send_header('Content-Type', 'text/plain')
            self.end_headers()
            self.wfile.write(b"Chit Fund Backend API - Version 1.0")
            return
        
        # Handle API v1 endpoints - redirect to mock data mode
        if self.path.startswith('/api/v1/'):
            self.send_response(503)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            error_data = {
                "success": False,
                "message": "Backend API server is not running. Please start the Ktor backend or use mock data mode.",
                "error": "SERVICE_UNAVAILABLE",
                "suggestion": "Switch to Mock Data mode or start the backend with: ./gradlew :backend:run"
            }
            self.wfile.write(json.dumps(error_data).encode())
            return
            
        # Default file serving
        super().do_GET()

def main():
    print(f"Starting ChitFund Backend Server on port {PORT}")
    print(f"Serving files from: {DIRECTORY}")
    print(f"Backend API endpoints:")
    print(f"  - GET http://localhost:{PORT}/health")
    print(f"  - GET http://localhost:{PORT}/api") 
    print(f"Open your browser to: http://localhost:3000 (if running web server separately)")
    print("Press Ctrl+C to stop the server")
    
    with socketserver.TCPServer(("", PORT), MyHTTPRequestHandler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nServer stopped.")
            sys.exit(0)

if __name__ == "__main__":
    main()