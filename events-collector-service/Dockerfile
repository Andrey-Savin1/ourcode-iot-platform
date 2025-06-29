# Stage 1: Сборка проекта
FROM gradle:8.12.1-jdk21-alpine AS builder
WORKDIR /events-collector-service
COPY --chown=gradle:gradle . .
RUN gradle clean build -x test --no-daemon

# Stage 2: Запуск приложения с минимальным JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /events-collector-service
EXPOSE 9991
# Копируем jar-файл из первого этапа
COPY --from=builder /events-collector-service/build/libs/events-collector-service-0.0.1-SNAPSHOT.jar /events-collector-service.jar
# Запуск приложения
ENTRYPOINT ["java", "-jar", "/events-collector-service.jar"]

#запуск в докере
#docker run -d -p 9991:9991 events-collector-service