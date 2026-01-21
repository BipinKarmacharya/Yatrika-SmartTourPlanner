# -------- Build Stage --------
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /build

# Copy pom.xml and download dependencies first for caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build JAR
COPY src ./src
RUN mvn clean package -DskipTests

# -------- Runtime Stage --------
FROM amazoncorretto:21-alpine
WORKDIR /app

# Install bash for debugging and wait-for-it script
RUN apk add --no-cache bash

# Copy wait-for-it script for database health check
COPY --from=build /build/wait-for-it.sh /app/wait-for-it.sh
RUN chmod +x /app/wait-for-it.sh

# Copy JAR from build stage
COPY --from=build /build/target/*.jar app.jar

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the app with optimized JVM settings
ENTRYPOINT ["java", "-jar", "app.jar"]