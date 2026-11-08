package si.contactgroups.acceptance.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** The operations of the REST contract {@code docs/02_contracts/openapi.yaml}. */
public final class Contract {

  /**
   * One operation: method, path below {@code /api/v1} (or {@code /health}), request body or not.
   */
  public record Operation(String method, String path, boolean hasBody, String operationId) {

    /** The path with every parameter replaced; {@code groupId} by the given id. */
    public String concrete(String groupId) {
      return path.replace("{groupId}", groupId)
          .replace("{contactId}", Db.randomId())
          .replace("{tagId}", Db.randomId())
          .replace("{userId}", Db.randomId())
          .replace("{invitationId}", Db.randomId())
          .replace("{number}", "1");
    }

    /** The path with every parameter name replaced by {@code {}} (names may differ). */
    public String shape() {
      return normalise(path);
    }
  }

  private static final Set<String> METHODS = Set.of("get", "post", "put", "patch", "delete");
  private static final Path FILE = Path.of("..", "docs", "02_contracts", "openapi.yaml");

  private Contract() {}

  public static List<Operation> operations() {
    try {
      JsonNode root = new ObjectMapper(new YAMLFactory()).readTree(FILE.toFile());
      List<Operation> out = new ArrayList<>();
      Iterator<Map.Entry<String, JsonNode>> paths = root.path("paths").fields();
      while (paths.hasNext()) {
        Map.Entry<String, JsonNode> path = paths.next();
        Iterator<Map.Entry<String, JsonNode>> ops = path.getValue().fields();
        while (ops.hasNext()) {
          Map.Entry<String, JsonNode> op = ops.next();
          if (METHODS.contains(op.getKey())) {
            out.add(
                new Operation(
                    op.getKey().toUpperCase(Locale.ROOT),
                    path.getKey(),
                    op.getValue().has("requestBody"),
                    op.getValue().path("operationId").asText()));
          }
        }
      }
      return out;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * {@code /groups/{groupId}/x} and {@code /api/v1/groups/{g}/x} both become {@code /groups/{}/x}.
   */
  public static String normalise(String path) {
    String p = path.startsWith(Http.API) ? path.substring(Http.API.length()) : path;
    return p.replaceAll("\\{[^}]+}", "{}");
  }
}
