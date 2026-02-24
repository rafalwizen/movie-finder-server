FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/*.jar app.jar
COPY db/cinema.db db/cinema.db

RUN mkdir -p db && chmod -R 777 db

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
