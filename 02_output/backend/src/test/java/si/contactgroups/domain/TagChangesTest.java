package si.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** R-005-01, R-005-03, R-005-06: which Tags a save put on or took off, ignoring case. */
class TagChangesTest {

  @Test
  void added_and_removed_tags_are_found_by_lower_case_name() {
    TagChanges changes =
        TagChanges.between(
            List.of("gost", "Udelezenec", "stara"), List.of("GOST", "nova", "udelezenec"));

    assertThat(changes.added()).containsExactly("nova");
    assertThat(changes.removed()).containsExactly("stara");
  }

  @Test
  void equal_lists_change_nothing_even_in_another_case_or_order() {
    assertThat(TagChanges.between(List.of("a", "B"), List.of("b", "A")).isEmpty()).isTrue();
  }
}
