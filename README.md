# 💰 Chit Fund Management App

A scalable, full-stack **Chit Fund Management App** built with Kotlin Multiplatform to digitize the traditional chit system for both **moderators** and **members**.

## 🏗️ Project Structure

```
chitfund-app/
├── androidApp/            # Android UI & platform code
├── shared/                # Shared business logic (Kotlin Multiplatform)
│   ├── data/              # Models, repositories, APIs
│   ├── domain/            # UseCases & core business rules
│   └── utils/             # Validators, date utils, etc.
├── backend/               # Ktor APIs
│   ├── routes/            # REST endpoints
│   ├── services/          # Business logic
│   ├── db/                # Database schema & migrations
│   └── plugins/           # Ktor plugins configuration
├── .github/workflows/     # CI/CD Pipelines
└── README.md
```

## 🚀 Features

### Core Features
- **Authentication**: Email/Mobile with OTP verification
- **Chit Management**: Create, configure, and manage chits
- **Member Management**: Invite, join, and manage chit members
- **Payment Tracking**: Monthly payment tracking and payout management
- **Multi-Role Support**: Users can be both moderators and members

### Technical Features
- **Kotlin Multiplatform**: Shared business logic across platforms
- **Android Native**: Jetpack Compose UI
- **Backend API**: Ktor-based REST API
- **Database**: PostgreSQL with Exposed ORM
- **CI/CD**: GitHub Actions for automated builds

## 🔧 Getting Started

### Prerequisites
- JDK 17 or higher
- PostgreSQL (for backend)
- Android SDK (for Android development)

### Building the Project

```bash
# Build all modules including Android
./gradlew build

# Build Android debug APK
./gradlew :androidApp:assembleDebug

# Build Android release APK
./gradlew :androidApp:assembleRelease

# Build backend only
./gradlew :shared:build :backend:build

# Run all tests
./gradlew test
```

### Running the Backend

#### Using Gradle
```bash
./gradlew :backend:run
```

#### Using Docker Compose
```bash
docker-compose up
```

#### Manual Setup
```bash
# Set up PostgreSQL database
createdb chitfund

# Set environment variable (optional, defaults to localhost)
export DATABASE_URL="jdbc:postgresql://localhost:5432/chitfund"

# Run the backend
./gradlew :backend:run
```

### Testing the API

The backend provides a complete REST API. Here are some example requests:

```bash
# Test basic connectivity
curl http://localhost:8080

# Initiate login (check server logs for OTP)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'

# Verify OTP (use OTP from server logs)
curl -X POST http://localhost:8080/api/v1/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "otp": "123456"}'

# Create a chit
curl -X POST http://localhost:8080/api/v1/chits \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Family Savings",
    "fundAmount": 100000000000,
    "tenure": 12,
    "memberCount": 10,
    "startMonth": "2024-02",
    "payoutMethod": "RANDOM"
  }'
```

See `IMPLEMENTATION_STATUS.md` for complete API documentation and testing instructions.

## 🧱 Architecture

- **Shared Module**: Contains data models and business logic
- **Android App**: Jetpack Compose UI with MVVM architecture
- **Backend**: Ktor server with RESTful APIs
- **Database**: PostgreSQL with Exposed ORM

## 📝 API Endpoints

### Authentication
- `POST /api/v1/auth/login` - Login with email/mobile
- `POST /api/v1/auth/verify-otp` - Verify OTP

### Chits
- `GET /api/v1/chits` - Get user's chits
- `POST /api/v1/chits` - Create new chit
- `GET /api/v1/chits/{id}` - Get chit details
- `POST /api/v1/chits/{id}/invite` - Invite member
- `POST /api/v1/chits/{id}/join` - Join chit

### Users
- `GET /api/v1/users/profile` - Get user profile

## 🔮 Future Enhancements

- Web app with Compose Multiplatform
- iOS app
- Desktop applications (macOS/Windows)
- Advanced payment integrations
- Real-time notifications
- Analytics and reporting

## 🤝 Contributing

This project follows the standard Git workflow. Please create feature branches and submit pull requests for review.

## 📄 License

This project is licensed under the MIT License.