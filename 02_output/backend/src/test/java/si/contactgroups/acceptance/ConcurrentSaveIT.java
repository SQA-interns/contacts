package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Resp;
import si.contactgroups.acceptance.support.TestUser;

/**
 * T-75, NFR-14 (F-50): two Users save the same Contact version at the same moment. Exactly one save
 * is stored; the other is refused with 409 and stores nothing (BR-07, R-003-07).
 */
class ConcurrentSaveIT extends AcceptanceTestBase {

  private TestUser ana;
  private TestUser boris;
  private Grp acme;

  @BeforeEach
  void anaAndBorisInAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
    boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
  }

  @RepeatedTest(5)
  @DisplayName("NFR-14 two saves of the same version at once: one stored, one 409, one new version")
  void two_saves_of_one_version_at_once() throws Exception {
    JsonNode current = createContact(ana, acme, "firstName", "Ana", "company", "Acme d.o.o.");
    String id = current.path("id").asText();

    List<Resp> answers =
        atOnce(
            () -> save(ana, acme, current, "company", "Beta d.o.o."),
            () -> save(boris, acme, current, "company", "Gamma d.o.o."));

    long stored = answers.stream().filter(a -> a.status() == 200).count();
    long refused = answers.stream().filter(a -> a.status() == 409).count();
    assertThat(stored).as("saves stored").isOne();
    assertThat(refused).as("saves refused with 409").isOne();
    JsonNode now = getContact(ana, acme, id);
    assertThat(now.path("version").asInt()).isEqualTo(2);
    String winner = answers.get(0).status() == 200 ? "Beta d.o.o." : "Gamma d.o.o.";
    assertThat(now.path("company").asText()).isEqualTo(winner);
    assertThat(
            history(ana, acme, id).stream()
                .filter(e -> e.path("action").asText().equals("CONTACT_EDITED")))
        .as("one edit entry only")
        .hasSize(1);
  }
}
