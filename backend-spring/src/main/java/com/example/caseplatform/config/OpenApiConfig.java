package com.example.caseplatform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI casePlatformOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Case Platform API")
                        .description("Case and order management service")
                        .version("v1")
                        .contact(new Contact().name("Order Case Platform")));
    }
}
