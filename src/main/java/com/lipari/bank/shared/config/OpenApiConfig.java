package com.lipari.bank.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lipariBankOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LipariBank API")
                        .description("LipariBank Day 08 — Error Handling Globale e Logging")
                        .version("1.0.0"));
    }
}
