package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import si.contactgroups.domain.ContactData;
import si.contactgroups.error.ApiError.FieldError;

/** BR-06, R-003-03..05: the checks of a Contact before it is stored. */
class ContactValidatorTest {

  private final ContactValidator validator = new ContactValidator();

  private static ContactData.Builder with() {
    return ContactData.builder();
  }

  private List<FieldError> errorsOf(ContactData data) {
    return ((ValidationException)
            catchThrowable(() -> validator.validate(data), ValidationException.class))
        .getFieldErrors();
  }

  private static Throwable catchThrowable(Runnable run, Class<? extends Throwable> type) {
    try {
      run.run();
    } catch (RuntimeException e) {
      assertThat(e).isInstanceOf(type);
      return e;
    }
    throw new AssertionError("expected " + type.getSimpleName());
  }

  @Test
  void every_text_is_trimmed_and_the_email_lowercased() {
    ContactData checked =
        validator.validate(with().firstName("  Ana ").email(" ANA@Acme.COM ").build());

    assertThat(checked.firstName()).isEqualTo("Ana");
    assertThat(checked.email()).isEqualTo("ana@acme.com");
  }

  @Test
  void spaces_only_become_empty() {
    ContactData checked = validator.validate(with().firstName("   ").lastName("Novak").build());

    assertThat(checked.firstName()).isNull();
  }

  @Test
  void none_of_the_four_fields_gives_required_any() {
    assertThat(errorsOf(with().phone("040 123 456").build()))
        .extracting(FieldError::code)
        .containsOnly("validation.required_any");
  }

  @ParameterizedTest
  @CsvSource({"ana@", "@acme.com", "ana acme@x.si", "ana@acme", "a@@b.si"})
  void a_bad_email_is_refused_on_the_email_field(String email) {
    assertThat(errorsOf(with().firstName("Ana").email(email).build()))
        .containsExactly(new FieldError("email", "validation.email_invalid"));
  }

  @Test
  void the_email_length_limit_is_255() {
    String local = "a".repeat(255 - "@acme.si".length());
    assertThat(validator.validate(with().email(local + "@acme.si").build()).email()).hasSize(255);
    assertThat(errorsOf(with().email("a" + local + "@acme.si").build()))
        .containsExactly(new FieldError("email", "validation.too_long"));
  }

  @ParameterizedTest
  @CsvSource({"abc", "040 12x", "1234567890123456789012345678901"})
  void a_bad_phone_is_refused(String phone) {
    assertThat(errorsOf(with().firstName("Ana").phone(phone).build()))
        .extracting(FieldError::field)
        .containsExactly("phone");
  }

  @Test
  void the_phone_characters_are_digits_space_and_the_listed_signs() {
    assertThat(
            validator
                .validate(with().firstName("A").phone("+386 (40) 123-456.7/8").build())
                .phone())
        .isEqualTo("+386 (40) 123-456.7/8");
  }

  @Test
  void text_limits_are_255_and_notes_5000() {
    assertThat(validator.validate(with().firstName("a".repeat(255)).build()).firstName())
        .hasSize(255);
    assertThat(errorsOf(with().firstName("a".repeat(256)).build()))
        .containsExactly(new FieldError("firstName", "validation.too_long"));
    assertThat(validator.validate(with().firstName("A").notes("n".repeat(5000)).build()).notes())
        .hasSize(5000);
    assertThat(errorsOf(with().firstName("A").notes("n".repeat(5001)).build()))
        .containsExactly(new FieldError("notes", "validation.too_long"));
  }

  @Test
  void the_country_must_be_on_the_list() {
    assertThat(validator.validate(with().firstName("A").country("SI").build()).country())
        .isEqualTo("SI");
    assertThat(errorsOf(with().firstName("A").country("XX").build()))
        .containsExactly(new FieldError("country", "validation.country_invalid"));
  }

  @Test
  void all_errors_are_returned_at_once() {
    assertThatThrownBy(
            () -> validator.validate(with().email("ana@").phone("abc").country("XX").build()))
        .isInstanceOfSatisfying(
            ValidationException.class, e -> assertThat(e.getFieldErrors()).hasSize(3));
  }

  @Test
  void slovene_letters_and_markup_are_kept_unchanged() {
    ContactData checked =
        validator.validate(with().firstName("Čeh").lastName("<script>alert(1)</script>").build());

    assertThat(checked.firstName()).isEqualTo("Čeh");
    assertThat(checked.lastName()).isEqualTo("<script>alert(1)</script>");
  }
}
