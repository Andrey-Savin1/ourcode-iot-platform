#!/bin/bash

## Правильный адрес зависит от того, где выполняется скрипт:
## Если внутри контейнера Kafka:
#BOOTSTRAP="kafka:9092"
#
## Если с хоста (через порт 9092):
## BOOTSTRAP="localhost:9092"

MAX_RETRIES=5
RETRY_INTERVAL=5
BOOTSTRAP_SERVER="localhost:9092"

echo "Проверяем доступность Kafka..."

for ((i=1; i<=$MAX_RETRIES; i++)); do
  if kafka-topics --bootstrap-server=$BOOTSTRAP_SERVER --list &>/dev/null; then
    echo "Kafka доступна, создаём топики..."

    # Создаём топик events
    kafka-topics --bootstrap-server=$BOOTSTRAP_SERVER \
      --create \
      --if-not-exists \
      --topic events \
      --partitions 3 \
      --replication-factor 1

    # Создаём топик device-id-topic
    kafka-topics --bootstrap-server=$BOOTSTRAP_SERVER \
      --create \
      --if-not-exists \
      --topic device-id-topic \
      --partitions 3 \
      --replication-factor 1

    echo "Проверяем созданные топики:"
    kafka-topics --bootstrap-server=$BOOTSTRAP_SERVER --list

    exit 0
  else
    echo "Попытка $i из $MAX_RETRIES: Kafka недоступна, ждём $RETRY_INTERVAL сек..."
    sleep $RETRY_INTERVAL
  fi
done

echo "Ошибка: Kafka не стала доступной после $MAX_RETRIES попыток"
exit 1



#echo "Ждём, пока Kafka станет доступной..."
#sleep 25
#echo "Создаём топик 'events'..."
#
## Создаём топик events
#kafka-topics --bootstrap-server=$BOOTSTRAP_SERVER \
#      --create \
#      --if-not-exists \
#      --topic events \
#      --partitions 3 \
#      --replication-factor 1
#
# # Создаём топик device-id-topic
# kafka-topics --bootstrap-server=$BOOTSTRAP_SERVER \
#      --create \
#      --if-not-exists \
#      --topic device-id-topic \
#      --partitions 3 \
#      --replication-factor 1
#echo "Все топики успешно созданы!"


