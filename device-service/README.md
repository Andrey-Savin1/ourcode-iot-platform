# Модуль 4: Device Service

## Ключевые функции device-service
- Предоставляет CRUD API для работы с устройствами
- Читает и обновляет данные из шардированной базы данных PostgreSQL,
- Поддерживает горизонтальное масштабирование и синхронизацию данных между шардами,
- Экспонирует метрики, обеспечивает отказоустойчивость, логирование и удобную эксплуатацию.
- Все эндпоинты защищены OIDC (JWT) через Keycloak (Проверка токена на каждом запросе, доступ по ролям.)
- Использует кеш Redis (read-through стратегия) для быстрого доступа к данным.

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
- 📄[`sequence`](./diagrams/component.puml)

## Инструкция по запуску:
- Запуск инфраструктуры: из корня выполнить docker-compose up -d
- Запуск микросервиса: docker-compose -f device-service/docker-compose.yml up -d
- Сервис будет слушать порт 9993

## Технологический стек
- Язык: Java 21
- Фреймворки: Spring Boot 3.5.3, Spring Web MVC, Spring Data JPA, Spring Boot Actuator, Spring Data Redis, 
- База данных:  PostgreSQL
- Безопасность: Spring Security, Keycloak Spring Boot Starter 25.0.3, Keycloak Admin Client 26.0.2
- Трейсы: OpenTelemetry Spring Boot Starter 2.11.0
- Логи: Loki4j Logback Appender 1.4.2
- Шардинг: Apache ShardingSphere JDBC 5.5.2
- Миграции: Flyway Core + Flyway PostgreSQL
- Мониторинг: Micrometer + Prometheus (через micrometer-registry-prometheus)
- Сериализация: Avro (для Kafka сообщений) + плагин
- Утилиты: Lombok 1.18.30, MapStruct 1.6.3
- Тестирование: Testcontainers (Kafka, PostgreSQL), Spring Boot Test, Spring Kafka Test, JUnit 5 (Jupiter)
- Инфраструктура: Spring Testcontainers, JUnit 5