# Makefile для ourcode-iot-platform

.PHONY: help up down build logs restart clean dashboard

help:
    @echo "Available commands:"
    @echo "  make up         - Запустить всю инфраструктуру"
    @echo "  make down       - Остановить всё"
#    @echo "  make build      - Собрать микросервис"
    @echo "  make restart    - Перезапустить всё"
    @echo "  make dashboard  - Открыть Grafana"

up:
    docker-compose.yaml up -d

down:
     docker-compose.yaml down

#build:
#    mvn clean package

restart:
     docker-compose.yaml restart

dashboard:
    @echo "Открываем Grafana..."
    open http://localhost:3000

grafana:
    @echo "Grafana URL: http://localhost:3000"

keycloak:
    @echo "Keycloak: http://localhost:8080/auth"

reset:
    make down
    make clean
    make up