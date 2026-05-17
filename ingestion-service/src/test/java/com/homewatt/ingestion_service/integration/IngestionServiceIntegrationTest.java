package com.homewatt.ingestion_service.integration;

import com.homewatt.ingestion_service.dto.EnergyUsageDto;
import com.homewatt.ingestion_service.testsupport.KafkaTestcontainersBase;
import com.homewatt.kafka.event.EnergyUsageEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class IngestionServiceIntegrationTest extends KafkaTestcontainersBase {

    @Autowired
    private TestRestTemplate restTemplate;

    private Consumer<String, EnergyUsageEvent> testConsumer;

    @AfterEach
    void tearDown() {
        if (testConsumer != null) {
            testConsumer.close();
        }
    }

    @Test
    void ingestData_returnsAcceptedAndProducesKafkaMessage() {
        testConsumer = createConsumer("ingestion-test-group");
        testConsumer.subscribe(List.of("energy-usage"));

        EnergyUsageDto request = EnergyUsageDto.builder()
                .deviceId(42L)
                .energyConsumed(1500.0)
                .timestamp(Instant.parse("2026-05-17T10:00:00Z"))
                .build();

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/v1/ingestion", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        ConsumerRecords<String, EnergyUsageEvent> records =
                KafkaTestUtils.getRecords(testConsumer, Duration.ofSeconds(10));

        assertThat(records.count()).isGreaterThan(0);
        EnergyUsageEvent event = records.iterator().next().value();
        assertThat(event.deviceId()).isEqualTo(42L);
        assertThat(event.energyConsumed()).isEqualTo(1500.0);
        assertThat(event.timestamp()).isEqualTo(Instant.parse("2026-05-17T10:00:00Z"));
    }

    @Test
    void ingestMultiple_eventsAreProducedIndividually() {
        testConsumer = createConsumer("ingestion-multi-test");
        testConsumer.subscribe(List.of("energy-usage"));

        restTemplate.postForEntity("/api/v1/ingestion",
                EnergyUsageDto.builder().deviceId(1L).energyConsumed(100.0).timestamp(Instant.now()).build(),
                Void.class);
        restTemplate.postForEntity("/api/v1/ingestion",
                EnergyUsageDto.builder().deviceId(2L).energyConsumed(200.0).timestamp(Instant.now()).build(),
                Void.class);

        ConsumerRecords<String, EnergyUsageEvent> records =
                KafkaTestUtils.getRecords(testConsumer, Duration.ofSeconds(10));

        assertThat(records.count()).isGreaterThanOrEqualTo(2);
    }

    private Consumer<String, EnergyUsageEvent> createConsumer(String groupId) {

        Map<String, Object> props = new HashMap<>();

        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                getBootstrapServers()
        );

        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                groupId
        );

        props.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        props.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        props.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        JsonDeserializer<EnergyUsageEvent> valueDeserializer =
                new JsonDeserializer<>(EnergyUsageEvent.class);

        valueDeserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                valueDeserializer
        ).createConsumer();
    }
}
