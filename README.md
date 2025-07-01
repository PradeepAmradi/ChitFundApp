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
- JDK 8 or higher
- Android SDK (for Android development)
- PostgreSQL (for backend)

### Building the Project

```bash
# Build all modules
./gradlew build

# Build Android app
./gradlew :androidApp:assembleDebug

# Build backend
./gradlew :backend:build
```

### Running the Backend

```bash
# Using Docker Compose
docker-compose up

# Or run directly
./gradlew :backend:run
```

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