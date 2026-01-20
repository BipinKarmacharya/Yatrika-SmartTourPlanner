# -------- Build Stage --------
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /build

# Copy pom.xml and download dependencies first for caching
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source and build JAR
COPY src ./src
RUN mvn clean package -DskipTests

# -------- Runtime Stage --------
FROM amazoncorretto:21
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /build/target/yatrika-backend.jar app.jar

EXPOSE 8080

# Environment variables
ENV SPRING_PROFILES_ACTIVE=dev
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/yatrika

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
