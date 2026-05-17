package com.homewatt.usage_service.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

public class ContainersBase {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Container
    static GenericContainer<?> influxDb = new GenericContainer<>("influxdb:2.7")
            .withEnv("DOCKER_INFLUXDB_INIT_MODE", "setup")
            .withEnv("DOCKER_INFLUXDB_INIT_USERNAME", "admin")
            .withEnv("DOCKER_INFLUXDB_INIT_PASSWORD", "password")
            .withEnv("DOCKER_INFLUXDB_INIT_ORG", "homewatt")
            .withEnv("DOCKER_INFLUXDB_INIT_BUCKET", "usage-bucket")
            .withEnv("DOCKER_INFLUXDB_INIT_ADMIN_TOKEN", "my-token")
            .withExposedPorts(8086);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("influx.url",
                () -> "http://" + influxDb.getHost() + ":" + influxDb.getMappedPort(8086));
    }
}
