# Runtime-only Dockerfile.
# Перед сборкой образа нужно собрать JAR:
#   ./gradlew :configurator:bootJar

FROM eclipse-temurin:21-jre@sha256:7a65df4b22d2de92d4e04056e884f3b9122d70b21e2847fd66084278bd0ce037

ARG OCI_CREATED=""
ARG OCI_REVISION="unknown"
ARG OCI_VERSION="0.0.0-dev"

LABEL org.opencontainers.image.title="Configurator Backend" \
      org.opencontainers.image.description="Spring Boot API for the local Configurator preview" \
      org.opencontainers.image.source="https://github.com/eltgm/configurator-root" \
      org.opencontainers.image.revision="${OCI_REVISION}" \
      org.opencontainers.image.version="${OCI_VERSION}" \
      org.opencontainers.image.created="${OCI_CREATED}" \
      org.opencontainers.image.licenses="MIT"

WORKDIR /app

ARG JAR_FILE=configurator/build/libs/configurator-0.1.0-SNAPSHOT.jar
COPY --chown=10001:10001 ${JAR_FILE} app.jar

EXPOSE 8080

USER 10001:10001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
