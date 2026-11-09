package si.contactgroups.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import si.contactgroups.domain.ContactData;
import si.contactgroups.domain.CountryCodes;
import si.contactgroups.error.ApiError.FieldError;

/**
 * BR-06 and R-003-03..05: trims every text, lowercases the e-mail and checks the Contact; all
 * errors are returned at once.
 */
@Component
public class ContactValidator {

  private static final int MAX_TEXT = 255;
  private static final int MAX_PHONE = 30;
  private static final int MAX_NOTES = 5000;
  private static final Pattern PHONE = Pattern.compile("^[0-9 +\\-()./]*$");

  /**
   * @return the trimmed Contact data, empty texts as {@code null}
   * @throws ValidationException when BR-06 is broken
   */
  public ContactData validate(ContactData in) {
    ContactData data =
        new ContactData(
            clean(in.firstName()),
            clean(in.lastName()),
            lower(clean(in.email())),
            clean(in.phone()),
            clean(in.company()),
            clean(in.jobTitle()),
            clean(in.street()),
            clean(in.postalCode()),
            clean(in.city()),
            clean(in.country()),
            clean(in.notes()));
    List<FieldError> errors = new ArrayList<>();
    checkOneOfFour(data, errors);
    checkEmail(data.email(), errors);
    checkPhone(data.phone(), errors);
    checkLength("firstName", data.firstName(), MAX_TEXT, errors);
    checkLength("lastName", data.lastName(), MAX_TEXT, errors);
    checkLength("company", data.company(), MAX_TEXT, errors);
    checkLength("jobTitle", data.jobTitle(), MAX_TEXT, errors);
    checkLength("street", data.street(), MAX_TEXT, errors);
    checkLength("postalCode", data.postalCode(), MAX_TEXT, errors);
    checkLength("city", data.city(), MAX_TEXT, errors);
    checkLength("notes", data.notes(), MAX_NOTES, errors);
    if (data.country() != null && !CountryCodes.contains(data.country())) {
      errors.add(new FieldError("country", "validation.country_invalid"));
    }
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
    return data;
  }

  private static String clean(String text) {
    if (text == null) {
      return null;
    }
    String stripped = text.strip();
    return stripped.isEmpty() ? null : stripped;
  }

  private static String lower(String text) {
    return text == null ? null : text.toLowerCase(Locale.ROOT);
  }

  private static void checkOneOfFour(ContactData data, List<FieldError> errors) {
    if (data.firstName() == null
        && data.lastName() == null
        && data.email() == null
        && data.company() == null) {
      for (String field : List.of("firstName", "lastName", "email", "company")) {
        errors.add(new FieldError(field, "validation.required_any"));
      }
    }
  }

  private static void checkEmail(String email, List<FieldError> errors) {
    if (email == null) {
      return;
    }
    if (email.length() > MAX_TEXT) {
      errors.add(new FieldError("email", "validation.too_long"));
    } else if (!isEmailFormat(email)) {
      errors.add(new FieldError("email", "validation.email_invalid"));
    }
  }

  /** One {@code @}, a local part, and a domain of at least two non-empty labels; no spaces. */
  static boolean isEmailFormat(String email) {
    int at = email.indexOf('@');
    if (at < 1 || at != email.lastIndexOf('@')) {
      return false;
    }
    String domain = email.substring(at + 1);
    boolean hasSpace = email.chars().anyMatch(Character::isWhitespace);
    return !hasSpace
        && domain.indexOf('.') > 0
        && !domain.startsWith(".")
        && !domain.endsWith(".")
        && !domain.contains("..");
  }

  private static void checkPhone(String phone, List<FieldError> errors) {
    if (phone != null && (phone.length() > MAX_PHONE || !PHONE.matcher(phone).matches())) {
      errors.add(new FieldError("phone", "validation.phone_invalid"));
    }
  }

  private static void checkLength(String field, String text, int max, List<FieldError> errors) {
    if (text != null && text.length() > max) {
      errors.add(new FieldError(field, "validation.too_long"));
    }
  }
}
