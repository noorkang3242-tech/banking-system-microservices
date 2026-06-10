package com.bank.customer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Customer Service API")
                .version("v1")
                .description("Customer profiles and KYC for the Banking System"));
    }
}
