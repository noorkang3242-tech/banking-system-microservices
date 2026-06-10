package com.bank.transaction.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transactionOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Transaction Service API")
                .version("v1")
                .description("Transaction history/ledger for the Banking System"));
    }
}
