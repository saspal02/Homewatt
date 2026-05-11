package com.homewatt.ingestion_service.simulation;

import com.homewatt.ingestion_service.dto.EnergyUsageDto;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class ParallelDataSimulator implements CommandLineRunner {

    private final RestClient restClient;

    @Value("${simulation.parallel-threads}")
    private int parallelThreads;

    @Value("${simulation.requests-per-interval}")
    private int requestsPerInterval;

    @Value("${simulation.endpoint}")
    private String ingestionEndpoint;

    private ExecutorService executorService;

    public ParallelDataSimulator(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void run(String... args) {
        this.executorService = Executors.newFixedThreadPool(parallelThreads);

        log.info("""
                
                ParallelDataSimulator started
                Threads: {}
                Requests Per Interval: {}
                Endpoint: {}
                
                """,
                parallelThreads,
                requestsPerInterval,
                ingestionEndpoint
        );
    }

    @Scheduled(fixedRateString = "${simulation.interval-ms}")
    public void sendMockData() {

        int batchSize = requestsPerInterval / parallelThreads;
        int remainder = requestsPerInterval % parallelThreads;

        for (int i = 0; i < parallelThreads; i++) {

            int requestsForThread = batchSize + (i < remainder ? 1 : 0);

            executorService.submit(() -> {

                for (int j = 0; j < requestsForThread; j++) {

                    EnergyUsageDto dto = EnergyUsageDto.builder()
                            .deviceId(
                                    ThreadLocalRandom.current()
                                            .nextLong(1, 201)
                            )
                            .energyConsumed(
                                    Math.round(
                                            ThreadLocalRandom.current()
                                                    .nextDouble(0.0, 2.0) * 100.0
                                    ) / 100.0
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

                        log.error(
                                "Failed to send data for deviceId {}",
                                dto.deviceId(),
                                e
                        );
                    }
                }
            });
        }
    }

    @PreDestroy
    public void shutdown() {

        if (executorService != null) {
            executorService.shutdown();
        }

        log.info("ParallelDataSimulator shut down.");
    }
}