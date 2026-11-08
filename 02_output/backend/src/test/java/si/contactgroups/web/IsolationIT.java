package si.contactgroups.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import si.contactgroups.domain.User;
import si.contactgroups.support.WebTestSupport;

/**
 * AC-018-04, SR-01 over the endpoints that exist: every mapped operation with a Group in its path
 * answers a User who is no Member with 404 {@code group.not_found} and no data. The list comes from
 * the handler mapping, so a new endpoint is covered the day it exists; the frozen acceptance test
 * does the same over the whole contract at the release (D-23c).
 */
class IsolationIT extends WebTestSupport {

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping mappings;

  private record Call(String method, String path) {}

  private List<Call> groupOperations(UUID groupId) {
    List<Call> calls = new ArrayList<>();
    for (RequestMappingInfo info : mappings.getHandlerMethods().keySet()) {
      for (String pattern : info.getPathPatternsCondition().getPatternValues()) {
        if (pattern.contains("{groupId}")) {
          for (RequestMethod method : info.getMethodsCondition().getMethods()) {
            calls.add(new Call(method.name(), concrete(pattern, groupId)));
          }
        }
      }
    }
    return calls;
  }

  private static String concrete(String pattern, UUID groupId) {
    return pattern.replace("{groupId}", groupId.toString()).replaceAll("\\{[^}]+}", "1");
  }

  @Test
  void ac_018_04_every_operation_with_a_group_in_its_path_answers_a_non_member_404()
      throws Exception {
    User ana = newUser("Ana Novak");
    User boris = newUser("Boris Kos");
    UUID acme = UUID.fromString(createGroup(ana, "Acme"));
    List<Call> calls = groupOperations(acme);
    assertThat(calls).isNotEmpty();

    List<String> wrong = new ArrayList<>();
    for (Call call : calls) {
      var answer =
          mvc.perform(
                  request(HttpMethod.valueOf(call.method()), call.path())
                      .with(as(boris))
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{}"))
              .andReturn()
              .getResponse();
      String body = answer.getContentAsString();
      if (answer.getStatus() != 404 || !body.contains("group.not_found") || body.contains("Acme")) {
        wrong.add(call.method() + " " + call.path() + " -> " + answer.getStatus());
      }
    }

    assertThat(wrong).isEmpty();
  }

  private String createGroup(User owner, String name) throws Exception {
    String body =
        mvc.perform(
                request(HttpMethod.POST, "/api/v1/groups")
                    .with(as(owner))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + name + "\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return new ObjectMapper().readTree(body).path("id").asText();
  }
}
