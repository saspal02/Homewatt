package com.homewatt.alert_service.integration;

import com.homewatt.alert_service.entity.Alert;
import com.homewatt.alert_service.repository.AlertRepository;
import com.homewatt.alert_service.testsupport.ContainersBase;
import com.homewatt.kafka.event.AlertingEvent;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class AlertServiceIntegrationTest extends ContainersBase {

    @Autowired
    private KafkaTemplate<String, AlertingEvent> kafkaTemplate;

    @Autowired
    private AlertRepository alertRepository;

    private static final Logger log = LoggerFactory.getLogger(AlertServiceIntegrationTest.class);

    @BeforeEach
    void cleanUp() {
        alertRepository.deleteAll();
    }

    @Test
    void alertingEvent_consumedAndAlertPersisted() {
        AlertingEvent event = AlertingEvent.builder()
                .userId(1L)
                .message("Energy consumption threshold exceeded")
                .threshold(2000.0)
                .energyConsumed(2500.0)
                .email("user@example.com")
                .build();

        log.info("Sent alert event for user {}, waiting for consumption...", event.getUserId());
        kafkaTemplate.send("energy-alerts", event).join();

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> !alertRepository.findAll().isEmpty());

        List<Alert> alerts = alertRepository.findAll();
        assertThat(alerts).hasSize(1);
        Alert alert = alerts.get(0);
        assertThat(alert.getUserId()).isEqualTo(1L);
        assertThat(alert.isSent()).isFalse();
        assertThat(alert.getCreatedAt()).isNotNull();
    }

    @Test
    void multipleAlertingEvents_allPersisted() {
        kafkaTemplate.send("energy-alerts", AlertingEvent.builder()
                .userId(10L).message("Alert 1").threshold(100.0)
                .energyConsumed(150.0).email("a@b.com").build()).join();
        kafkaTemplate.send("energy-alerts", AlertingEvent.builder()
                .userId(20L).message("Alert 2").threshold(200.0)
                .energyConsumed(300.0).email("c@d.com").build()).join();

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> alertRepository.findAll().size() >= 2);

        assertThat(alertRepository.findAll()).hasSize(2);
    }
}
