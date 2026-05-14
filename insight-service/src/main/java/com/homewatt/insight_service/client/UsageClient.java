package com.homewatt.insight_service.client;


import com.homewatt.insight_service.dto.UsageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class UsageClient {

    private final RestClient restClient;
    private final String baseUrl;

    public UsageClient(
            RestClient restClient,
            @Value("${usage.service.url}") String baseUrl
    ) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public UsageDto getXDaysUsageForUser(Long userId, int days) {

        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/{userId}")
                .queryParam("days", days)
                .buildAndExpand(userId)
                .toUriString();

        return restClient
                .get()
                .uri(url)
                .retrieve()
                .body(UsageDto.class);
    }
}