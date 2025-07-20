package ru.savin.deviceservice;

import org.springframework.boot.SpringApplication;
import ru.savin.deviceservice.util.TestContainersConfig;

public class TestDeviceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(DeviceServiceApplication::main).with(TestContainersConfig.class).run(args);
    }

}
