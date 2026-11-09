package si.contactgroups.domain;

import java.util.List;

/**
 * The conditions of the Contact list (US-008, US-009), all joined by AND. Texts are already
 * normalised (BR-05) except {@code country} and the lower-cased {@code attrKey}.
 *
 * @param words LIKE patterns of the search text; empty means no search
 * @param tags normalised Tag names to require; empty means no Tag filtering
 * @param anyTag {@code true} for "has any of", {@code false} for "has all of"
 * @param withoutTags normalised Tag names the Contact must not carry
 * @param country upper-case country code, or {@code null}
 * @param company normalised company, or {@code null}
 * @param attrKey lower-case Attribute key, or {@code null}
 * @param attrValue normalised Attribute value, or {@code null}
 * @param attrExists whether only the key has to exist (used when there is no value)
 */
public record ContactFilter(
    List<String> words,
    List<String> tags,
    boolean anyTag,
    List<String> withoutTags,
    String country,
    String company,
    String attrKey,
    String attrValue,
    boolean attrExists) {

  /** No search and no filter. */
  public static final ContactFilter NONE =
      new ContactFilter(List.of(), List.of(), false, List.of(), null, null, null, null, false);

  /** The same filter with another search text (used by tests and callers that only search). */
  public static ContactFilter searching(List<String> words) {
    return new ContactFilter(words, List.of(), false, List.of(), null, null, null, null, false);
  }
}
