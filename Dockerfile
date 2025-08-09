FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the backend JAR
COPY backend/build/libs/chitfund-backend.jar app.jar

# Copy the web assets
COPY web ./web

EXPOSE 8080

# Use H2 database by default for Azure deployment (can be overridden with env vars)
ENV DATABASE_URL=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1

CMD ["java", "-jar", "app.jar"]