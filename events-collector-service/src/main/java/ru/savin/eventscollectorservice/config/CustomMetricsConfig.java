package ru.savin.eventscollectorservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CustomMetricsConfig {

    private final Counter successCounter;
    private final Counter failureCounter;

    public CustomMetricsConfig(MeterRegistry registry) {
        this.successCounter = Counter.builder("cassandra.save.success")
                .description("Количество успешных записей в Cassandra")
                .tag("component", "cassandra-writer")
                .tag("operation", "save")
                .register(registry);

        this.failureCounter = Counter.builder("cassandra.save.failure")
                .description("Количество ошибок при записи в Cassandra")
                .tag("component", "cassandra-writer")
                .tag("operation", "save")
                .register(registry);
    }

    public void incrementSuccessSaveToCassandra() {
        successCounter.increment();
    }
    public void incrementFailedSaveToCassandra() {
        failureCounter.increment();
    }
}
