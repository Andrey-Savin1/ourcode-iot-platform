# Stage 1: Сборка проекта
FROM gradle:8.12.1-jdk21-alpine AS builder
WORKDIR /device-collector-service
COPY --chown=gradle:gradle . .
RUN gradle clean build -x test --no-daemon

# Stage 2: Запуск приложения с минимальным JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /device-collector-service
EXPOSE 9992
# Копируем jar-файл из первого этапа
COPY --from=builder /device-collector-service/build/libs/device-collector-service-0.0.1-SNAPSHOT.jar /device-collector-service.jar
# Запуск приложения
ENTRYPOINT ["java", "-jar", "/device-collector-service.jar"]

#запуск в докере
#docker run -d -p 9992:9992 device-collector-service