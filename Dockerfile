# --- Build stage: compile the JAR inside the container ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline          # cache dependencies layer
COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Runtime stage: JRE only ---
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir -p db
COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
