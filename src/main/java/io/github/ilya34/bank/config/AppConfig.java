package io.github.ilya34.bank.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Bank Service API")
                .version("0.1.0")
                .description("Demo banking backend: customers, accounts, cards, transfers and a ledger. "
                        + "Educational project: no authentication, no real payments. "
                        + "Errors are returned as RFC 7807 problem details with a `code` field.")
                .contact(new Contact().name("Ilya Kolesnik").url("https://github.com/ilya-34"))
                .license(new License().name("MIT")));
    }
}
