package si.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import si.contactgroups.domain.VersionDiff.Difference;

/** R-022-04: the fields, Tags and Attributes that differ, with both values (D-19b). */
class VersionDiffTest {

  private static final ContactData ACME =
      new ContactData("Ana", "Novak", null, null, "Acme", null, null, null, null, null, null);
  private static final ContactData BETA =
      new ContactData("Ana", "Novak", null, null, "Beta", null, null, null, null, null, null);

  private static VersionContent content(
      ContactData data, List<String> tags, List<AttributePair> attributes) {
    return new VersionContent(data, tags, attributes);
  }

  @Test
  void a_changed_field_shows_both_values_and_nothing_else() {
    List<Difference> diff =
        VersionDiff.between(
            content(ACME, List.of(), List.of()), content(BETA, List.of(), List.of()));

    assertThat(diff).containsExactly(new Difference("field", "company", "Acme", "Beta"));
  }

  @Test
  void a_field_that_was_empty_has_no_value() {
    ContactData withCity =
        new ContactData("Ana", "Novak", null, null, "Acme", null, null, null, "Celje", null, null);

    List<Difference> diff =
        VersionDiff.between(
            content(ACME, List.of(), List.of()), content(withCity, List.of(), List.of()));

    assertThat(diff).containsExactly(new Difference("field", "city", null, "Celje"));
  }

  @Test
  void a_tag_only_in_the_current_state_is_added() {
    List<Difference> diff =
        VersionDiff.between(
            content(ACME, List.of(), List.of()), content(ACME, List.of("gost"), List.of()));

    assertThat(diff).containsExactly(new Difference("tag", "gost", null, "gost"));
  }

  @Test
  void a_tag_only_in_the_version_is_removed() {
    List<Difference> diff =
        VersionDiff.between(
            content(ACME, List.of("gost", "vip"), List.of()),
            content(ACME, List.of("vip"), List.of()));

    assertThat(diff).containsExactly(new Difference("tag", "gost", "gost", null));
  }

  @Test
  void attributes_are_compared_by_key_and_show_both_values() {
    List<AttributePair> old = List.of(new AttributePair("kompetenca", "java"));
    List<AttributePair> now =
        List.of(new AttributePair("kompetenca", "kotlin"), new AttributePair("mesto", "Celje"));

    List<Difference> diff =
        VersionDiff.between(content(ACME, List.of(), old), content(ACME, List.of(), now));

    assertThat(diff)
        .containsExactly(
            new Difference("attribute", "kompetenca", "java", "kotlin"),
            new Difference("attribute", "mesto", null, "Celje"));
  }

  @Test
  void equal_states_have_no_difference() {
    VersionContent same =
        content(ACME, List.of("gost"), List.of(new AttributePair("kompetenca", "java")));

    assertThat(VersionDiff.between(same, same)).isEmpty();
  }
}
