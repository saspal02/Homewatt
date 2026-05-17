package com.homewatt.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "keycloak.auth.jwk-set-uri=http://localhost:0/realms/homewatt/protocol/openid-connect/certs",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:0/realms/homewatt"
})
class GatewayRouteTest {

    @Test
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
