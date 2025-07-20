# Stage 1: Сборка проекта
FROM gradle:8.12.1-jdk21-alpine AS builder
WORKDIR /device-service
COPY --chown=gradle:gradle . .
RUN gradle clean build -x test --no-daemon

# Stage 2: Запуск приложения с минимальным JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /device-service
EXPOSE 9993
# Копируем jar-файл из первого этапа
COPY --from=builder /device-service/build/libs/device-service-0.0.1-SNAPSHOT.jar /device-service.jar
# Запуск приложения
ENTRYPOINT ["java", "-jar", "/device-service.jar"]

#запуск в докере
#docker run -d -p 9993:9993 device-service