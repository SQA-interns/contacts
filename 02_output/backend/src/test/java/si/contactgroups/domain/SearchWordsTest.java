package si.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** R-008-01, R-008-05, R-008-06: the words of a search text as LIKE patterns. */
class SearchWordsTest {

  @Test
  void words_are_split_at_spaces_and_normalised() {
    assertThat(SearchWords.patterns("  ČEH   ana ")).containsExactly("%ceh%", "%ana%");
  }

  @Test
  void empty_or_blank_text_is_no_search() {
    assertThat(SearchWords.patterns(null)).isEmpty();
    assertThat(SearchWords.patterns("")).isEmpty();
    assertThat(SearchWords.patterns("   ")).isEmpty();
  }

  @Test
  void like_characters_are_escaped_as_plain_text() {
    List<String> patterns = SearchWords.patterns("50%_off\\");

    assertThat(patterns).containsExactly("%50\\%\\_off\\\\%");
  }

  @Test
  void quotes_and_tags_stay_plain_text() {
    assertThat(SearchWords.patterns("'; <script>")).containsExactly("%';%", "%<script>%");
  }
}
