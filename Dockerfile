# Runtime-only Dockerfile.
# Перед сборкой образа нужно собрать JAR:
#   ./gradlew :configurator:bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app

ARG JAR_FILE=configurator/build/libs/configurator-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
