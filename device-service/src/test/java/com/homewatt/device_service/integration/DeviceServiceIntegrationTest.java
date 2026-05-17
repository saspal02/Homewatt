package com.homewatt.device_service.integration;

import com.homewatt.device_service.dto.DeviceDto;
import com.homewatt.device_service.entity.Device;
import com.homewatt.device_service.model.DeviceType;
import com.homewatt.device_service.repository.DeviceRepository;
import com.homewatt.device_service.testsupport.MySqlTestcontainersBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class DeviceServiceIntegrationTest extends MySqlTestcontainersBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DeviceRepository deviceRepository;

    @Test
    void createDevice_viaRestApi_persistsAndReturnsDevice() {
        DeviceDto request = new DeviceDto(null, "Thermostat", DeviceType.THERMOSTAT, "Living Room", 1L);

        ResponseEntity<DeviceDto> response = restTemplate.postForEntity("/api/v1/device/create", request, DeviceDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Thermostat");
        assertThat(response.getBody().type()).isEqualTo(DeviceType.THERMOSTAT);
        assertThat(response.getBody().location()).isEqualTo("Living Room");
        assertThat(response.getBody().userId()).isEqualTo(1L);

        ResponseEntity<DeviceDto> loaded = restTemplate.getForEntity("/api/v1/device/" + response.getBody().id(), DeviceDto.class);
        assertThat(loaded.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loaded.getBody()).isNotNull();
        assertThat(loaded.getBody().name()).isEqualTo("Thermostat");
    }

    @Test
    void saveDevice_viaRepository_roundTripsThroughMysql() {
        Device saved = deviceRepository.save(Device.builder()
                .name("Camera")
                .type(DeviceType.CAMERA)
                .location("Front Door")
                .userId(2L)
                .build());

        assertThat(saved.getId()).isNotNull();

        Device fromDb = deviceRepository.findById(saved.getId()).orElseThrow();
        assertThat(fromDb.getName()).isEqualTo("Camera");
        assertThat(fromDb.getType()).isEqualTo(DeviceType.CAMERA);
        assertThat(fromDb.getLocation()).isEqualTo("Front Door");
        assertThat(fromDb.getUserId()).isEqualTo(2L);
    }

    @Test
    void updateDevice_viaRestApi_persistsChanges() {
        DeviceDto createRequest = new DeviceDto(null, "Lock", DeviceType.LOCK, "Main Door", 1L);
        ResponseEntity<DeviceDto> created = restTemplate.postForEntity("/api/v1/device/create", createRequest, DeviceDto.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(created.getBody()).isNotNull();
        Long id = created.getBody().id();

        DeviceDto updateRequest = new DeviceDto(id, "Smart Lock", DeviceType.LOCK, "Back Door", 2L);
        ResponseEntity<DeviceDto> updateResponse = restTemplate.exchange(
                "/api/v1/device/" + id, HttpMethod.PUT, new HttpEntity<>(updateRequest), DeviceDto.class);

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).isNotNull();
        assertThat(updateResponse.getBody().name()).isEqualTo("Smart Lock");
        assertThat(updateResponse.getBody().location()).isEqualTo("Back Door");
        assertThat(updateResponse.getBody().userId()).isEqualTo(2L);
    }

    @Test
    void deleteDevice_viaRestApi_removesDevice() {
        DeviceDto createRequest = new DeviceDto(null, "Speaker", DeviceType.SPEAKER, "Kitchen", 1L);
        ResponseEntity<DeviceDto> created = restTemplate.postForEntity("/api/v1/device/create", createRequest, DeviceDto.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(created.getBody()).isNotNull();
        Long id = created.getBody().id();

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/v1/device/" + id, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> afterDelete = restTemplate.getForEntity("/api/v1/device/" + id, String.class);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(afterDelete.getBody()).contains("Device not found");
    }

    @Test
    void getDevicesByUserId_returnsDeviceList() {
        deviceRepository.save(Device.builder().name("Light-1").type(DeviceType.LIGHT).location("Hall").userId(10L).build());
        deviceRepository.save(Device.builder().name("Light-2").type(DeviceType.LIGHT).location("Bedroom").userId(10L).build());
        deviceRepository.save(Device.builder().name("Doorbell").type(DeviceType.DOORBELL).location("Gate").userId(20L).build());

        ResponseEntity<DeviceDto[]> response = restTemplate.getForEntity("/api/v1/device/user/10", DeviceDto[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isEqualTo(2);
    }
}
