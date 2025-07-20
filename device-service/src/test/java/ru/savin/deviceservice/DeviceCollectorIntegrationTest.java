package ru.savin.deviceservice;


import com.zaxxer.hikari.HikariDataSource;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.savin.deviceservice.dto.DeviceDto;
import ru.savin.deviceservice.model.Device;
import ru.savin.deviceservice.repository.DeviceRepository;
import ru.savin.deviceservice.util.BaseConfig;
import ru.savin.deviceservice.util.TestContainersConfig;


import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.savin.deviceservice.util.TestContainersConfig.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Slf4j
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class DeviceCollectorIntegrationTest extends BaseConfig {


    private final TestRestTemplate restTemplate;
    private final DeviceRepository deviceRepository;

    @BeforeAll
    public static void createDevicesTableInShard() {
        executeSql(POSTGRES_0, """
                CREATE TABLE IF NOT EXISTS device_0.public.devices (
                    device_id text primary key,
                    device_type text,
                    created_at bigint,
                    meta text
                );
                """);

        executeSql(POSTGRES_1, """
                CREATE TABLE IF NOT EXISTS device_1.public.devices (
                    device_id text primary key,
                    device_type text,
                    created_at bigint,
                    meta text
                );""");
    }

    private static void executeSql(PostgreSQLContainer<?> container, String sql) {
        try (Connection conn = container.createConnection("");
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL", e);
        }
    }


    @Test
    @DisplayName("Testing method POST")
    void controllerPostIntegrationTest() {

        Device saveInDb = new Device();
        saveInDb.setDeviceId("");
        saveInDb.setDeviceType("public1");
        saveInDb.setMeta("meta1");
        saveInDb.setCreatedAt(System.currentTimeMillis());

        int keycloakPort = KEYCLOAK_CONTAINER.getMappedPort(8080);
        log.debug("port: {}", keycloakPort);
        Response response = RestAssured.given()
                .baseUri("http://localhost:" + keycloakPort)
                .basePath("/realms/devices/protocol/openid-connect/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .formParam("grant_type", "password")
                .formParam("client_id", "devices-api")
                .formParam("username", "pol")
                .formParam("password", "1234")
                .when()
                .post();

        // Извлечение токена
        String accessToken = response.jsonPath().getString("access_token");
        log.debug("Token: {}", accessToken);

        // Создание заголовка с токеном
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Device> requestEntity = new HttpEntity<>(saveInDb, headers);
        //Вызовем ендпоинт и проверим, что возвращается правильный объект
        ResponseEntity<DeviceDto> resp = restTemplate.exchange(
                "/api/v1/devices",
                HttpMethod.POST,
                requestEntity,
                DeviceDto.class
        );

        //Ендпоинт отдает 204
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().getDeviceType()).isEqualTo(saveInDb.getDeviceType());
    }

    @Test
    @DisplayName("Testing method PUT")
    void controllerPutIntegrationTest() {

        Device saveInDb = new Device();
        saveInDb.setDeviceId("1");
        saveInDb.setDeviceType("public1");
        saveInDb.setMeta("meta1");
        saveInDb.setCreatedAt(System.currentTimeMillis());
        deviceRepository.save(saveInDb);

        Device update = new Device();
        update.setDeviceId("1");
        update.setDeviceType("public2");
        update.setMeta("meta2");
        update.setCreatedAt(System.currentTimeMillis());

        int keycloakPort = KEYCLOAK_CONTAINER.getMappedPort(8080);
        log.debug("port: {}", keycloakPort);
        Response response = RestAssured.given()
                .baseUri("http://localhost:" + keycloakPort)
                .basePath("/realms/devices/protocol/openid-connect/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .formParam("grant_type", "password")
                .formParam("client_id", "devices-api")
                .formParam("username", "pol")
                .formParam("password", "1234")
                .when()
                .post();

        // Извлечение токена
        String accessToken = response.jsonPath().getString("access_token");
        log.debug("Token: {}", accessToken);

        // Создание заголовка с токеном
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Device> requestEntity = new HttpEntity<>(update, headers);
        //Вызовем ендпоинт и проверим, что возвращается правильный объект
        ResponseEntity<DeviceDto> resp = restTemplate.exchange(
                "/api/v1/devices/{id}",
                HttpMethod.PUT,
                requestEntity,
                DeviceDto.class,
                saveInDb.getDeviceId()
        );

        //Ендпоинт отдает 200
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getDeviceType()).isEqualTo(update.getDeviceType());
    }

    @Test
    @DisplayName("Testing method GET and correct Sharding")
    void controllerGetIntegrationTest() throws InterruptedException {

        Device saveInDb = new Device();
        saveInDb.setDeviceId("1");
        saveInDb.setDeviceType("public1");
        saveInDb.setMeta("meta1");
        saveInDb.setCreatedAt(System.currentTimeMillis());

        Device saveInDb2 = new Device();
        saveInDb2.setDeviceId("2");
        saveInDb2.setDeviceType("public2");
        saveInDb2.setMeta("meta2");
        saveInDb2.setCreatedAt(System.currentTimeMillis());

        List<Device> devices = new ArrayList<>();
        devices.add(saveInDb);
        devices.add(saveInDb2);
        deviceRepository.saveAll(devices);

        List<Device> firstShard = new ArrayList<>();
        List<Device> secondShard = new ArrayList<>();

        // RowMapper для маппинга результата в объект Device
        RowMapper<Device> rowMapper = (rs, rowNum) -> {
            Device device = new Device();
            device.setDeviceId(rs.getString("device_id"));
            device.setDeviceType(rs.getString("device_type"));
            device.setCreatedAt(rs.getLong("created_at"));
            device.setMeta(rs.getString("meta"));
            return device;
        };

        Thread.sleep(5000);

        // Запрос к первому шарду
        try (HikariDataSource ds0 = createDataSource(POSTGRES_0)) {
            JdbcTemplate template0 = new JdbcTemplate(ds0);
            List<Device> devices0 = template0.query("SELECT * FROM device_0.public.devices", rowMapper);
            firstShard.addAll(devices0);
        }

        // Запрос ко второму шарду
        try (HikariDataSource ds1 = createDataSource(POSTGRES_1)) {
            JdbcTemplate template1 = new JdbcTemplate(ds1);
            List<Device> devices1 = template1.query("SELECT * FROM device_1.public.devices", rowMapper);
            secondShard.addAll(devices1);
        }

        // Получим первые элементы
        var deviceShard0 = firstShard.stream().findFirst().get();
        var deviceShard1 = secondShard.stream().findFirst().get();

        int keycloakPort = KEYCLOAK_CONTAINER.getMappedPort(8080);
        log.debug("port: {}", keycloakPort);
        Response response = RestAssured.given()
                .baseUri("http://localhost:" + keycloakPort)
                .basePath("/realms/devices/protocol/openid-connect/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .formParam("grant_type", "password")
                .formParam("client_id", "devices-api")
                .formParam("username", "pol")
                .formParam("password", "1234")
                .when()
                .post();

        // Извлечение токена
        String accessToken = response.jsonPath().getString("access_token");
        log.debug("Token: {}", accessToken);

        // Создайте заголовки с токеном
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        //Вызовем ендпоинт и проверим, что возвращается правильный объект
        ResponseEntity<DeviceDto> resp = restTemplate.exchange(
                "/api/v1/devices/{id}",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                DeviceDto.class,
                saveInDb.getDeviceId()
        );

        // Проверяю количество объектов которые достали из базы
        assertEquals(1, firstShard.size());
        assertEquals(1, secondShard.size());
        //Проверяю deviceId объектов из шард
        assertEquals(deviceShard0.getDeviceId(), "2");
        assertEquals(deviceShard1.getDeviceId(), "1");

        //Ендпоинт отдает 200 ОК
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        // deviceId совпадает
        assertThat(resp.getBody().getDeviceId()).isEqualTo(saveInDb.getDeviceId());

    }

    @Test
    @DisplayName("Testing method DELETE")
    void controllerDeleteIntegrationTest() {

        Device saveInDb = new Device();
        saveInDb.setDeviceId("1");
        saveInDb.setDeviceType("public1");
        saveInDb.setMeta("meta1");
        saveInDb.setCreatedAt(System.currentTimeMillis());

        Device saveInDb2 = new Device();
        saveInDb2.setDeviceId("2");
        saveInDb2.setDeviceType("public2");
        saveInDb2.setMeta("meta2");
        saveInDb2.setCreatedAt(System.currentTimeMillis());

        List<Device> devices = new ArrayList<>();
        devices.add(saveInDb);
        devices.add(saveInDb2);
        deviceRepository.saveAll(devices);

        int keycloakPort = KEYCLOAK_CONTAINER.getMappedPort(8080);
        log.debug("port: {}", keycloakPort);
        Response response = RestAssured.given()
                .baseUri("http://localhost:" + keycloakPort)
                .basePath("/realms/devices/protocol/openid-connect/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .formParam("grant_type", "password")
                .formParam("client_id", "devices-api")
                .formParam("username", "pol")
                .formParam("password", "1234")
                .when()
                .post();

        // Извлечение токена
        String accessToken = response.jsonPath().getString("access_token");
        log.debug("Token: {}", accessToken);

        // Создайте заголовки с токеном
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        //Вызовем ендпоинт и проверим, что возвращается правильный объект
        ResponseEntity<DeviceDto> resp = restTemplate.exchange(
                "/api/v1/devices/{deviceId}",
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                DeviceDto.class,
                saveInDb.getDeviceId()
        );

        //Ендпоинт отдает 204
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // Настройка DataSource для шард
    private HikariDataSource createDataSource(TestContainersConfig.FixedPortPostgreSQLContainer config) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(config.getJdbcUrl());
        ds.setUsername(config.getUsername());
        ds.setPassword(config.getPassword());
        log.debug("ДАТАСОРС {} {}", ds.getPassword(), ds.getJdbcUrl());
        return ds;
    }

    @Test
    public void getKeycloakToken() {
        int keycloakPort = KEYCLOAK_CONTAINER.getFirstMappedPort();

        Response response = RestAssured.given()
                .baseUri("http://localhost:" + keycloakPort)
                .basePath("/realms/devices/protocol/openid-connect/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .formParam("grant_type", "password")
                .formParam("client_id", "devices-api")
                .formParam("username", "pol")
                .formParam("password", "1234")
                .when()
                .post();

        // Проверка статус кода
        response.then().statusCode(200);

        // Извлечение токена
        String accessToken = response.jsonPath().getString("access_token");
        System.out.println("Access Token: " + accessToken);

    }
}
