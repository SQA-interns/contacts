package si.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** R-004-06: what a save did to the Attributes, matched by key ignoring case. */
class AttributeChangesTest {

  private static AttributePair pair(String key, String value) {
    return new AttributePair(key, value);
  }

  @Test
  void added_changed_and_removed_pairs_are_found_by_lower_case_key() {
    List<AttributeChanges.Change> changes =
        AttributeChanges.between(
            List.of(pair("a", "1"), pair("b", "2"), pair("c", "3")),
            List.of(pair("A", "1"), pair("b", "9"), pair("d", "4")));

    assertThat(changes)
        .containsExactly(
            new AttributeChanges.Change(HistoryAction.ATTRIBUTE_CHANGED, "b", "2", "9"),
            new AttributeChanges.Change(HistoryAction.ATTRIBUTE_ADDED, "d", null, "4"),
            new AttributeChanges.Change(HistoryAction.ATTRIBUTE_REMOVED, "c", "3", null));
  }

  @Test
  void equal_lists_change_nothing_even_in_another_order() {
    assertThat(
            AttributeChanges.between(
                List.of(pair("a", "1"), pair("b", "2")), List.of(pair("b", "2"), pair("a", "1"))))
        .isEmpty();
  }
}
