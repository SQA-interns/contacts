package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** R-013-08, ADR-08: the values of an entry's details read "removed"; the shape stays. */
class HistoryRedactorTest {

  private final JsonMapper json = JsonMapper.builder().build();
  private final HistoryRedactor redactor = new HistoryRedactor(json);

  private void assertRedacted(String before, String expected) {
    assertThat(json.readTree(redactor.redact(before))).isEqualTo(json.readTree(expected));
  }

  @Test
  void anEditKeepsTheFieldNamesAndRemovesOldAndNewValue() {
    assertRedacted(
        "{\"changes\":{\"lastName\":{\"old\":\"Novak\",\"new\":\"Kos\"}}}",
        "{\"changes\":{\"lastName\":{\"old\":\"removed\",\"new\":\"removed\"}}}");
  }

  @Test
  void aCreationRemovesEveryStoredValue() {
    assertRedacted(
        "{\"values\":{\"firstName\":\"Mojca\",\"email\":\"mojca@zupan.si\"}}",
        "{\"values\":{\"firstName\":\"removed\",\"email\":\"removed\"}}");
  }

  @Test
  void aTagNameAndAnAttributeKeyAndValueAreRemoved() {
    assertRedacted(
        "{\"key\":\"kompetenca\",\"old\":\"java\",\"new\":\"go\"}",
        "{\"key\":\"removed\",\"old\":\"removed\",\"new\":\"removed\"}");
  }

  @Test
  void numbersNullsAndListsAreRemovedToo() {
    assertRedacted(
        "{\"n\":3,\"x\":null,\"l\":[\"a\",{\"b\":\"c\"}]}",
        "{\"n\":\"removed\",\"x\":\"removed\",\"l\":[\"removed\",{\"b\":\"removed\"}]}");
  }

  @Test
  void emptyDetailsStayEmpty() {
    assertRedacted("{}", "{}");
  }
}
