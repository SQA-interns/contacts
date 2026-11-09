package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import si.contactgroups.domain.ContactFilter;

/** R-009-01..07: the query parameters of the Contact list as one normalised filter. */
class FilterParserTest {

  private static ContactFilter parse(
      List<String> tags, String mode, List<String> without, String country, String company) {
    return FilterParser.parse(null, tags, mode, without, country, company, null, null, null);
  }

  @Test
  void noParameterIsNoFilter() {
    assertThat(FilterParser.parse(null, null, null, null, null, null, null, null, null))
        .isEqualTo(ContactFilter.NONE);
  }

  @Test
  void tagsAreNormalisedAndBlankOnesDropped() {
    ContactFilter f = parse(List.of(" ČEH ", " ", "a"), "any", List.of("NeObvescaj"), null, null);

    assertThat(f.tags()).containsExactly("ceh", "a");
    assertThat(f.anyTag()).isTrue();
    assertThat(f.withoutTags()).containsExactly("neobvescaj");
  }

  @Test
  void tagModeDefaultsToAllAndRefusesOtherValues() {
    assertThat(parse(List.of("a"), null, null, null, null).anyTag()).isFalse();
    assertThatThrownBy(() -> parse(List.of("a"), "some", null, null, null))
        .isInstanceOf(InvalidQueryException.class);
  }

  @Test
  void countryMustBeTwoCapitalLetters() {
    assertThat(parse(null, null, null, "SI", null).country()).isEqualTo("SI");
    assertThatThrownBy(() -> parse(null, null, null, "si", null))
        .isInstanceOf(InvalidQueryException.class);
  }

  @Test
  void companyIsNormalisedAndBlankIsNoFilter() {
    assertThat(parse(null, null, null, null, " Čeh d.o.o. ").company()).isEqualTo("ceh d.o.o.");
    assertThat(parse(null, null, null, null, "  ").company()).isNull();
  }

  @Test
  void attributeValueIsNormalisedAndKeyLowerCased() {
    ContactFilter f =
        FilterParser.parse(null, null, null, null, null, null, "Kompetenca", "JAVA", null);

    assertThat(f.attrKey()).isEqualTo("kompetenca");
    assertThat(f.attrValue()).isEqualTo("java");
    assertThat(f.attrExists()).isFalse();
  }

  @Test
  void attributeExistsNeedsAKeyAndMustBeABoolean() {
    ContactFilter f =
        FilterParser.parse(null, null, null, null, null, null, "kompetenca", null, "true");
    assertThat(f.attrExists()).isTrue();

    assertThat(FilterParser.parse(null, null, null, null, null, null, null, null, "true"))
        .isEqualTo(ContactFilter.NONE);
    assertThatThrownBy(
            () -> FilterParser.parse(null, null, null, null, null, null, "k", null, "yes"))
        .isInstanceOf(InvalidQueryException.class);
  }

  @Test
  void anAttributeKeyAloneIsNoFilter() {
    assertThat(FilterParser.parse(null, null, null, null, null, null, "kompetenca", null, null))
        .isEqualTo(ContactFilter.NONE);
  }
}
