package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import si.contactgroups.error.ApiError.FieldError;

/** R-005-02, R-005-04, EX-005-01..07: the checks of the Tag names before they are stored. */
class TagCheckTest {

  private static List<FieldError> errors(List<String> names) {
    List<FieldError> errors = new ArrayList<>();
    TagCheck.clean(names, errors);
    return errors;
  }

  @ParameterizedTest
  @ValueSource(strings = {"a", "č_š-ž1", "udelezenec2025"})
  void valid_names_pass(String name) {
    assertThat(errors(List.of(name))).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "  ", "a b", "a/b", "a.b", "a@b"})
  void invalid_names_are_reported_next_to_their_position(String name) {
    assertThat(errors(List.of("ok", name)))
        .containsExactly(new FieldError("tags[1]", "validation.tag_invalid"));
  }

  @Test
  void length_limit_is_50_characters() {
    assertThat(errors(List.of("b".repeat(50)))).isEmpty();
    assertThat(errors(List.of("b".repeat(51)))).hasSize(1);
  }

  @Test
  void names_are_trimmed_and_the_same_name_ignoring_case_counts_once() {
    List<FieldError> errors = new ArrayList<>();

    assertThat(TagCheck.clean(List.of(" gost ", "GOST", "x"), errors)).containsExactly("gost", "x");
    assertThat(errors).isEmpty();
    assertThat(TagCheck.clean(null, errors)).isEmpty();
  }

  @Test
  void more_than_50_distinct_tags_report_the_limit_and_exactly_50_do_not() {
    List<String> fifty = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      fifty.add("t" + i);
    }
    assertThat(errors(fifty)).isEmpty();

    fifty.add("t51");
    assertThat(errors(fifty)).containsExactly(new FieldError("tags", "validation.tag_limit"));
  }

  @Test
  void a_new_name_for_a_rename_is_trimmed_and_checked_as_field_name() {
    List<FieldError> errors = new ArrayList<>();

    assertThat(TagCheck.cleanName("  gost  ", errors)).isEqualTo("gost");
    assertThat(errors).isEmpty();
    assertThat(TagCheck.cleanName("a b", errors)).isEqualTo("a b");
    assertThat(TagCheck.cleanName(null, errors)).isEmpty();
    assertThat(errors)
        .containsExactly(
            new FieldError("name", "validation.tag_invalid"),
            new FieldError("name", "validation.tag_invalid"));
  }
}
