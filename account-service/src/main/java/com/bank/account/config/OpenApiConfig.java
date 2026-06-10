package com.bank.account.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI accountOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Account Service API")
                .version("v1")
                .description("Bank accounts, balances and deposit/withdraw for the Banking System"));
    }
}
