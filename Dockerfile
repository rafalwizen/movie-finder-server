FROM eclipse-temurin:21-jre

WORKDIR /app

RUN mkdir -p db

COPY target/*.jar app.jar
COPY db/cinema.db db/cinema.db

RUN chmod -R 777 db

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
