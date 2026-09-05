package com.voyage.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger UI metadata. Declares a bearer-JWT scheme so the Swagger UI
 * "Authorize" button sends {@code Authorization: Bearer <token>}.
 * Docs live at {@code /swagger-ui.html} and {@code /v3/api-docs}.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI voyageOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Voyage API")
                        .description("공동 여행 플래너 백엔드 API")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
