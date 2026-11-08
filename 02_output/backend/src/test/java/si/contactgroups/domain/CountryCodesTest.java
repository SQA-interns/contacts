package si.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** EX-003-09: the country must be on the list of ISO 3166-1 alpha-2 codes. */
class CountryCodesTest {

  @Test
  void slovenia_is_on_the_list() {
    assertThat(CountryCodes.contains("SI")).isTrue();
  }

  @Test
  void xx_and_lowercase_are_not_on_the_list() {
    assertThat(CountryCodes.contains("XX")).isFalse();
    assertThat(CountryCodes.contains("si")).isFalse();
    assertThat(CountryCodes.contains(null)).isFalse();
  }
}
