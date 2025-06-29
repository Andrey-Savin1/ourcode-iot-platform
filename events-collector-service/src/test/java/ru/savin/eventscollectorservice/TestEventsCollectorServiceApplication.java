package ru.savin.eventscollectorservice;

import org.springframework.boot.SpringApplication;
import ru.savin.eventscollectorservice.util.TestContainersConfig;

public class TestEventsCollectorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(EventsCollectorServiceApplication::main).with(TestContainersConfig.class).run(args);
    }

}
