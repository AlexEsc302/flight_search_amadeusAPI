# Build
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY . .

RUN ./gradlew build --no-daemon

# Runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
