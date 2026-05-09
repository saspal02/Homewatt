package com.homewatt.device_service.service;

import com.homewatt.device_service.dto.DeviceDto;
import com.homewatt.device_service.entity.Device;
import com.homewatt.device_service.exception.DeviceNotFoundException;
import com.homewatt.device_service.repository.DeviceRepository;
import org.springframework.stereotype.Service;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public DeviceDto getDeviceById(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() ->
                        new DeviceNotFoundException("Device not found with id " + id));

        return mapToDto(device);
    }

    public DeviceDto createDevice(DeviceDto input) {
        Device device = new Device();

        device.setName(input.name());
        device.setType(input.type());
        device.setLocation(input.location());
        device.setUserId(input.userId());

        Device savedDevice = deviceRepository.save(device);

        return mapToDto(savedDevice);
    }

    public DeviceDto updateDevice(Long id, DeviceDto input) {
        Device existing = deviceRepository.findById(id)
                .orElseThrow(() ->
                        new DeviceNotFoundException("Device not found with id " + id));

        existing.setName(input.name());
        existing.setType(input.type());
        existing.setLocation(input.location());
        existing.setUserId(input.userId());

        Device updatedDevice = deviceRepository.save(existing);

        return mapToDto(updatedDevice);
    }

    public void deleteDevice(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() ->
                        new DeviceNotFoundException("Device not found with id " + id));

        deviceRepository.delete(device);
    }

    private DeviceDto mapToDto(Device device) {
        return new DeviceDto(
                device.getId(),
                device.getName(),
                device.getType(),
                device.getLocation(),
                device.getUserId()
        );
    }
}

