package com.homewatt.device_service.repository;

import com.homewatt.device_service.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {
}
