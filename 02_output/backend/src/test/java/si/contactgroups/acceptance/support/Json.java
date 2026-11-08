package si.contactgroups.acceptance.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/** Small JSON helpers for request bodies and answers. */
public final class Json {

  public static final ObjectMapper MAPPER = new ObjectMapper();

  private Json() {}

  /** An object from key-value pairs; a {@code null} value is written as JSON null. */
  public static ObjectNode obj(Object... keyValues) {
    if (keyValues.length % 2 != 0) {
      throw new IllegalArgumentException("key-value pairs expected");
    }
    ObjectNode node = MAPPER.createObjectNode();
    for (int i = 0; i < keyValues.length; i += 2) {
      node.set((String) keyValues[i], MAPPER.valueToTree(keyValues[i + 1]));
    }
    return node;
  }

  public static ArrayNode arr(Object... values) {
    ArrayNode node = MAPPER.createArrayNode();
    for (Object value : values) {
      node.add(MAPPER.<JsonNode>valueToTree(value));
    }
    return node;
  }

  public static JsonNode parse(String text) {
    try {
      return MAPPER.readTree(text == null || text.isEmpty() ? "null" : text);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static String write(Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static List<JsonNode> list(JsonNode array) {
    if (array == null || !array.isArray()) {
      return List.of();
    }
    return StreamSupport.stream(array.spliterator(), false).toList();
  }

  /** The text values of a JSON array of strings. */
  public static List<String> strings(JsonNode array) {
    List<String> out = new ArrayList<>();
    for (JsonNode node : list(array)) {
      out.add(node.asText());
    }
    return out;
  }

  /** The values of one text field of every element of a JSON array. */
  public static List<String> field(JsonNode array, String name) {
    List<String> out = new ArrayList<>();
    for (JsonNode node : list(array)) {
      out.add(node.path(name).isNull() ? null : node.path(name).asText(null));
    }
    return out;
  }
}
