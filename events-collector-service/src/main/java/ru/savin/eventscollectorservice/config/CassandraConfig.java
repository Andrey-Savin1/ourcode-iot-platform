package ru.savin.eventscollectorservice.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.CassandraTemplate;

@Configuration
public class CassandraConfig {

    @Bean
    public CassandraTemplate cassandraTemplate(CqlSession cqlSession) {
        return new CassandraTemplate(cqlSession);
    }
}