FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the backend JAR
COPY backend/build/libs/chitfund-backend.jar app.jar

# Copy the web assets
COPY web ./web

EXPOSE 8080

# Database URL must be set via environment variables for production.
# Falls back to H2 in-memory ONLY if no DATABASE_URL is provided at runtime.
# To use PostgreSQL, set DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD env vars.
# ENV DATABASE_URL=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1

CMD ["java", "-jar", "app.jar"]