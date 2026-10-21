FROM eclipse-temurin:latest

WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app

COPY target/*.jar /app/app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]