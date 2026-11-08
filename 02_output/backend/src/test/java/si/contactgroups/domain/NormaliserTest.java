package si.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** ADR-06, BR-05: case- and accent-insensitive comparison text. */
class NormaliserTest {

  @ParameterizedTest
  @CsvSource({
    "Škerlj, skerlj",
    "Čosić, cosic",
    "Đorđević, dordevic",
    "Straße, strasse",
    "ŽAGAR, zagar"
  })
  void accents_and_case_are_removed(String text, String expected) {
    assertThat(Normaliser.normalise(text)).isEqualTo(expected);
  }

  @Test
  void the_text_is_trimmed() {
    assertThat(Normaliser.normalise("  Ana  ")).isEqualTo("ana");
  }

  @Test
  void null_gives_null() {
    assertThat(Normaliser.normalise(null)).isNull();
  }
}
