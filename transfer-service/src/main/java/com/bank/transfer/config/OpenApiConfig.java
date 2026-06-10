package com.bank.transfer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transferOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Transfer Service API")
                .version("v1")
                .description("Fund transfers between accounts (saga) for the Banking System"));
    }
}
