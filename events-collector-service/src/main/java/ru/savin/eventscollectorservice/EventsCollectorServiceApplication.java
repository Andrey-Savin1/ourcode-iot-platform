package ru.savin.eventscollectorservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EventsCollectorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventsCollectorServiceApplication.class, args);
    }

}
