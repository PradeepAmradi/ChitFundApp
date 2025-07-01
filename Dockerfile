FROM openjdk:17-jre-slim

WORKDIR /app

COPY backend/build/libs/chitfund-backend.jar app.jar

EXPOSE 8080

ENV DATABASE_URL=jdbc:postgresql://localhost:5432/chitfund

CMD ["java", "-jar", "app.jar"]