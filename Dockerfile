# ==========================================
# Stage 1: Build stage
# ==========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml first to cache maven dependencies
COPY pom.xml .

# Download dependencies in offline mode for caching efficiency
RUN mvn dependency:go-offline -B

# Copy application source code
COPY src ./src

# Package the application as an executable JAR without running tests
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Production Runtime stage
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for security best practices
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy compiled JAR file from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose default port (Render will inject the PORT environment variable)
EXPOSE 8080

# Configure JVM flags and Render dynamic port binding
ENV PORT=8080 \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Start the Spring Boot application
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
