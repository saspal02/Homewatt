package com.homewatt.usage_service.client;


import com.homewatt.usage_service.dto.UserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class UserClient {

    private final RestClient restClient;
    private final String baseUrl;

    public UserClient(
            RestClient restClient,
            @Value("${user.service.url}") String baseUrl
    ) {

        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public UserDto getUserById(Long userId) {

        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/{userId}")
                .buildAndExpand(userId)
                .toUriString();

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(UserDto.class);
    }
}