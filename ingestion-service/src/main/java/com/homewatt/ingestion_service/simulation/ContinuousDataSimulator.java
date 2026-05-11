package com.homewatt.ingestion_service.simulation;

import com.homewatt.ingestion_service.dto.EnergyUsageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Random;

@Slf4j
@Component
public class ContinuousDataSimulator implements CommandLineRunner {

    private final RestClient restClient;
    private final Random random = new Random();

    @Value("${simulation.requests-per-interval}")
    private int requestPerInterval;

    @Value("${simulation.endpoint}")
    private String ingestionEndpoint;

    public ContinuousDataSimulator(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void run(String... args) {
        log.info("ContinuousDataSimulator started...");
    }

    //@Scheduled(fixedRateString = "${simulation.interval-ms}")
    public void sendMockData() {

        for (int i = 0; i < requestPerInterval; i++) {

            EnergyUsageDto dto = EnergyUsageDto.builder()
                    .deviceId(random.nextLong(1, 6))
                    .energyConsumed(
                            Math.round(random.nextDouble(0.0, 10.0) * 100.0) / 100.0
                    )
                    .timestamp(Instant.now())
                    .build();

            try {

                restClient.post()
                        .uri(ingestionEndpoint)
                        .body(dto)
                        .retrieve()
                        .toBodilessEntity();

                log.info("Sent mock data: {}", dto);

            } catch (Exception e) {
                log.error("Failed to send data", e);
            }
        }
    }
}