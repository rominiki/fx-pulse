package dev.rominiki.fxpulse.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI fxPulseOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FX Pulse API")
                .version("0.4.0-dev")
                .description("Historical ECB currency reference rates stored in PostgreSQL. " +
                        "These are not real-time executable trading prices."));
    }
}
