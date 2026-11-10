package si.contactgroups.config;

import java.util.List;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The health check answers {@code application/json} and nothing else (R-018-07, R-018-15); the
 * default of the actuator is its own vendor type, which a client that expects JSON may refuse.
 */
@Configuration
public class HealthMediaTypeConfig {

  private static final List<String> JSON = List.of("application/json");

  @Bean
  EndpointMediaTypes endpointMediaTypes() {
    return new EndpointMediaTypes(JSON, JSON);
  }
}
