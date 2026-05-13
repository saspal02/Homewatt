package com.homewatt.usage_service.client;


import com.homewatt.usage_service.dto.DeviceDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class DeviceClient {

    private final RestClient restClient;
    private final String baseUrl;

    public DeviceClient(
            RestClient restClient,
            @Value("${device.service.url}") String baseUrl
    ) {

        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public DeviceDto getDeviceById(Long deviceId) {

        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/{deviceId}")
                .buildAndExpand(deviceId)
                .toUriString();

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(DeviceDto.class);
    }

    public List<DeviceDto> getAllDevicesForUser(Long userId) {

        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/user/{userId}")
                .buildAndExpand(userId)
                .toUriString();

        DeviceDto[] devices = restClient.get()
                .uri(url)
                .retrieve()
                .body(DeviceDto[].class);

        return devices == null
                ? List.of()
                : List.of(devices);
    }
}