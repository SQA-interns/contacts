package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Emulator;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.Resp;
import si.contactgroups.acceptance.support.TestUser;

/** US-016 See the History of a Contact ({@code GET .../contacts/{c}/history}, 25 per page). */
class Us016HistoryAcceptanceIT extends AcceptanceTestBase {

  private TestUser ana;
  private TestUser boris;
  private Grp acme;

  @BeforeEach
  void anaAndBorisInAcme() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
    boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
  }

  private String anaNovak() {
    return createContact(ana, acme, "firstName", "Ana", "lastName", "Novak", "company", "Acme")
        .path("id")
        .asText();
  }

  /** All entries of every page, newest first. */
  private List<JsonNode> allPages(String contactId) {
    List<JsonNode> all = new ArrayList<>();
    for (int page = 1; page < 100; page++) {
      JsonNode answer = ana.get(acme.history(contactId) + "?page=" + page).expect(200).json();
      List<JsonNode> items = Json.list(answer.path("items"));
      if (items.isEmpty()) {
        break;
      }
      all.addAll(items);
    }
    return all;
  }

  @Test
  @DisplayName(
      "AC-016-01 creating Ana Novak gives one 'created' entry by Ana with the stored values")
  void ac_016_01_created_entry() {
    String id = anaNovak();

    List<JsonNode> entries = history(ana, acme, id);

    assertThat(entries).hasSize(1);
    JsonNode entry = entries.get(0);
    assertThat(entry.path("action").asText()).isEqualTo("CONTACT_CREATED");
    assertThat(entry.path("userName").asText()).isEqualTo("Ana Novak");
    assertThat(entry.path("userEmail").asText()).isEqualTo("ana@acme.com");
    assertThat(entry.path("contactId").asText()).isEqualTo(id);
    assertThat(entry.path("details").toString()).contains("Ana").contains("Novak").contains("Acme");
  }

  @Test
  @DisplayName(
      "AC-016-02 Boris's edit shows last name Novak → Kos and no line for the unchanged company")
  void ac_016_02_edit_lists_changed_fields_only() {
    String id = anaNovak();

    edit(boris, acme, id, "lastName", "Kos");

    JsonNode newest = history(ana, acme, id).get(0);
    assertThat(newest.path("action").asText()).isEqualTo("CONTACT_EDITED");
    assertThat(newest.path("userName").asText()).isEqualTo("Boris Kos");
    String details = newest.path("details").toString();
    assertThat(details)
        .contains("Novak")
        .contains("Kos")
        .doesNotContain("company")
        .doesNotContain("Acme");
  }

  @Test
  @DisplayName(
      "AC-016-03 entries of 2026-01-15 10:00 UTC and 2026-07-01 10:00 UTC are kept in UTC, newest first")
  void ac_016_03_times_in_utc_newest_first() {
    clock.setNow(Instant.parse("2026-01-15T10:00:00Z"));
    String id = anaNovak();
    clock.setNow(Instant.parse("2026-07-01T10:00:00Z"));
    edit(ana, acme, id, "city", "Kranj");

    List<JsonNode> entries = history(ana, acme, id);

    Instant newest = Instant.parse(entries.get(0).path("occurredAt").asText());
    Instant oldest = Instant.parse(entries.get(1).path("occurredAt").asText());
    assertThat(newest)
        .isBetween(Instant.parse("2026-07-01T10:00:00Z"), Instant.parse("2026-07-01T10:01:00Z"));
    assertThat(oldest)
        .isBetween(Instant.parse("2026-01-15T10:00:00Z"), Instant.parse("2026-01-15T10:01:00Z"));
  }

  @Test
  @DisplayName("AC-016-04 a Viewer's request for the History is refused")
  void ac_016_04_viewer_refused() {
    String id = anaNovak();
    TestUser viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");

    viola.get(acme.history(id)).expectError(403, "auth.role_forbidden");
  }

  @Test
  @DisplayName(
      "AC-016-05 a request by the Owner to change or delete an entry is refused; it stays unchanged")
  void ac_016_05_entries_cannot_be_changed() {
    String id = anaNovak();
    JsonNode entry = history(ana, acme, id).get(0);
    String entryId = entry.path("id").asText();

    List<Resp> answers =
        List.of(
            ana.put(acme.history(id), Json.obj("items", Json.arr())),
            ana.delete(acme.history(id)),
            ana.put(acme.history(id) + "/" + entryId, Json.obj("userName", "Nekdo")),
            ana.patch(acme.history(id) + "/" + entryId, Json.obj("userName", "Nekdo")),
            ana.delete(acme.history(id) + "/" + entryId));

    assertThat(answers).allMatch(a -> a.status() >= 400 && a.status() < 500);
    List<JsonNode> after = history(ana, acme, id);
    assertThat(after).hasSize(1);
    assertThat(after.get(0)).isEqualTo(entry);
  }

  @Test
  @DisplayName(
      "AC-016-06 an entry keeps the name and e-mail Boris had, after his removal and a new name")
  void ac_016_06_entry_keeps_name_of_the_time() {
    String id = anaNovak();
    edit(boris, acme, id, "city", "Celje");
    String borisId =
        Json.list(ana.get(acme.members()).expect(200).json()).stream()
            .filter(m -> m.path("email").asText().equals("boris@acme.com"))
            .findFirst()
            .orElseThrow()
            .path("userId")
            .asText();
    ana.delete(acme.member(borisId)).expect(204);
    Emulator.setDisplayName(boris.uid(), "Boris Novi");
    boris.refresh().get("/me").expect(200);

    JsonNode entry = history(ana, acme, id).get(0);

    assertThat(entry.path("userName").asText()).isEqualTo("Boris Kos");
    assertThat(entry.path("userEmail").asText()).isEqualTo("boris@acme.com");
  }

  @Test
  @DisplayName("AC-016-07 after delete and restore all earlier entries and both new ones are shown")
  void ac_016_07_history_after_restore() {
    String id = anaNovak();
    edit(ana, acme, id, "city", "Celje");
    moveToTrash(ana, acme, id);
    ana.post(acme.restore(id)).expect(200);

    assertThat(actions(history(ana, acme, id)))
        .containsExactly(
            "CONTACT_RESTORED", "CONTACT_DELETED", "CONTACT_EDITED", "CONTACT_CREATED");
  }

  @Test
  @DisplayName("AC-016-08 a save without changes adds no entry")
  void ac_016_08_no_change_no_entry() {
    String id = anaNovak();

    save(ana, acme, getContact(ana, acme, id)).expect(200);

    assertThat(history(ana, acme, id)).hasSize(1);
  }

  @Test
  @DisplayName("AC-016-09 the creation entry of 400 days ago is still listed on the last page")
  void ac_016_09_old_entries_kept() {
    clock.setNow(Instant.now().minus(Duration.ofDays(400)));
    String id = anaNovak();
    clock.reset();
    for (int i = 1; i <= 30; i++) {
      edit(ana, acme, id, "city", "Kraj" + i);
    }

    List<JsonNode> all = allPages(id);

    assertThat(all).hasSize(31);
    assertThat(all.get(all.size() - 1).path("action").asText()).isEqualTo("CONTACT_CREATED");
  }

  @Test
  @DisplayName("EX-016-01 typing ANA@x.si over ana@x.si changes nothing and adds no entry")
  void ex_016_01_email_case_only_no_entry() {
    String id = createContact(ana, acme, "email", "ana@x.si").path("id").asText();

    var saved = edit(ana, acme, id, "email", "ANA@x.si");

    assertThat(saved.path("version").asInt()).isEqualTo(1);
    assertThat(history(ana, acme, id)).hasSize(1);
  }

  @Test
  @DisplayName("EX-016-02 a field changed from empty to a value is recorded with the new value")
  void ex_016_02_empty_to_value() {
    String id = anaNovak();

    edit(ana, acme, id, "jobTitle", "Vodja");

    JsonNode newest = history(ana, acme, id).get(0);
    assertThat(newest.path("action").asText()).isEqualTo("CONTACT_EDITED");
    assertThat(newest.path("details").toString()).contains("Vodja");
  }

  @Test
  @DisplayName("EX-016-03 an edit of 5 fields at once gives one entry with 5 old/new pairs")
  void ex_016_03_five_fields_one_entry() {
    String id = anaNovak();

    edit(
        ana,
        acme,
        id,
        "jobTitle",
        "Vodja",
        "street",
        "Ulica 1",
        "postalCode",
        "2000",
        "city",
        "Maribor",
        "phone",
        "040 555 666");

    List<JsonNode> entries = history(ana, acme, id);
    assertThat(entries).hasSize(2);
    assertThat(entries.get(0).path("details").toString())
        .contains("Vodja", "Ulica 1", "2000", "Maribor", "040 555 666");
  }

  @Test
  @DisplayName("EX-016-05 the History of a Contact of another Group is not found")
  void ex_016_05_other_group_not_found() {
    Grp beta = group(ana, "Beta");
    String betaContact = createContact(ana, beta, "firstName", "Cene").path("id").asText();

    Resp answer = ana.get(acme.history(betaContact)).expect(404);

    assertThat(answer.body()).doesNotContain("Cene");
  }

  @Test
  @DisplayName("EX-016-06 all 200 entries of a Contact can be reached by paging")
  void ex_016_06_paging_reaches_all() {
    String id = anaNovak();
    for (int i = 1; i <= 199; i++) {
      edit(ana, acme, id, "city", "Kraj" + i);
    }

    JsonNode first = ana.get(acme.history(id)).expect(200).json();
    List<JsonNode> all = allPages(id);

    assertThat(first.path("total").asInt()).isEqualTo(200);
    assertThat(first.path("pageSize").asInt()).isEqualTo(25);
    assertThat(all).hasSize(200);
    assertThat(all.stream().map(e -> e.path("id").asText()).distinct()).hasSize(200);
  }

  @Test
  @DisplayName("EX-016-07 old and new notes of 5,000 characters are stored in full")
  void ex_016_07_long_values_in_full() {
    String old = chars(5000, 'a');
    String now = chars(5000, 'b');
    String id = createContact(ana, acme, "firstName", "Ana", "notes", old).path("id").asText();

    edit(ana, acme, id, "notes", now);

    String details = history(ana, acme, id).get(0).path("details").toString();
    assertThat(details).contains(old).contains(now);
  }
}
