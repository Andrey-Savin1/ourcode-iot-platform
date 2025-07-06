package ru.savin.devicecollectorservice;

import org.springframework.boot.SpringApplication;
import ru.savin.devicecollectorservice.util.TestContainersConfig;

public class TestDeviceCollectorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(DeviceCollectorServiceApplication::main).with(TestContainersConfig.class).run(args);
    }

}
