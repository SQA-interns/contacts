package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.TestUser;

/** F-42 (fix loop 1): R-006-07, one History entry per affected Contact, no version change. */
class FixRun1TagIT extends AcceptanceTestBase {

  private TestUser ana;
  private TestUser boris;
  private Grp acme;

  @BeforeEach
  void anaOwnsAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
    boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
  }

  private String contact(String name, String... tags) {
    return createContact(ana, acme, "firstName", name, "tags", Json.arr((Object[]) tags))
        .path("id")
        .asText();
  }

  private long entries(String contactId, String action) {
    return history(ana, acme, contactId).stream()
        .filter(e -> e.path("action").asText().equals(action))
        .count();
  }

  @Test
  @DisplayName("R-006-07 a rename records an entry for a Contact in Trash too, versions unchanged")
  void rename_covers_a_contact_in_trash() {
    String live = contact("Živa", "gost");
    String trashed = contact("Tomaž", "gost");
    moveToTrash(ana, acme, trashed);
    int liveVersion = getContact(ana, acme, live).path("version").asInt();

    boris.patch(acme.tag(tagId(ana, acme, "gost")), Json.obj("name", "obiskovalec")).expect(200);

    assertThat(entries(live, "TAG_RENAMED")).isOne();
    assertThat(entries(trashed, "TAG_RENAMED")).isOne();
    assertThat(getContact(ana, acme, live).path("version").asInt()).isEqualTo(liveVersion);
    JsonNode entry =
        history(ana, acme, live).stream()
            .filter(e -> e.path("action").asText().equals("TAG_RENAMED"))
            .findFirst()
            .orElseThrow();
    assertThat(entry.path("userName").asText()).isEqualTo("Boris Kos");
    assertThat(entry.path("details").path("old").asText()).isEqualTo("gost");
    assertThat(entry.path("details").path("new").asText()).isEqualTo("obiskovalec");
  }

  @Test
  @DisplayName("R-006-07 a delete records one entry per Contact with the name; versions unchanged")
  void delete_records_one_entry_per_contact() {
    String a = contact("A", "gost");
    String b = contact("B", "gost", "vip");
    String c = contact("C", "vip");
    moveToTrash(ana, acme, b);
    int beforeA = getContact(ana, acme, a).path("version").asInt();

    boris.delete(acme.tag(tagId(ana, acme, "gost"))).expect(204);

    assertThat(entries(a, "TAG_DELETED")).isOne();
    assertThat(entries(b, "TAG_DELETED")).isOne();
    assertThat(entries(c, "TAG_DELETED")).isZero();
    assertThat(getContact(ana, acme, a).path("version").asInt()).isEqualTo(beforeA);
    JsonNode entry =
        history(ana, acme, a).stream()
            .filter(e -> e.path("action").asText().equals("TAG_DELETED"))
            .findFirst()
            .orElseThrow();
    assertThat(entry.path("userName").asText()).isEqualTo("Boris Kos");
    assertThat(entry.path("details").toString()).contains("gost");
  }

  @Test
  @DisplayName("R-006-07 a case-only rename records the old and the new spelling")
  void case_only_rename_records_both_spellings() {
    String a = contact("A", "gost");

    ana.patch(acme.tag(tagId(ana, acme, "gost")), Json.obj("name", "GOST")).expect(200);

    List<JsonNode> renamed =
        history(ana, acme, a).stream()
            .filter(e -> e.path("action").asText().equals("TAG_RENAMED"))
            .toList();
    assertThat(renamed).hasSize(1);
    assertThat(renamed.get(0).path("details").path("old").asText()).isEqualTo("gost");
    assertThat(renamed.get(0).path("details").path("new").asText()).isEqualTo("GOST");
  }

  @Test
  @DisplayName("R-006-07 a rename to the same name records nothing")
  void a_rename_to_the_same_name_records_nothing() {
    String a = contact("A", "gost");
    int before = history(ana, acme, a).size();

    ana.patch(acme.tag(tagId(ana, acme, "gost")), Json.obj("name", "gost")).expect(200);

    assertThat(history(ana, acme, a)).hasSize(before);
  }

  @Test
  @DisplayName("R-006-07 the entries show in the Group activity list, one per Contact")
  void the_group_activity_shows_one_entry_per_contact() {
    contact("A", "gost");
    contact("B", "gost");
    contact("C", "gost");

    ana.patch(acme.tag(tagId(ana, acme, "gost")), Json.obj("name", "gostje")).expect(200);

    assertThat(activity(ana, acme, "action=TAG_RENAMED")).hasSize(3);
  }

  @Test
  @DisplayName("R-006-07 the Contact's own save after a rename still moves the version by one")
  void a_later_save_raises_the_version_by_one() {
    String a = contact("A", "gost");
    ana.patch(acme.tag(tagId(ana, acme, "gost")), Json.obj("name", "gostje")).expect(200);

    JsonNode saved = edit(ana, acme, a, "city", "Celje");

    assertThat(saved.path("version").asInt()).isEqualTo(2);
  }
}
