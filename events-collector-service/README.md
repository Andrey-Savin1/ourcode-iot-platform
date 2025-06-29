# Модуль 2: Events Collector

## Назначение сервиса:

events-collector-service - микросервис который обрабатывает поток событий IoT-устройств:

## Ключевые функции
- Получает события из Kafka в формате Avro (валидация через Schema Registry)
- Сохраняет их в оптимизированную под аналитические запросы таблицу в Cassandra
- Извлекает уникальные `device_id` и публикует их в отдельный Kafka-топик для downstream-процессов
- Откидывает метрики и liveness/readiness через Spring Actuator.

## Диаграммы
- Архитектурные диаграммы в формате C4 находятся в [diagrams](diagrams)

## Инструкция по запуску:
- Запуск инфраструктуры: из корня выполнить docker-compose up -d
- Запуск микросервиса: docker-compose -f events-collector-service/docker-compose.yml up -d
- Сервис будет слушать порт 9991
   

## Технологический стек
- Язык: Java 21
- Фреймворки: Spring Boot 3.5.0, Spring Web MVC, Spring Data Cassandra, Spring Kafka, Spring Actuator
- База данных: Cassandra
- Работа с сообщениями: Apache Kafka, Apache Avro 1.12.0, Confluent Kafka Avro Serializer 7.8.1
- Мониторинг: Micrometer + Prometheus (через micrometer-registry-prometheus)
- Сериализация: Avro (для Kafka сообщений) + плагин
- Утилиты: Lombok 1.18.30
- Тестирование: Testcontainers (Cassandra, Kafka, JUnit Jupiter), Spring Boot Test, Spring Kafka Test
- Инфраструктура: Spring Testcontainers, JUnit 5