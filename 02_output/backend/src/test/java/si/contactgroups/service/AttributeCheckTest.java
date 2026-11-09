package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import si.contactgroups.domain.AttributePair;
import si.contactgroups.error.ApiError.FieldError;

/** R-004-02..04, R-004-09: the checks of the Attributes before they are stored. */
class AttributeCheckTest {

  private static AttributePair pair(String key, String value) {
    return new AttributePair(key, value);
  }

  private static List<FieldError> errors(AttributePair... pairs) {
    List<FieldError> errors = new ArrayList<>();
    AttributeCheck.clean(List.of(pairs), errors);
    return errors;
  }

  @Test
  void keys_and_values_are_trimmed_and_a_missing_list_is_empty() {
    List<FieldError> errors = new ArrayList<>();

    assertThat(AttributeCheck.clean(List.of(pair(" delovno mesto ", " vodja ")), errors))
        .containsExactly(pair("delovno mesto", "vodja"));
    assertThat(AttributeCheck.clean(null, errors)).isEmpty();
    assertThat(errors).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({"50, true", "51, false"})
  void key_length_limit_is_50(int length, boolean valid) {
    assertThat(errors(pair("k".repeat(length), "v")).isEmpty()).isEqualTo(valid);
  }

  @ParameterizedTest
  @CsvSource({"255, true", "256, false"})
  void value_length_limit_is_255(int length, boolean valid) {
    assertThat(errors(pair("k", "v".repeat(length))).isEmpty()).isEqualTo(valid);
  }

  @Test
  void empty_key_and_empty_value_are_reported_next_to_their_pair() {
    assertThat(errors(pair("a", "1"), pair(" ", "x"), pair("b", null)))
        .containsExactly(
            new FieldError("attributes[1].key", "validation.attribute_key_invalid"),
            new FieldError("attributes[2].value", "validation.attribute_value_invalid"));
  }

  @Test
  void a_key_equal_ignoring_case_is_reported_at_the_second_pair_only() {
    assertThat(errors(pair("kompetenca", "java"), pair("Kompetenca", "sql")))
        .containsExactly(new FieldError("attributes[1].key", "validation.attribute_duplicate_key"));
  }

  @Test
  void more_than_50_pairs_are_refused_with_the_limit() {
    List<AttributePair> pairs = new ArrayList<>();
    for (int i = 0; i < 51; i++) {
      pairs.add(pair("k" + i, "v"));
    }
    List<FieldError> errors = new ArrayList<>();

    AttributeCheck.clean(pairs, errors);

    assertThat(errors).containsExactly(new FieldError("attributes", "validation.attribute_limit"));
  }
}
