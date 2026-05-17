package com.homewatt.device_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI deviceServiceApiDocs() {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Device Service API")
                        .description("Device service API for Home Energy Tracker Project")
                        .contact(getContact())
                        .version("1.0.0"));
    }

    private static Contact getContact() {
        Contact contact = new Contact();
        contact.setUrl("https://github.com/saspal02/Homewatt");
        return contact;
    }
}
