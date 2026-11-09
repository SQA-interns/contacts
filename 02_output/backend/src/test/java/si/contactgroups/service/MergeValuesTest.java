package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import si.contactgroups.domain.AttributePair;
import si.contactgroups.domain.ContactData;
import si.contactgroups.error.ApiError.FieldError;

/**
 * R-026-02..04, D-19(d): the values of a merge come from the merged Contacts; Tags and Attributes
 * are combined. The first source is the Survivor.
 */
class MergeValuesTest {

  private static MergeSource source(
      ContactData data, List<AttributePair> attributes, List<String> tags) {
    return new MergeSource(UUID.randomUUID(), data, attributes, tags);
  }

  private static MergeSource named(String firstName, String phone) {
    return source(
        new ContactDataBuilder().firstName(firstName).phone(phone).build(), List.of(), List.of());
  }

  private static MergeSource withTags(String... tags) {
    return source(new ContactDataBuilder().firstName("Ana").build(), List.of(), List.of(tags));
  }

  private static MergeSource withAttributes(AttributePair... pairs) {
    return source(new ContactDataBuilder().firstName("Ana").build(), List.of(pairs), List.of());
  }

  private static AttributePair pair(String key, String value) {
    return new AttributePair(key, value);
  }

  private static List<FieldError> errorsOf(Runnable run) {
    return org.junit.jupiter.api.Assertions.assertThrows(MergeValueException.class, run::run)
        .getFieldErrors();
  }

  @Test
  void r_026_02_a_chosen_value_of_any_merged_contact_is_kept() {
    List<MergeSource> sources = List.of(named("Ana", null), named("ana", "040 123 456"));

    ContactData chosen =
        MergeValues.chooseFields(
            new ContactDataBuilder().firstName("ana").phone("040 123 456").build(), sources);

    assertThat(chosen.firstName()).isEqualTo("ana");
    assertThat(chosen.phone()).isEqualTo("040 123 456");
  }

  @Test
  void d_19_d_a_value_that_no_merged_contact_has_is_refused_with_its_field() {
    List<MergeSource> sources = List.of(named("Ana", null), named("ana", "040 123 456"));

    List<FieldError> errors =
        errorsOf(
            () ->
                MergeValues.chooseFields(
                    new ContactDataBuilder().firstName("Anica").phone("040 123 456").build(),
                    sources));

    assertThat(errors).containsExactly(new FieldError("firstName", "merge.invalid_value"));
  }

  @Test
  void ex_026_09_notes_joined_from_two_contacts_are_no_value_of_a_contact() {
    MergeSource a =
        source(
            new ContactDataBuilder().firstName("Ana").notes("prva").build(), List.of(), List.of());
    MergeSource b =
        source(
            new ContactDataBuilder().firstName("Ana").notes("druga").build(), List.of(), List.of());

    List<FieldError> errors =
        errorsOf(
            () ->
                MergeValues.chooseFields(
                    new ContactDataBuilder().firstName("Ana").notes("prvadruga").build(),
                    List.of(a, b)));

    assertThat(errors).containsExactly(new FieldError("notes", "merge.invalid_value"));
  }

  @Test
  void an_empty_choice_is_allowed_even_when_a_contact_has_a_value_and_the_text_is_trimmed() {
    List<MergeSource> sources = List.of(named("Ana", "040 123 456"), named("Anica", null));

    ContactData chosen =
        MergeValues.chooseFields(
            new ContactDataBuilder().firstName(" Anica ").phone("  ").build(), sources);

    assertThat(chosen.firstName()).isEqualTo("Anica");
    assertThat(chosen.phone()).isNull();
  }

  @Test
  void the_e_mail_matches_ignoring_case_because_it_is_stored_in_lower_case() {
    MergeSource a =
        source(new ContactDataBuilder().email("ana@acme.com").build(), List.of(), List.of());
    MergeSource b =
        source(new ContactDataBuilder().email("ana@acme.com").build(), List.of(), List.of());

    ContactData chosen =
        MergeValues.chooseFields(
            new ContactDataBuilder().email("Ana@Acme.com").build(), List.of(a, b));

    assertThat(chosen.email()).isEqualTo("Ana@Acme.com");
  }

  @Test
  void r_026_03_tags_are_combined_ignoring_case_survivor_first() {
    List<String> tags =
        MergeValues.combineTags(
            List.of(withTags("udelezenec2025", "VIP"), withTags("neObvescaj", "vip")));

    assertThat(tags).containsExactly("udelezenec2025", "VIP", "neObvescaj");
  }

  @Test
  void r_026_04_attributes_with_different_keys_are_all_kept() {
    List<AttributePair> result =
        MergeValues.combineAttributes(
            List.of(),
            List.of(
                withAttributes(pair("jezik", "sl")), withAttributes(pair("kompetenca", "java"))));

    assertThat(result).containsExactly(pair("jezik", "sl"), pair("kompetenca", "java"));
  }

  @Test
  void ex_026_08_keys_that_differ_in_case_are_one_key_and_a_choice_decides_the_value() {
    List<MergeSource> sources =
        List.of(
            withAttributes(pair("Kompetenca", "java")), withAttributes(pair("kompetenca", "sql")));

    List<AttributePair> result =
        MergeValues.combineAttributes(List.of(pair("kompetenca", "sql")), sources);

    assertThat(result).containsExactly(pair("Kompetenca", "sql"));
  }

  @Test
  void r_026_04_the_same_value_under_one_key_needs_no_choice() {
    List<MergeSource> sources =
        List.of(
            withAttributes(pair("kompetenca", "java")), withAttributes(pair("Kompetenca", "java")));

    assertThat(MergeValues.combineAttributes(List.of(), sources))
        .containsExactly(pair("kompetenca", "java"));
  }

  @Test
  void ac_026_03_two_values_for_a_key_without_a_choice_are_refused() {
    List<MergeSource> sources =
        List.of(
            withAttributes(pair("kompetenca", "java")),
            withAttributes(pair("kompetenca", "spletnoProgramiranje")));

    List<FieldError> errors = errorsOf(() -> MergeValues.combineAttributes(List.of(), sources));

    assertThat(errors).containsExactly(new FieldError("attributes", "merge.invalid_value"));
  }

  @Test
  void d_19_d_a_chosen_attribute_value_no_contact_has_is_refused() {
    List<MergeSource> sources =
        List.of(
            withAttributes(pair("kompetenca", "java")), withAttributes(pair("kompetenca", "sql")));

    assertThatThrownBy(
            () -> MergeValues.combineAttributes(List.of(pair("kompetenca", "c++")), sources))
        .isInstanceOf(MergeValueException.class);
    assertThatThrownBy(
            () ->
                MergeValues.combineAttributes(
                    List.of(pair("kompetenca", "java"), pair("Kompetenca", "sql")), sources))
        .isInstanceOf(MergeValueException.class);
    assertThatThrownBy(() -> MergeValues.combineAttributes(List.of(pair("novo", "x")), sources))
        .isInstanceOf(MergeValueException.class);
  }
}
