package si.contactgroups.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import si.contactgroups.acceptance.support.Contract;
import si.contactgroups.acceptance.support.Contract.Operation;
import si.contactgroups.domain.User;
import si.contactgroups.support.WebTestSupport;

/**
 * R-018-08, AC-018-10, DoD-P02: the OpenAPI description the system publishes at {@code
 * /api/v1/openapi.json} agrees with the contract {@code docs/02_contracts/openapi.yaml} for every
 * operation that exists. The frozen {@code Us018SecurityAcceptanceIT} checks that every operation
 * of the contract is published, which holds when the last story is done.
 */
class ContractParityIT extends WebTestSupport {

  private JsonNode published(User user) throws Exception {
    String body =
        mvc.perform(get("/api/v1/openapi.json").with(as(user)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return new ObjectMapper().readTree(body);
  }

  /** "METHOD shape" to operation id of the published description. */
  private static Map<String, String> operationsOf(JsonNode description) {
    Map<String, String> out = new HashMap<>();
    Iterator<Map.Entry<String, JsonNode>> paths = description.path("paths").properties().iterator();
    while (paths.hasNext()) {
      Map.Entry<String, JsonNode> path = paths.next();
      Iterator<Map.Entry<String, JsonNode>> methods = path.getValue().properties().iterator();
      while (methods.hasNext()) {
        Map.Entry<String, JsonNode> method = methods.next();
        out.put(
            method.getKey().toUpperCase(Locale.ROOT) + " " + Contract.normalise(path.getKey()),
            method.getValue().path("operationId").asText());
      }
    }
    return out;
  }

  @Test
  void r_018_08_the_description_is_an_openapi_3_document_with_the_operations_that_exist()
      throws Exception {
    JsonNode description = published(newUser("Ana Novak"));

    assertThat(description.path("openapi").asText()).startsWith("3.");
    assertThat(operationsOf(description)).containsKeys("GET /groups", "POST /groups", "GET /me");
  }

  @Test
  void dod_p02_every_published_operation_is_in_the_contract_with_the_same_operation_id()
      throws Exception {
    Map<String, String> contract = new HashMap<>();
    for (Operation op : Contract.operations()) {
      contract.put(op.method() + " " + op.shape(), op.operationId());
    }

    List<String> differences = new ArrayList<>();
    operationsOf(published(newUser("Ana Novak")))
        .forEach(
            (operation, id) -> {
              if (!id.equals(contract.get(operation))) {
                differences.add(operation + ": " + id + " but contract " + contract.get(operation));
              }
            });

    assertThat(differences).isEmpty();
  }

  @Test
  void r_018_08_the_description_names_the_api_and_the_bearer_scheme_of_the_contract()
      throws Exception {
    JsonNode description = published(newUser("Ana Novak"));

    assertThat(description.path("info").path("title").asText()).isEqualTo("Contact Groups API");
    assertThat(description.path("info").path("version").asText()).isEqualTo("1.0.0");
    JsonNode bearer = description.path("components").path("securitySchemes").path("bearerAuth");
    assertThat(bearer.path("type").asText()).isEqualTo("http");
    assertThat(bearer.path("scheme").asText()).isEqualTo("bearer");
    assertThat(description.path("security").toString()).contains("bearerAuth");
  }
}
