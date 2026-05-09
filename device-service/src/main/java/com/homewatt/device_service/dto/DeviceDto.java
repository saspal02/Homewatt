package com.homewatt.device_service.dto;

import com.homewatt.device_service.model.DeviceType;


public record DeviceDto(
        Long id,
        String name,
        DeviceType type,
        String location,
        Long userId) {}
