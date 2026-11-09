package si.contactgroups.service;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Replaces every value in the details of a History entry with {@value #REMOVED}, keeping the names
 * of the fields (R-013-08, ADR-08).
 */
@Component
public class HistoryRedactor {

  /** The text that stands for a removed value. */
  public static final String REMOVED = "removed";

  private final JsonMapper json;

  public HistoryRedactor(JsonMapper json) {
    this.json = json;
  }

  /** The JSON text of {@code detailsJson} with all values removed. */
  public String redact(String detailsJson) {
    return json.writeValueAsString(redact(json.readTree(detailsJson)));
  }

  private JsonNode redact(JsonNode node) {
    if (node.isObject()) {
      ObjectNode out = json.createObjectNode();
      node.properties().forEach(e -> out.set(e.getKey(), redact(e.getValue())));
      return out;
    }
    if (node.isArray()) {
      ArrayNode out = json.createArrayNode();
      node.forEach(item -> out.add(redact(item)));
      return out;
    }
    return json.getNodeFactory().stringNode(REMOVED);
  }
}
