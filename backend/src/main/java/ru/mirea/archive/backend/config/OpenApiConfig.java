package ru.mirea.archive.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ИС «Архив документов» — REST API (КР-3)")
                        .version("3.0.0")
                        .description("Полнофункциональный REST API информационной системы учёта и выдачи архивных документов. " +
                                "Поддерживает аутентификацию по JWT, сессии refresh-токенов, оптимистичную блокировку, " +
                                "расширенные фильтры, двухэтапный CSV импорт/экспорт, аналитику и журнал аудита.")
                        .contact(new Contact().name("МИРЭА").email("support@mirea.ru"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Передайте JWT токен в формате: Bearer {token}")));
    }
}
