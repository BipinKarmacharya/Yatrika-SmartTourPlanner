# -------- Build Stage --------
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Cache dependencies (standard industry practice)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Build the JAR
COPY src ./src
RUN mvn clean package -DskipTests

# -------- Runtime Stage --------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Security: Run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 3. Copy only the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Configuration
EXPOSE 8080

# The -Djava.security.egd helps with faster startup in Linux environments
ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", \
            "app.jar"]