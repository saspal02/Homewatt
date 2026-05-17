package com.homewatt.usage_service.integration;

import com.homewatt.usage_service.client.DeviceClient;
import com.homewatt.usage_service.client.UserClient;
import com.homewatt.usage_service.dto.DeviceDto;
import com.homewatt.usage_service.dto.UsageDto;
import com.homewatt.usage_service.testsupport.ContainersBase;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class UsageServiceIntegrationTest extends ContainersBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InfluxDBClient influxDBClient;

    @Autowired
    private DeviceClient deviceClient;

    @Autowired
    private UserClient userClient;

    @Value("${influx.bucket}")
    private String bucket;

    @Value("${influx.org}")
    private String org;

    @Test
    void getUsageForUser_returnsAggregatedData() {
        Long userId = 1L;
        Long deviceId = 100L;

        when(deviceClient.getAllDevicesForUser(userId))
                .thenReturn(List.of(DeviceDto.builder()
                        .id(deviceId).name("Thermostat").type("THERMOSTAT")
                        .location("Living Room").userId(userId).energyConsumed(0.0)
                        .build()));

        Point point = Point.measurement("energy_usage")
                .addTag("deviceId", String.valueOf(deviceId))
                .addField("energyConsumed", 500.0)
                .time(Instant.now(), WritePrecision.MS);
        influxDBClient.getWriteApiBlocking().writePoint(bucket, org, point);

        ResponseEntity<UsageDto> response = restTemplate
                .getForEntity("/api/v1/usage/{userId}?days=1", UsageDto.class, userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().userId()).isEqualTo(userId);
        assertThat(response.getBody().devices()).isNotNull();
        assertThat(response.getBody().devices()).isNotEmpty();
        DeviceDto device = response.getBody().devices().get(0);
        assertThat(device.name()).isEqualTo("Thermostat");
        assertThat(device.energyConsumed()).isEqualTo(500.0);
    }

    @Test
    void getUsageForUser_noDevices_returnsEmpty() {
        Long userId = 99L;

        when(deviceClient.getAllDevicesForUser(userId)).thenReturn(List.of());

        ResponseEntity<UsageDto> response = restTemplate
                .getForEntity("/api/v1/usage/{userId}", UsageDto.class, userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().userId()).isEqualTo(userId);
        assertThat(response.getBody().devices()).isNull();
    }

    @Test
    void getUsageForUser_multipleDevices_aggregatesCorrectly() {
        Long userId = 3L;
        Long deviceA = 10L;
        Long deviceB = 11L;

        when(deviceClient.getAllDevicesForUser(userId)).thenReturn(List.of(
                DeviceDto.builder().id(deviceA).name("Fridge").type("SPEAKER")
                        .location("Kitchen").userId(userId).energyConsumed(0.0).build(),
                DeviceDto.builder().id(deviceB).name("TV").type("SPEAKER")
                        .location("Living Room").userId(userId).energyConsumed(0.0).build()
        ));

        Point p1 = Point.measurement("energy_usage")
                .addTag("deviceId", String.valueOf(deviceA))
                .addField("energyConsumed", 300.0)
                .time(Instant.now(), WritePrecision.MS);
        Point p2 = Point.measurement("energy_usage")
                .addTag("deviceId", String.valueOf(deviceB))
                .addField("energyConsumed", 700.0)
                .time(Instant.now(), WritePrecision.MS);
        influxDBClient.getWriteApiBlocking().writePoints(bucket, org, List.of(p1, p2));

        ResponseEntity<UsageDto> response = restTemplate
                .getForEntity("/api/v1/usage/{userId}?days=1", UsageDto.class, userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().devices()).hasSize(2);
        assertThat(response.getBody().devices().stream()
                .mapToDouble(DeviceDto::energyConsumed).sum()).isEqualTo(1000.0);
    }

    @Test
    void getUsageForUser_noDataInInflux_returnsZeroUsage() {
        Long userId = 5L;
        Long deviceId = 50L;

        when(deviceClient.getAllDevicesForUser(userId))
                .thenReturn(List.of(DeviceDto.builder()
                        .id(deviceId).name("Sensor").type("CAMERA")
                        .location("Garden").userId(userId).energyConsumed(0.0)
                        .build()));

        ResponseEntity<UsageDto> response = restTemplate
                .getForEntity("/api/v1/usage/{userId}?days=1", UsageDto.class, userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().devices()).hasSize(1);
        assertThat(response.getBody().devices().get(0).energyConsumed()).isEqualTo(0.0);
    }

    @TestConfiguration
    static class TestMockConfiguration {

        @Bean
        @Primary
        DeviceClient deviceClient() {
            return Mockito.mock(DeviceClient.class);
        }

        @Bean
        @Primary
        UserClient userClient() {
            return Mockito.mock(UserClient.class);
        }
    }
}
