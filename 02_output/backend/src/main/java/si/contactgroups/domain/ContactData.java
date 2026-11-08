package si.contactgroups.domain;

/** The 11 predefined fields of a Contact (R-003-02); an empty field is {@code null}. */
public record ContactData(
    String firstName,
    String lastName,
    String email,
    String phone,
    String company,
    String jobTitle,
    String street,
    String postalCode,
    String city,
    String country,
    String notes) {}
