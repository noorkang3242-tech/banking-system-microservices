package com.bank.card.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cardOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Card Service API")
                .version("v1")
                .description("Debit/credit card issuing and management for the Banking System"));
    }
}
