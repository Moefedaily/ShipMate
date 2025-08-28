# ---- Base build image ----
FROM maven:3.9.6-eclipse-temurin-21 as build
WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Package the application (without running tests for dev speed)
RUN mvn clean package -DskipTests

# ---- Runtime image ----
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose backend port (used in docker-compose)
EXPOSE 8080

# Default command
ENTRYPOINT ["java", "-jar", "app.jar"]
