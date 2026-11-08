package si.contactgroups.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The OpenAPI description published at {@code /api/v1/openapi.json} (R-018-08, D-12): name, version
 * and the Firebase bearer scheme as in {@code docs/02_contracts/openapi.yaml}; the operations come
 * from the controllers.
 */
@Configuration
public class OpenApiConfig {

  private static final String BEARER = "bearerAuth";

  @Bean
  public OpenAPI contactGroupsApi() {
    return new OpenAPI()
        .info(new Info().title("Contact Groups API").version("1.0.0"))
        .addServersItem(new Server().url("/api/v1"))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("Firebase ID token")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER));
  }
}
