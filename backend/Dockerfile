# Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Run: a JDK (not a JRE) because the language service and judge use javac in-process,
# plus the Docker CLI so DOCKER sandbox mode can start sibling containers via the host socket.
FROM eclipse-temurin:21-jdk
COPY --from=docker:27-cli /usr/local/bin/docker /usr/local/bin/docker
WORKDIR /app
COPY --from=build /src/target/algocode-backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-jar", "/app/app.jar"]
