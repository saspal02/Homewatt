package com.homewatt.alert_service.repository;

import com.homewatt.alert_service.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {
}
