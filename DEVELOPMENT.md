# ChitFund Management App - Development Guide

## Project Setup

### 1. Clone and Build
```bash
git clone <repository-url>
cd ChitFundApp
./gradlew build
```

### 2. Database Setup
```bash
# Start PostgreSQL with Docker
docker-compose up postgres

# Or install PostgreSQL locally and create database
createdb chitfund
```

### 3. Backend Development
```bash
# Run backend server
./gradlew :backend:run

# Backend will be available at http://localhost:8080
```

### 4. Android Development
```bash
# Build Android APK
./gradlew :androidApp:assembleDebug

# Install on device/emulator
adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

## Architecture Overview

### Shared Module
- **Data Layer**: Models and API interfaces
- **Domain Layer**: Business logic and use cases
- **Utils**: Common utilities and validators

### Android App
- **UI Layer**: Jetpack Compose screens
- **ViewModels**: State management
- **Repository**: Data access abstraction

### Backend
- **Routes**: API endpoints
- **Services**: Business logic
- **Database**: Schema and data access
- **Plugins**: Configuration and middleware

## Development Workflow

1. **Feature Development**: Create feature branches
2. **Testing**: Write unit tests and integration tests
3. **Code Review**: Submit pull requests for review
4. **CI/CD**: GitHub Actions runs tests and builds
5. **Deployment**: Deploy to staging and production

## API Development

### Adding New Endpoints
1. Define request/response models in `shared/data/`
2. Create route handlers in `backend/routes/`
3. Add database schema changes if needed
4. Update API documentation

### Testing APIs
```bash
# Run backend tests
./gradlew :backend:test

# Manual testing with curl
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'
```

## Mobile Development

### Adding New Screens
1. Create Composable functions in `androidApp/ui/`
2. Add ViewModels for state management
3. Update navigation routes
4. Add to main app navigation

### Testing UI
```bash
# Run Android tests
./gradlew :androidApp:testDebugUnitTest
```

## Database Management

### Schema Changes
1. Update table definitions in `backend/db/Schema.kt`
2. Add migration scripts if needed
3. Test with sample data

### Sample Data
```sql
-- Insert test user
INSERT INTO users (id, email, mobile, name) 
VALUES ('test-user-1', 'test@example.com', '+1234567890', 'Test User');

-- Insert test chit
INSERT INTO chits (id, name, fund_amount, tenure, member_count, start_month, end_month, payout_method, moderator_id, status)
VALUES ('test-chit-1', 'Test Chit', 1000000000, 12, 10, '2024-01', '2024-12', 'RANDOM', 'test-user-1', 'OPEN');
```