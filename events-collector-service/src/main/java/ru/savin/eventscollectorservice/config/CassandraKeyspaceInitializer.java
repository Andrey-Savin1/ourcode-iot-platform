package ru.savin.eventscollectorservice.config;

import com.datastax.oss.driver.api.core.CqlSession;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CassandraKeyspaceInitializer {

    private final CqlSession session;

    @PostConstruct
    public void init() {
        String createKeyspace = "CREATE KEYSPACE IF NOT EXISTS device " +
                "WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}";
        String createTableDeviceEvent = "CREATE TABLE IF NOT EXISTS device.device_events_by_device\n" +
                "              (\n" +
                "               device_id text,\n" +
                "               event_id  text,\n" +
                "               timestamp bigint,\n" +
                "               type      text,\n" +
                "               payload   text,\n" +
                "               PRIMARY KEY (device_id, timestamp)\n" +
                "               ) WITH CLUSTERING ORDER BY (timestamp DESC);";

        String createTableDeviceId = "CREATE TABLE IF NOT EXISTS device.device_ids (device_id TEXT PRIMARY KEY)";
        try {
            session.execute("SELECT release_version FROM system.local");
            log.info(" Cassandra доступна");
            session.execute(createKeyspace);
            session.execute(createTableDeviceEvent);
            session.execute(createTableDeviceId);

            log.info("Keyspace и таблицы успешно созданы или уже существуют.");
        } catch (Exception e) {
            log.error("Ошибка при создании keyspace 'device': {}", e.getMessage());
        }
    }

}
