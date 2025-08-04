# 💰 Chit Fund Management App

A scalable, full-stack **Chit Fund Management App** built with Kotlin Multiplatform to digitize the traditional chit system for both **moderators** and **members**.

## 🚀 Continuous Integration & Deployment Status

[![CI Build](https://github.com/PradeepAmradi/ChitFundApp/actions/workflows/android-ci.yml/badge.svg)](https://github.com/PradeepAmradi/ChitFundApp/actions/workflows/android-ci.yml)
[![Azure Backend Deployment](https://github.com/PradeepAmradi/ChitFundApp/actions/workflows/azure-deploy.yml/badge.svg)](https://github.com/PradeepAmradi/ChitFundApp/actions/workflows/azure-deploy.yml)

### Build & Deployment Pipeline
- **� CI Build**: Automated testing and building for all components (Backend, Android, Shared modules)
- **🏗️ Backend Build**: Kotlin backend compilation, testing, and fat JAR generation
- **� Android Release Build**: APK generation for both debug and release variants
- **🐳 Azure Deployment**: Containerized backend deployment to Azure Web App
- **🧪 Automated Testing**: Unit tests execution across all modules

### CI Build Workflow
The CI pipeline runs on:
- Push to `main` and `develop` branches
- Pull requests to `main` branch
- Builds and tests all project components:
  - **Shared Module**: Kotlin Multiplatform business logic
  - **Backend**: Ktor server with fat JAR generation
  - **Android App**: Debug and release APK builds
  - **Artifacts**: Automatic upload of JARs and APKs

### Workflow Triggers
**CI Build Workflow** runs when:
- Code is pushed to `main` or `develop` branches
- Pull requests are opened against `main` branch
- Builds all components: Backend, Android, and Shared modules

**Azure Deployment Workflow** runs when:
- Code is pushed to the `main` branch
- Changes are made to `backend/`, `shared/`, `Dockerfile`, or workflow files
- Manual trigger via GitHub Actions UI (`workflow_dispatch`)
- Deploys containerized backend to Azure after successful CI build

### Live Environment
- **🌐 Production Backend**: [https://chitfund-webapp.azurewebsites.net](https://chitfund-webapp.azurewebsites.net)
- **📱 Android APKs**: Available as artifacts from successful CI builds
- **🔍 Build Artifacts**: JARs and APKs automatically uploaded and retained

### Infrastructure
- **Azure Container Registry**: `chitfundacr.azurecr.io`
- **Azure Database**: PostgreSQL managed instance
- **Azure Web App**: `chitfund-webapp` (Container-based)
- **Resource Group**: `chitfund-rg` (Central India)

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
├── web/                   # Web application (HTML/CSS/JS)
│   ├── css/               # Stylesheets
│   ├── js/                # JavaScript modules
│   ├── index.html         # Main HTML file
│   └── server.py          # Development server
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
- **Web Application**: Modern HTML/CSS/JavaScript web app
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

### Running the Web Application

The web application provides a modern interface for managing chit funds with features like authentication, dashboard, and chit management.

#### Quick Start (Demo Mode)
```bash
# Navigate to web directory
cd web

# Start the development server
python3 server.py

# Open browser to http://localhost:3000
```

#### With Backend Integration
```bash
# 1. Start the backend server (in project root)
./gradlew :backend:run

# 2. Configure API connection (in web/js/api.js)
# Set useMockData: false to connect to backend

# 3. Start the web server (in web directory)
cd web
python3 server.py

# 4. Open browser to http://localhost:3000
```

#### Web App Features
- **Authentication**: Email-based OTP login system
- **Dashboard**: Overview of chits and statistics  
- **Chit Management**: Create, view, and manage chit funds
- **Member Management**: Invite and manage chit members
- **API Documentation**: Interactive Swagger-like API testing interface (`/api-docs.html`)
- **Responsive Design**: Works on desktop, tablet, and mobile devices
- **Mock Data Mode**: Runs standalone for demo purposes

### Testing the API

The backend provides a complete REST API with an interactive Swagger-like testing interface.

#### 🧪 Interactive API Testing (Swagger-like Interface)

For easy API testing and exploration, use the built-in API documentation interface:

```bash
# 1. Start the backend server
./gradlew :backend:run

# 2. Start the web server (in a new terminal)
cd web
python3 server.py

# 3. Open the API testing interface
# Navigate to: http://localhost:3000/api-docs.html
```

**Features of the API Testing Interface:**
- 📋 **Complete API Documentation**: All endpoints with descriptions and examples
- 🔐 **Authentication Testing**: Login and OTP verification with token management
- 🧪 **Interactive Forms**: Test any endpoint directly from the browser
- 📊 **Response Viewer**: Formatted JSON responses with syntax highlighting
- 🔄 **Token Management**: Automatic token handling for authenticated requests
- 📱 **Responsive Design**: Works on desktop, tablet, and mobile devices

**API Testing Workflow:**
1. **Authentication**: Use the login endpoint to get an OTP (check backend logs)
2. **Token Generation**: Verify OTP to receive authentication token
3. **API Testing**: Use the token to test protected endpoints
4. **Data Management**: Create chits, manage users, and test all functionality

#### 🔧 Manual API Testing with cURL

You can also test the API manually using cURL commands:

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
- **Web Application**: Modern HTML/CSS/JavaScript single-page application
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

## ☁️ Azure Deployment

The backend can be deployed to Azure using the provided infrastructure setup:

### Quick Deploy to Azure
```bash
# 1. Login to Azure CLI
az login

# 2. Run the deployment script
./deploy-azure.sh

# 3. Configure GitHub secrets and push to main branch
# The CI/CD pipeline will automatically deploy your app
```

### Azure Infrastructure
- **Azure Container Registry** for Docker images
- **Azure Database for PostgreSQL** for data storage  
- **Azure Web App for Containers** for hosting
- **GitHub Actions** for automated CI/CD

### Monitoring Deployments
- **GitHub Actions**: Monitor build and deployment status at [Actions tab](https://github.com/PradeepAmradi/ChitFundApp/actions)
- **Azure Portal**: View application logs and metrics in the Azure portal
- **Application Health**: Automatic health checks are performed after deployment
- **Deployment Logs**: Detailed logs available in both GitHub Actions and Azure Web App logs

### Troubleshooting
```bash
# Check deployment status
az webapp show --name chitfund-webapp --resource-group chitfund-rg --query "state"

# View application logs
az webapp log tail --name chitfund-webapp --resource-group chitfund-rg

# Restart the application
az webapp restart --name chitfund-webapp --resource-group chitfund-rg
```

See [`AZURE_DEPLOYMENT.md`](AZURE_DEPLOYMENT.md) for detailed setup instructions.

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