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
    String notes) {

  public static Builder builder() {
    return new Builder();
  }

  /** Builds a {@link ContactData} field by field. */
  public static final class Builder {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String company;
    private String jobTitle;
    private String street;
    private String postalCode;
    private String city;
    private String country;
    private String notes;

    public Builder firstName(String value) {
      this.firstName = value;
      return this;
    }

    public Builder lastName(String value) {
      this.lastName = value;
      return this;
    }

    public Builder email(String value) {
      this.email = value;
      return this;
    }

    public Builder phone(String value) {
      this.phone = value;
      return this;
    }

    public Builder company(String value) {
      this.company = value;
      return this;
    }

    public Builder jobTitle(String value) {
      this.jobTitle = value;
      return this;
    }

    public Builder street(String value) {
      this.street = value;
      return this;
    }

    public Builder postalCode(String value) {
      this.postalCode = value;
      return this;
    }

    public Builder city(String value) {
      this.city = value;
      return this;
    }

    public Builder country(String value) {
      this.country = value;
      return this;
    }

    public Builder notes(String value) {
      this.notes = value;
      return this;
    }

    public ContactData build() {
      return new ContactData(
          firstName,
          lastName,
          email,
          phone,
          company,
          jobTitle,
          street,
          postalCode,
          city,
          country,
          notes);
    }
  }
}
