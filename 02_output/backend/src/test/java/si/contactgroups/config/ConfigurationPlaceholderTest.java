package si.contactgroups.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * AR-07 / ES-01: environment-specific values in application.yml are placeholders, never literals.
 */
class ConfigurationPlaceholderTest {

  private static final List<String> MUST_BE_PLACEHOLDERS =
      List.of(
          "spring.datasource.url",
          "spring.datasource.username",
          "spring.datasource.password",
          "spring.flyway.url",
          "spring.flyway.user",
          "spring.flyway.password",
          "spring.flyway.placeholders.app_password",
          "app.firebase-project-id",
          "app.cors-allowed-origin",
          "server.port",
          "logging.level.root");

  @Test
  void environmentSpecificValuesComeFromEnvironmentVariables() throws Exception {
    Map<String, Object> flat = new java.util.TreeMap<>();
    try (InputStream in = getClass().getResourceAsStream("/application.yml")) {
      Object root = new Yaml().load(in);
      flatten("", root, flat);
    }
    List<String> literals = new ArrayList<>();
    for (String key : MUST_BE_PLACEHOLDERS) {
      Object value = flat.get(key);
      if (value == null || !String.valueOf(value).startsWith("${")) {
        literals.add(key + "=" + (value == null ? "missing" : "literal"));
      }
    }
    assertThat(literals).isEmpty();
  }

  @Test
  void secretsHaveNoDefault() throws Exception {
    Map<String, Object> flat = new java.util.TreeMap<>();
    try (InputStream in = getClass().getResourceAsStream("/application.yml")) {
      flatten("", new Yaml().load(in), flat);
    }
    for (String key :
        List.of(
            "spring.datasource.password",
            "spring.flyway.password",
            "spring.flyway.placeholders.app_password")) {
      assertThat(String.valueOf(flat.get(key))).as(key).doesNotContain(":");
    }
  }

  @SuppressWarnings("unchecked")
  private static void flatten(String prefix, Object node, Map<String, Object> out) {
    if (node instanceof Map<?, ?> map) {
      for (Map.Entry<String, Object> e : ((Map<String, Object>) map).entrySet()) {
        flatten(prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey(), e.getValue(), out);
      }
    } else {
      out.put(prefix, node);
    }
  }
}
