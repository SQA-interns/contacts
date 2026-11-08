package si.contactgroups.service;

import si.contactgroups.domain.ContactData;

/** Builds Contact data field by field for tests. */
final class ContactDataBuilder {
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

  ContactDataBuilder firstName(String value) {
    this.firstName = value;
    return this;
  }

  ContactDataBuilder lastName(String value) {
    this.lastName = value;
    return this;
  }

  ContactDataBuilder email(String value) {
    this.email = value;
    return this;
  }

  ContactDataBuilder phone(String value) {
    this.phone = value;
    return this;
  }

  ContactDataBuilder company(String value) {
    this.company = value;
    return this;
  }

  ContactDataBuilder jobTitle(String value) {
    this.jobTitle = value;
    return this;
  }

  ContactDataBuilder street(String value) {
    this.street = value;
    return this;
  }

  ContactDataBuilder postalCode(String value) {
    this.postalCode = value;
    return this;
  }

  ContactDataBuilder city(String value) {
    this.city = value;
    return this;
  }

  ContactDataBuilder country(String value) {
    this.country = value;
    return this;
  }

  ContactDataBuilder notes(String value) {
    this.notes = value;
    return this;
  }

  ContactData build() {
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
