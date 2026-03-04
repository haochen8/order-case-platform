FROM gradle:9.0.0-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle --no-daemon clean bootJar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
