package si.contactgroups.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import si.contactgroups.domain.User;
import si.contactgroups.support.WebTestSupport;

/**
 * US-018 T-09: size of a body (R-018-11), JSON only (R-018-15), the error format (R-018-09) and
 * text stored as typed (R-018-14), on the endpoints that exist.
 */
class RequestLimitsIT extends WebTestSupport {

  private static final int ONE_MB = 1024 * 1024;

  /** A valid create body of exactly {@code size} bytes: a name and JSON whitespace. */
  private static byte[] groupBodyOfSize(int size) {
    String start = "{\"name\":\"Acme\"";
    String end = "}";
    int padding = size - start.length() - end.length();
    return (start + " ".repeat(padding) + end).getBytes(StandardCharsets.UTF_8);
  }

  private ResultActions create(User user, byte[] body) throws Exception {
    return mvc.perform(
        post("/api/v1/groups")
            .with(as(user))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  @Test
  void r_018_11_a_body_of_one_megabyte_plus_one_byte_is_refused_and_nothing_is_stored()
      throws Exception {
    User ana = newUser("Ana Novak");

    create(ana, groupBodyOfSize(ONE_MB + 1))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.code").value("request.too_large"))
        .andExpect(jsonPath("$.message").isNotEmpty());

    mvc.perform(get("/api/v1/groups").with(as(ana))).andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void ex_018_04_a_body_of_exactly_one_megabyte_is_accepted() throws Exception {
    User ana = newUser("Ana Novak");

    create(ana, groupBodyOfSize(ONE_MB)).andExpect(status().isCreated());
  }

  @Test
  void r_018_15_a_body_that_is_not_json_is_refused_in_the_error_format() throws Exception {
    User ana = newUser("Ana Novak");

    mvc.perform(
            post("/api/v1/groups")
                .with(as(ana))
                .contentType(MediaType.TEXT_PLAIN)
                .content("name=Acme"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("request.unsupported_media_type"));
    mvc.perform(
            post("/api/v1/groups")
                .with(as(ana))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("request.malformed"));
  }

  @Test
  void ex_018_06_a_method_that_is_not_allowed_on_a_path_answers_405_in_the_error_format()
      throws Exception {
    User ana = newUser("Ana Novak");

    mvc.perform(delete("/api/v1/groups").with(as(ana)))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("request.method_not_allowed"))
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  @Test
  void r_018_14_text_with_markup_is_stored_and_returned_as_typed() throws Exception {
    User ana = newUser("Ana Novak");
    String name = "<script>alert(1)</script>";

    create(ana, ("{\"name\":\"" + name + "\"}").getBytes(StandardCharsets.UTF_8))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value(name));

    mvc.perform(get("/api/v1/groups").with(as(ana))).andExpect(jsonPath("$[0].name").value(name));
  }
}
