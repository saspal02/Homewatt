package com.homewatt.ingestion_service;

import com.homewatt.ingestion_service.dto.EnergyUsageDto;
import com.homewatt.ingestion_service.service.IngestionService;
import com.homewatt.kafka.event.EnergyUsageEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
@EmbeddedKafka(topics = "energy-usage", partitions = 1)
class IngestionServiceKafkaIntegrationTest {

    @Value("${spring.embedded.kafka.brokers}")
    private String embeddedKafkaBrokers;

    @Autowired
    private IngestionService ingestionService;

    @Test
    void shouldSendMessageToKafka() {
        EnergyUsageDto dto = EnergyUsageDto.builder()
                .deviceId(10L)
                .energyConsumed(2500.0)
                .timestamp(Instant.now())
                .build();

        ingestionService.ingestEnergyUsage(dto);

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBrokers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, EnergyUsageEvent> consumer =
                     new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(List.of("energy-usage"));
            ConsumerRecords<String, EnergyUsageEvent> records =
                    consumer.poll(Duration.ofSeconds(5));

            assertThat(records.count()).isGreaterThanOrEqualTo(1);
            EnergyUsageEvent received = records.iterator().next().value();
            assertThat(received.deviceId()).isEqualTo(10L);
            assertThat(received.energyConsumed()).isEqualTo(2500.0);
        }
    }
}
