# -------- Build Stage --------
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# -------- Runtime Stage --------
FROM amazoncorretto:21
WORKDIR /app

COPY --from=build /build/target/yatrika-backend.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=dev
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-sql-yatrika:5432/yatrika

ENTRYPOINT ["java", "-jar", "app.jar"]
