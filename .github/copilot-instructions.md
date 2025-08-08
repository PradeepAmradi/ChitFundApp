# ChitFund Management App - Copilot Instructions

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Project Overview

ChitFund Management App is a Kotlin Multiplatform application for digitizing traditional chit fund systems. It consists of:
- **Backend**: Ktor server with PostgreSQL database
- **Android**: Jetpack Compose mobile application  
- **Web**: HTML/CSS/JavaScript frontend with Python development server
- **Shared**: Kotlin Multiplatform business logic module

## Prerequisites and Environment Setup

Install these exact prerequisites before working with the codebase:
- **JDK 17** (required) - `java -version` should show OpenJDK 17
- **Android SDK** (for Android builds) - Android SDK 34, min SDK 24
- **Python 3** (for web dev server) - `python3 --version` should show Python 3.x
- **PostgreSQL** (for backend database) - Optional for development, required for production
- **Docker** (for containerized deployment) - `docker --version` should work

## Build Commands (NEVER CANCEL - Set 10+ Minute Timeouts)

### Primary Build Commands
```bash
# Build all modules (shared, backend, Android) - NEVER CANCEL: Takes 6 minutes on first run
./gradlew build
# Timeout: Set to 10+ minutes for first build, 3+ minutes for subsequent builds

# Run all tests - Takes under 2 seconds after initial build
./gradlew test
# Timeout: Set to 5+ minutes for safety

# Build backend deployable JAR - Takes under 2 seconds after initial build
./gradlew :backend:buildFatJar
# Timeout: Set to 5+ minutes for safety

# Build Android debug APK - Takes under 2 seconds after initial build
./gradlew :androidApp:assembleDebug
# Timeout: Set to 5+ minutes for safety

# Build Android release APK - Takes under 2 seconds after initial build  
./gradlew :androidApp:assembleRelease
# Timeout: Set to 5+ minutes for safety
```

### Validation Commands
```bash
# Run Android lint - Takes under 2 seconds after initial build
./gradlew lint
# Timeout: Set to 5+ minutes for safety

# Check all modules - Takes under 2 seconds after initial build
./gradlew check
# Timeout: Set to 5+ minutes for safety
```

**CRITICAL TIMING NOTES:**
- **NEVER CANCEL** any Gradle build command - First build takes 6+ minutes
- Subsequent builds are much faster (under 2 minutes) due to Gradle caching
- Set timeouts to at least 10 minutes for initial builds, 5 minutes for subsequent builds
- Build artifacts: `backend/build/libs/chitfund-backend.jar` (25MB fat JAR)
- APK artifacts: `androidApp/build/outputs/apk/debug/androidApp-debug.apk` (11MB)

## Running the Application

### Backend Server
```bash
# Start Ktor backend server on port 8080 - Takes 10-15 seconds to start
./gradlew :backend:run
# Backend available at: http://localhost:8080
# Health check: curl http://localhost:8080/api
```

### Web Application (Development Mode)
```bash
# Start Python development server on port 8080
cd web
python3 server.py

# Endpoints available:
# - GET http://localhost:8080/health - Health check endpoint
# - GET http://localhost:8080/api - API info endpoint  
# - GET http://localhost:8080/index.html - Main web application
```

### Full Development Environment
```bash
# Use the development startup script (starts both backend and web)
./start-development.sh
# Starts backend on port 8080 and web server on port 3000
```

### Docker Deployment
```bash
# Build and run with Docker Compose (includes PostgreSQL)
docker compose up
# Backend: http://localhost:8080
# Database: PostgreSQL on port 5432
```

## API Testing and Validation

### Manual API Testing
```bash
# Test basic connectivity
curl http://localhost:8080/api

# Test authentication flow
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'

# Check server logs for OTP (usually "123456" in development)
# Then verify OTP:
curl -X POST http://localhost:8080/api/v1/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "otp": "123456"}'

# Test chit operations (requires database)
curl -X POST http://localhost:8080/api/v1/chits \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Chit",
    "fundAmount": 100000000000,
    "tenure": 12, 
    "memberCount": 10,
    "startMonth": "2024-02",
    "payoutMethod": "RANDOM"
  }'
```

### Interactive API Testing
Use the built-in API documentation interface:
1. Start backend: `./gradlew :backend:run`
2. Start web server: `cd web && python3 server.py`
3. Open browser: `http://localhost:8080/api-docs.html`

## Validation Scenarios

### ALWAYS Run These Validation Steps After Changes:
1. **Build Validation**: Run `./gradlew build` and ensure it completes without errors
2. **Test Validation**: Run `./gradlew test` and ensure all tests pass
3. **Lint Validation**: Run `./gradlew lint` before committing changes
4. **Backend Functionality**: Start backend and test at least one API endpoint
5. **Android Build**: If touching shared or Android code, run `./gradlew :androidApp:assembleDebug`

### End-to-End Testing Scenarios:
1. **Authentication Flow**: Test login → OTP generation → OTP verification
2. **API Connectivity**: Test basic endpoints return expected responses
3. **Build Artifacts**: Verify JAR and APK files are generated correctly
4. **Web Interface**: Verify web application loads and basic navigation works

## Project Structure and Key Locations

```
ChitFundApp/
├── shared/                    # Kotlin Multiplatform shared code
│   ├── src/commonMain/kotlin/ # Shared business logic
│   └── build.gradle.kts       # Shared module configuration
├── backend/                   # Ktor backend server
│   ├── src/main/kotlin/       # Backend implementation
│   ├── build.gradle.kts       # Backend configuration
│   └── build/libs/            # Generated JAR artifacts
├── androidApp/                # Android Jetpack Compose app
│   ├── src/main/kotlin/       # Android implementation
│   ├── build.gradle.kts       # Android configuration
│   └── build/outputs/apk/     # Generated APK artifacts
├── web/                       # Web frontend
│   ├── index.html             # Main web page
│   ├── server.py             # Development server
│   └── api-docs.html         # API testing interface
├── .github/workflows/         # CI/CD pipelines
│   ├── android-ci.yml        # CI build workflow
│   └── azure-deploy.yml      # Azure deployment workflow
├── gradle/libs.versions.toml  # Dependency versions
├── build.gradle.kts          # Root project configuration
├── settings.gradle.kts       # Project modules
├── docker-compose.yml        # Docker development setup
└── Dockerfile               # Backend container definition
```

### Frequently Modified Files:
- **API Routes**: `backend/src/main/kotlin/com/chitfund/backend/routes/`
- **Data Models**: `shared/src/commonMain/kotlin/data/`
- **Android UI**: `androidApp/src/main/kotlin/`
- **Web Frontend**: `web/js/` and `web/css/`

## Common Development Tasks

### Adding New API Endpoints:
1. Define models in `shared/src/commonMain/kotlin/data/`
2. Add routes in `backend/src/main/kotlin/com/chitfund/backend/routes/`
3. Test with `curl` or the API documentation interface
4. Always run `./gradlew :backend:test` after changes

### Modifying Android UI:
1. Update Compose screens in `androidApp/src/main/kotlin/`
2. Build and test: `./gradlew :androidApp:assembleDebug`
3. Install on device/emulator if needed

### Database Changes:
1. Modify schema in `backend/src/main/kotlin/db/`
2. Update Docker Compose for development: `docker compose up postgres`
3. Test with real database connection

### Web Frontend Changes:
1. Modify HTML/CSS/JS in `web/` directory
2. Start development server: `cd web && python3 server.py`
3. Test in browser at `http://localhost:8080`

## CI/CD Pipeline Information

### GitHub Actions Workflows:
- **CI Build** (`android-ci.yml`): Runs on push to main/develop branches
  - Builds all modules (6+ minutes)
  - Runs tests (under 2 minutes)
  - Creates APK and JAR artifacts
- **Azure Deploy** (`azure-deploy.yml`): Deploys to Azure on main branch changes
  - Builds Docker image
  - Deploys to Azure Web App

### Build Artifacts:
- **Backend JAR**: `backend/build/libs/chitfund-backend.jar` (~25MB)
- **Android Debug APK**: `androidApp/build/outputs/apk/debug/androidApp-debug.apk` (~11MB)
- **Android Release APK**: `androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk` (~8MB)

## Troubleshooting

### Common Issues:
1. **Build fails with "JDK not found"**: Ensure JDK 17 is installed and JAVA_HOME is set
2. **Android build fails**: Ensure Android SDK is installed and ANDROID_HOME is set
3. **Backend database errors**: Use Docker Compose to start PostgreSQL or work in mock mode
4. **Gradle daemon issues**: Run `./gradlew --stop` then retry build command
5. **Permission denied on scripts**: Run `chmod +x script-name.sh`

### Performance Tips:
- Use `./gradlew build --build-cache` for faster subsequent builds
- Run `./gradlew clean` if seeing strange build issues
- Use `./gradlew --offline` when working without internet

### Live Environments:
- **Production Backend**: https://chitfund-webapp.azurewebsites.net
- **Azure Container Registry**: chitfundacr.azurecr.io
- **Resource Group**: chitfund-rg (Central India)

## Version Information

Key dependency versions (see `gradle/libs.versions.toml`):
- **Kotlin**: 1.9.20
- **Ktor**: 2.3.4  
- **Compose**: 1.5.4
- **Android Gradle Plugin**: 8.0.2
- **PostgreSQL**: 42.6.0
- **Exposed ORM**: 0.44.1

ALWAYS validate your changes by running the complete build and test suite before committing. Use the timing guidelines above to set appropriate timeouts and never cancel long-running builds.