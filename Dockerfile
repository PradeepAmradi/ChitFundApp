FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the backend JAR
COPY backend/build/libs/chitfund-backend.jar app.jar

# Copy the web assets
COPY web ./web

EXPOSE 8080

ENV DATABASE_URL=jdbc:postgresql://localhost:5432/chitfund

CMD ["java", "-jar", "app.jar"]