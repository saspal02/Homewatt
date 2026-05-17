package com.homewatt.device_service.testsupport;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;

public class MySqlTestcontainersBase {

    @ServiceConnection
    @Container
    static org.testcontainers.mysql.MySQLContainer mysql = new org.testcontainers.mysql.MySQLContainer("mysql:8.4")
            .withDatabaseName("homewatt_db")
            .withUsername("root")
            .withPassword("password");
}