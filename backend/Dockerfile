# ---- build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- run
# A JDK (not a JRE): the judge and the language service run javac in-process.
# The Docker CLI lets DOCKER sandbox mode start one throwaway container per run, either
# through a mounted socket (Docker Compose) or a Docker-in-Docker sidecar (Kubernetes,
# via DOCKER_HOST).
FROM eclipse-temurin:21-jdk
COPY --from=docker:27-cli /usr/local/bin/docker /usr/local/bin/docker
RUN groupadd --system --gid 10001 algocode \
    && useradd --system --uid 10001 --gid algocode --home-dir /app algocode \
    && mkdir -p /app/data && chown -R algocode:algocode /app
WORKDIR /app
COPY --from=build /src/target/algocode-backend-*.jar app.jar
USER 10001
EXPOSE 8080
ENV ALGOCODE_SANDBOX_MODE=DOCKER
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-jar", "/app/app.jar"]
