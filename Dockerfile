# Shared multi-stage build for every Spring Boot service in this repo.
# docker-compose sets the build context to each service folder and points
# the dockerfile here, so one file builds them all.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline || true
COPY src ./src
RUN mvn -q -B -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*-0.0.1-SNAPSHOT.jar app.jar
# size the heap to the container memory limit
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
