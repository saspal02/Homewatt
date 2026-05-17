package com.homewatt.user_service.integration;

import com.homewatt.user_service.dto.UserDto;
import com.homewatt.user_service.entity.User;
import com.homewatt.user_service.repository.UserRepository;
import com.homewatt.user_service.testsupport.MySqlTestcontainersBase;
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
public class UserServiceIntegrationTest extends MySqlTestcontainersBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createUser_viaRestApi_persistsAndReturnsUser() {
        UserDto request = UserDto.builder()
                .name("Leet")
                .surname("Journey")
                .email("leetjourney@gmail.com")
                .address("123 Coding St")
                .alerting(true)
                .energyAlertingThreshold(2000.0)
                .build();

        ResponseEntity<UserDto> response =
                restTemplate.postForEntity("/api/v1/user", request, UserDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Leet");
        assertThat(response.getBody().getSurname()).isEqualTo("Journey");
        assertThat(response.getBody().getAddress()).isEqualTo("123 Coding St");
        assertThat(response.getBody().getEmail()).isEqualTo("leetjourney@gmail.com");
        assertThat(response.getBody().isAlerting()).isTrue();
        assertThat(response.getBody().getEnergyAlertingThreshold()).isEqualTo(2000.0);

        ResponseEntity<UserDto> loaded =
                restTemplate.getForEntity("/api/v1/user/"
                        + response.getBody().getId(), UserDto.class);

        assertThat(loaded.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loaded.getBody()).isNotNull();
        assertThat(loaded.getBody().getEmail()).isEqualTo("leetjourney@gmail.com");
    }

    @Test
    void saveUser_viaRepository_roundTripsThroughMysql() {
        User saved = userRepository.save(User.builder()
                .name("Grace")
                .surname("Hopper")
                .email("grace.it@example.com")
                .address("2 Compiler Way")
                .alerting(false)
                .energyAlertingThreshold(900.0)
                .build());

        assertThat(saved.getId()).isNotNull();

        User fromDb = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(fromDb.getEmail()).isEqualTo("grace.it@example.com");
        assertThat(fromDb.getName()).isEqualTo("Grace");
        assertThat(fromDb.isAlerting()).isFalse();
        assertThat(fromDb.getEnergyAlertingThreshold()).isEqualTo(900.0);
    }


    @Test
    void updateUser_viaRestApi_persistsChanges() {

        UserDto createRequest = UserDto.builder()
                .name("Alan")
                .surname("Turing")
                .email("alan.update.it@example.com")
                .address("10 Bletchley Park")
                .alerting(true)
                .energyAlertingThreshold(500.0)
                .build();

        ResponseEntity<UserDto> created =
                restTemplate.postForEntity(
                        "/api/v1/user",
                        createRequest,
                        UserDto.class
                );

        assertThat(created.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        assertThat(created.getBody()).isNotNull();

        Long id = created.getBody().getId();

        UserDto updateRequest = UserDto.builder()
                .id(id)
                .name("Alan Mathison")
                .surname("Turing")
                .email("alan.update.it@example.com")
                .address("12 Wilmslow Rd")
                .alerting(false)
                .energyAlertingThreshold(750.0)
                .build();

        ResponseEntity<UserDto> updateResponse =
                restTemplate.exchange(
                        "/api/v1/user/" + id,
                        HttpMethod.PUT,
                        new HttpEntity<>(updateRequest),
                        UserDto.class
                );

        assertThat(updateResponse.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(updateResponse.getBody()).isNotNull();

        assertThat(updateResponse.getBody().getName())
                .isEqualTo("Alan Mathison");

        assertThat(updateResponse.getBody().getAddress())
                .isEqualTo("12 Wilmslow Rd");

        assertThat(updateResponse.getBody().isAlerting())
                .isFalse();

        assertThat(updateResponse.getBody()
                .getEnergyAlertingThreshold())
                .isEqualTo(750.0);
    }

    @Test
    void deleteUser_viaRestApi_removesUser() {

        UserDto createRequest = UserDto.builder()
                .name("Neha")
                .surname("Kapoor")
                .email("neha.kapoor@example.com")
                .address("9 Maple Residency")
                .alerting(false)
                .energyAlertingThreshold(400.0)
                .build();

        ResponseEntity<UserDto> created =
                restTemplate.postForEntity(
                        "/api/v1/user",
                        createRequest,
                        UserDto.class
                );

        assertThat(created.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        assertThat(created.getBody()).isNotNull();

        Long id = created.getBody().getId();

        ResponseEntity<Void> deleteResponse =
                restTemplate.exchange(
                        "/api/v1/user/" + id,
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        Void.class
                );

        assertThat(deleteResponse.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> afterDelete =
                restTemplate.getForEntity(
                        "/api/v1/user/" + id,
                        String.class
                );

        assertThat(afterDelete.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(afterDelete.getBody())
                .contains("User not found");
    }
}
