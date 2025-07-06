# Модуль 2: Device Collector

## Назначение сервиса:

- device-collector-service - микросервис, который сохраняет/обновляет информацию об устройствах используя шардирование

## Ключевые функции
- Получает события из Kafka в формате Avro (валидация через Schema Registry)
- Cохраняет/обновляет информацию о устройствах в PostgreSQL, используя шардирование через Apache ShardingSphere
- Гарантирует идемпотентность, корректную обработку ошибок
- Откидывает метрики и liveness/readiness через Spring Actuator.

## Реализация шардинга
### Настройки находятся в resources:  
- 📄[`sharding.yaml`](./src/main/resources/sharding.yaml)

### Принцип работы
Распределение данных:
- Устройства распределяются между шардами по хешу device_id
- Формула: ``` target_shard = abs(device_id.hashCode()) % 2 → ds_0 или ds_1 ```


### Особенности:
- Все шарды содержат одинаковую схему таблиц
- Запросы автоматически маршрутизируются к нужному шарду
- Поддерживаются транзакции в пределах одного шарда

### Миграции данных
- Flyway-миграции нужно выполнять поочередно к каждой базе используя профили в  📄[`application.yaml`](./src/main/resources/application.yaml)
- Файлы миграций находятся в [migration](./src/main/resources/db/migration)

## Диаграммы
### Архитектурные диаграммы в формате C4 находятся в [diagrams](diagrams):

- 📄[`container`](./diagrams/container.puml)
- 📄[`context`](./diagrams/context.puml)
- 📄[`sequence`](./diagrams/sequence.puml)

## Инструкция по запуску:
- Запуск инфраструктуры: из корня выполнить docker-compose up -d
- Запуск микросервиса: docker-compose -f device-collector-service/docker-compose.yml up -d
- Сервис будет слушать порт 9992


## Технологический стек
- Язык: Java 21
- Фреймворки: Spring Boot 3.5.3, Spring Web MVC, Spring Data JPA, Spring Kafka, Spring Actuator, Spring Testcontainers
- База данных:  PostgreSQL
- Шардинг: Apache ShardingSphere JDBC 5.5.2
- Миграции: Flyway Core + Flyway PostgreSQL
- Работа с сообщениями: Apache Kafka, Apache Avro 1.12.0, Confluent Kafka Avro Serializer 7.8.1
- Мониторинг: Micrometer + Prometheus (через micrometer-registry-prometheus)
- Сериализация: Avro (для Kafka сообщений) + плагин
- Утилиты: Lombok 1.18.30, MapStruct 1.6.3
- Тестирование: Testcontainers (Kafka, PostgreSQL), Spring Boot Test, Spring Kafka Test, JUnit 5 (Jupiter)
- Инфраструктура: Spring Testcontainers, JUnit 5