package si.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.contactgroups.acceptance.support.AcceptanceTestBase;
import si.contactgroups.acceptance.support.Exports;
import si.contactgroups.acceptance.support.Grp;
import si.contactgroups.acceptance.support.Json;
import si.contactgroups.acceptance.support.Resp;
import si.contactgroups.acceptance.support.TestUser;

/**
 * US-017 See the Group activity list ({@code GET /groups/{g}/activity}). Calendar days are days in
 * Europe/Ljubljana (business-rules.md, project-wide facts).
 */
class Us017ActivityAcceptanceIT extends AcceptanceTestBase {

  private TestUser ana;
  private TestUser boris;
  private Grp acme;
  private String anaId;
  private String borisId;

  @BeforeEach
  void anaAndBorisInAcme() {
    // the set-up entries (Boris's Invitation and joining) lie on 1 January, away from the test days
    clock.setNow(Instant.parse("2026-01-01T12:00:00Z"));
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
    boris = member(acme, ana, "Boris Kos", "boris@acme.com", "EDITOR");
    anaId = ana.userId();
    borisId = boris.userId();
    clock.reset();
  }

  /** Boris edits a Contact, Ana exports 3 Contacts, Ana invites Cene (in this order). */
  private void threeActions() {
    String a = createContact(ana, acme, "firstName", "A").path("id").asText();
    String b = createContact(ana, acme, "firstName", "B").path("id").asText();
    String c = createContact(ana, acme, "firstName", "C").path("id").asText();
    edit(boris, acme, a, "city", "Celje");
    Exports.byIds(ana, acme, "en", List.of(a, b, c)).expect(200);
    ana.post(acme.invitations(), Json.obj("email", "cene@acme.com", "role", "VIEWER")).expect(201);
  }

  private static boolean newestFirst(List<JsonNode> entries) {
    for (int i = 1; i < entries.size(); i++) {
      Instant before = Instant.parse(entries.get(i - 1).path("occurredAt").asText());
      Instant after = Instant.parse(entries.get(i).path("occurredAt").asText());
      if (before.isBefore(after)) {
        return false;
      }
    }
    return true;
  }

  private void contactAt(String utc, String name) {
    clock.setNow(Instant.parse(utc));
    createContact(ana, acme, "firstName", name);
  }

  @Test
  @DisplayName(
      "AC-017-01 the list shows the edit, the export with 3 and the invitation, newest first")
  void ac_017_01_all_entries_newest_first() {
    threeActions();

    List<JsonNode> entries = activity(ana, acme, "");

    assertThat(newestFirst(entries)).isTrue();
    List<String> actions = actions(entries);
    assertThat(actions)
        .containsSubsequence("INVITATION_CREATED", "CONTACTS_EXPORTED", "CONTACT_EDITED");
    JsonNode export = entries.get(actions.indexOf("CONTACTS_EXPORTED"));
    assertThat(export.path("details").toString()).contains("3");
    assertThat(export.path("userName").asText()).isEqualTo("Ana Novak");
    JsonNode edited = entries.get(actions.indexOf("CONTACT_EDITED"));
    assertThat(edited.path("contactId").asText()).isNotBlank();
  }

  @Test
  @DisplayName(
      "AC-017-02 filtering by user Ana gives the export and the invitation, not Boris's edit")
  void ac_017_02_filter_by_user() {
    threeActions();

    List<JsonNode> entries = activity(ana, acme, "userId=" + anaId);

    assertThat(entries).allMatch(e -> e.path("userName").asText().equals("Ana Novak"));
    assertThat(actions(entries))
        .contains("CONTACTS_EXPORTED", "INVITATION_CREATED")
        .doesNotContain("CONTACT_EDITED");
  }

  @Test
  @DisplayName("AC-017-03 filtering by action 'Contacts exported' gives only the export")
  void ac_017_03_filter_by_action() {
    threeActions();

    assertThat(actions(activity(ana, acme, "action=CONTACTS_EXPORTED")))
        .containsExactly("CONTACTS_EXPORTED");
  }

  @Test
  @DisplayName("AC-017-04 the day 2026-11-07 holds 2026-11-06 23:30 UTC and 2026-11-07 10:00 UTC")
  void ac_017_04_calendar_day_in_ljubljana() {
    contactAt("2026-11-06T22:30:00Z", "Prej");
    contactAt("2026-11-06T23:30:00Z", "Polnoc");
    contactAt("2026-11-07T10:00:00Z", "Dopoldne");
    contactAt("2026-11-07T23:30:00Z", "Pozneje");
    clock.reset();

    List<JsonNode> entries = activity(ana, acme, "from=2026-11-07&to=2026-11-07");

    assertThat(entries).hasSize(2);
    assertThat(entries.get(0).path("details").toString()).contains("Dopoldne");
    assertThat(entries.get(1).path("details").toString()).contains("Polnoc");
  }

  @Test
  @DisplayName("AC-017-05 filters user Boris and action 'Contact edited' are combined with AND")
  void ac_017_05_filters_combined() {
    threeActions();
    String own = createContact(boris, acme, "firstName", "Borisov").path("id").asText();
    String anas = createContact(ana, acme, "firstName", "Anin").path("id").asText();
    edit(ana, acme, anas, "city", "Koper");

    List<JsonNode> entries = activity(ana, acme, "userId=" + borisId + "&action=CONTACT_EDITED");

    assertThat(entries).hasSize(1);
    assertThat(entries.get(0).path("userName").asText()).isEqualTo("Boris Kos");
    assertThat(actions(activity(ana, acme, "userId=" + borisId))).contains("CONTACT_CREATED");
    assertThat(own).isNotBlank();
  }

  @Test
  @DisplayName("AC-017-06 start 2026-11-10 after end 2026-11-01 gives no result")
  void ac_017_06_start_after_end_refused() {
    threeActions();

    Resp answer =
        ana.get(acme.activity("from=2026-11-10&to=2026-11-01"))
            .expectError(422, "activity.range_invalid");

    assertThat(answer.json().has("items")).isFalse();
  }

  @Test
  @DisplayName("AC-017-07 filters that match nothing give an empty list")
  void ac_017_07_no_match_empty() {
    threeActions();

    JsonNode page =
        ana.get(acme.activity("userId=" + borisId + "&action=CONTACTS_EXPORTED"))
            .expect(200)
            .json();

    assertThat(page.path("items")).isEmpty();
    assertThat(page.path("total").asInt()).isZero();
  }

  @Test
  @DisplayName("AC-017-08 a Viewer's request for the list is refused")
  void ac_017_08_viewer_refused() {
    TestUser viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");

    viola.get(acme.activity("")).expectError(403, "auth.role_forbidden");
  }

  @Test
  @DisplayName("AC-017-09 a Member of Acme gets not found and no data for the list of Beta")
  void ac_017_09_other_group_not_found() {
    TestUser cene = user("Cene Zupan", "cene@acme.com");
    Grp beta = group(cene, "Beta");
    createContact(cene, beta, "firstName", "Betin");

    Resp answer = ana.get(beta.activity("")).expect(404);

    assertThat(answer.body()).doesNotContain("Betin");
  }

  @Test
  @DisplayName("EX-017-01 only a start date gives all entries from that day on")
  void ex_017_01_only_start() {
    contactAt("2026-11-06T10:00:00Z", "Prej");
    contactAt("2026-11-07T10:00:00Z", "Na");
    contactAt("2026-11-20T10:00:00Z", "Pozneje");
    clock.reset();

    List<JsonNode> entries = activity(ana, acme, "from=2026-11-07");

    assertThat(entries.stream().map(e -> e.path("details").toString()))
        .anyMatch(d -> d.contains("Na"))
        .anyMatch(d -> d.contains("Pozneje"))
        .noneMatch(d -> d.contains("Prej"));
  }

  @Test
  @DisplayName("EX-017-02 only an end date gives all entries up to the end of that day")
  void ex_017_02_only_end() {
    contactAt("2026-11-06T10:00:00Z", "Prej");
    contactAt("2026-11-07T22:30:00Z", "Pozno");
    contactAt("2026-11-07T23:30:00Z", "Naslednji");
    clock.reset();

    List<JsonNode> entries = activity(ana, acme, "to=2026-11-07");

    assertThat(entries.stream().map(e -> e.path("details").toString()))
        .anyMatch(d -> d.contains("Prej"))
        .anyMatch(d -> d.contains("Pozno"))
        .noneMatch(d -> d.contains("Naslednji"));
  }

  @Test
  @DisplayName("EX-017-03 a removed user can still be chosen as user filter")
  void ex_017_03_removed_user_filter() {
    createContact(boris, acme, "firstName", "Borisov");
    ana.delete(acme.member(borisId)).expect(204);

    List<JsonNode> entries = activity(ana, acme, "userId=" + borisId);

    assertThat(actions(entries)).contains("CONTACT_CREATED");
    assertThat(entries).allMatch(e -> e.path("userName").asText().equals("Boris Kos"));
  }

  @Test
  @DisplayName("EX-017-04 25 entries make one page, 26 make two")
  void ex_017_04_pages_of_25() {
    Grp other = group(ana, "Druga");
    for (int i = 1; i <= 25; i++) {
      createContact(ana, other, "firstName", "Oseba" + i);
    }
    assertThat(activity(ana, other, "page=1")).hasSize(25);
    assertThat(activity(ana, other, "page=2")).isEmpty();

    createContact(ana, other, "firstName", "Oseba26");

    assertThat(activity(ana, other, "page=1")).hasSize(25);
    assertThat(activity(ana, other, "page=2")).hasSize(1);
  }

  @Test
  @DisplayName("EX-017-05 an entry at 2026-11-07 23:00 UTC belongs to 8 Nov, not to 7 Nov")
  void ex_017_05_midnight_local() {
    contactAt("2026-11-07T23:00:00Z", "Polnoc");
    clock.reset();

    assertThat(activity(ana, acme, "from=2026-11-07&to=2026-11-07")).isEmpty();
    assertThat(activity(ana, acme, "from=2026-11-08&to=2026-11-08")).hasSize(1);
  }
}
