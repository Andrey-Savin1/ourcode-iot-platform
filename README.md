## Модуль 1: IoT Monitoring Platform 

- Архитектурный шаблон микросервисной IoT-системы, включающий инфраструктуру,
  контекстную и контейнерную диаграммы, а также готовое окружение через Docker Compose.

## Цель репозитория

Этот репозиторий содержит:

- Архитектурные диаграммы в формате C4 находятся в [diagrams](diagrams)]
- Конфигурации Docker Compose для локального развёртывания
- Шаблоны дашбордов Grafana
- Makefile для автоматизации

Запуск инфраструктуры:

- docker-compose up -d

## Доступные URL после запуска

- http://localhost:5432 — PostgreSQL
- http://localhost:9000 — MinIO
- http://localhost:6379 — Redis
- http://localhost:9093 — Kafka
- http://localhost:8084 — KafkaUi
- http://localhost:8085 — Schema Registry
- http://localhost:8080 — Keycloak
- http://localhost:3000 — Grafana
- http://localhost:9042— Cassandra
- http://localhost:8088— Camunda
- http://localhost:9090— Prometheus


