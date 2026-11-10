package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.TestUser;

/** F-44 (fix loop 1): R-017-02 for cases the frozen tests do not contain. */
class FixRun1ActivityIT extends AcceptanceTestBase {

  private TestUser ana;
  private TestUser boris;
  private Grp acme;

  @BeforeEach
  void anaAndBorisInAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
    boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
  }

  private JsonNode page(String query) {
    return ana.get(acme.activity(query)).expect(200).json();
  }

  @Test
  @DisplayName("R-017-02 the total and the pages count only the entries of the chosen user")
  void total_and_pages_follow_the_user() {
    for (int i = 0; i < 30; i++) {
      createContact(ana, acme, "firstName", "A" + i);
    }
    for (int i = 0; i < 3; i++) {
      createContact(boris, acme, "firstName", "B" + i);
    }

    JsonNode anas = page("userId=" + ana.userId() + "&action=CONTACT_CREATED");
    JsonNode anasSecond = page("userId=" + ana.userId() + "&action=CONTACT_CREATED&page=2");
    JsonNode boriss = page("userId=" + boris.userId() + "&action=CONTACT_CREATED");

    assertThat(anas.path("total").asInt()).isEqualTo(30);
    assertThat(anas.path("items")).hasSize(25);
    assertThat(anasSecond.path("items")).hasSize(5);
    assertThat(boriss.path("total").asInt()).isEqualTo(3);
    assertThat(boriss.path("items")).allMatch(e -> e.path("userName").asText().equals("Boris Kos"));
  }

  @Test
  @DisplayName("R-017-02 a user without entries in the Group gives an empty page, not an error")
  void a_user_without_entries_gives_an_empty_page() {
    TestUser cene = user("Cene Zupan", "cene@beta.com");
    group(cene, "Beta");
    createContact(ana, acme, "firstName", "A");

    JsonNode answer = page("userId=" + cene.userId());

    assertThat(answer.path("total").asInt()).isZero();
    assertThat(answer.path("items")).isEmpty();
  }

  @Test
  @DisplayName("R-017-02 a user id in capital letters chooses the same user")
  void a_capital_letter_user_id_works() {
    createContact(boris, acme, "firstName", "B");

    List<JsonNode> entries =
        activity(ana, acme, "userId=" + boris.userId().toUpperCase() + "&action=CONTACT_CREATED");

    assertThat(entries).hasSize(1);
  }

  @Test
  @DisplayName("R-017-02 a user who deleted the account can still be chosen")
  void a_user_with_a_deleted_account_can_still_be_chosen() {
    createContact(boris, acme, "firstName", "B");
    String borisId = boris.userId();
    boris.delete("/me").expect(204);

    List<JsonNode> entries = activity(ana, acme, "userId=" + borisId);

    assertThat(actions(entries)).contains("CONTACT_CREATED");
    assertThat(entries).allMatch(e -> e.path("userName").asText().equals("Boris Kos"));
  }

  @Test
  @DisplayName("R-017-02 a user id that is not a UUID is refused with 400")
  void a_malformed_user_id_is_400() {
    ana.get(acme.activity("userId=ana")).expectError(400, "request.malformed");
  }
}
