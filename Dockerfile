# Runtime-only Dockerfile.
# Перед сборкой образа нужно собрать JAR:
#   ./gradlew :configurator:bootJar

FROM eclipse-temurin:25-jre
WORKDIR /app

ARG JAR_FILE=configurator/build/libs/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
