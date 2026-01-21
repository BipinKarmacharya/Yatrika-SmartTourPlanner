# -------- Build Stage --------
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies first for caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build JAR
COPY src ./src
RUN mvn clean package -DskipTests -Pprod

# -------- Runtime Stage --------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

EXPOSE 8080

# Run the app with optimized JVM settings
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "-Dserver.port=${PORT:-8080}", "app.jar"]