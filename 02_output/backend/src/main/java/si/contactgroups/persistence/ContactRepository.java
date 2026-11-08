package si.contactgroups.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import si.contactgroups.domain.ContactData;
import si.contactgroups.domain.Normaliser;
import si.contactgroups.domain.StoredContact;

/** The Contacts of a Group (table {@code contact}); every method is scoped to one Group. */
@Repository
public class ContactRepository {

  private static final String COLUMNS =
      "id, version, first_name, last_name, email, phone, company, job_title, street,"
          + " postal_code, city, country, notes, created_at, updated_at";

  private final JdbcClient jdbc;

  public ContactRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** Stores a new Contact with version 1 and returns its id. */
  public UUID insert(UUID groupId, UUID createdBy, ContactData data, Instant now) {
    return bind(
            jdbc.sql(
                "INSERT INTO contact (group_id, created_by, created_at, updated_at, first_name,"
                    + " last_name, email, phone, company, job_title, street, postal_code, city,"
                    + " country, notes, first_name_norm, last_name_norm, company_norm, email_norm,"
                    + " search_norm) VALUES (:groupId, :createdBy, :now, :now, :firstName,"
                    + " :lastName, :email, :phone, :company, :jobTitle, :street, :postalCode,"
                    + " :city, :country, :notes, :firstNameNorm, :lastNameNorm, :companyNorm,"
                    + " :emailNorm, :searchNorm) RETURNING id"),
            data)
        .param("groupId", groupId)
        .param("createdBy", createdBy)
        .param("now", Timestamp.from(now))
        .query(UUID.class)
        .single();
  }

  /** The Contact of the Group that is not in Trash; empty when there is none (BR-01). */
  public Optional<StoredContact> find(UUID groupId, UUID contactId) {
    return jdbc.sql(
            "SELECT "
                + COLUMNS
                + " FROM contact WHERE id = :id AND group_id = :groupId AND deleted_at IS NULL")
        .param("id", contactId)
        .param("groupId", groupId)
        .query(
            (rs, n) ->
                new StoredContact(
                    rs.getObject("id", UUID.class),
                    rs.getInt("version"),
                    new ContactData(
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("company"),
                        rs.getString("job_title"),
                        rs.getString("street"),
                        rs.getString("postal_code"),
                        rs.getString("city"),
                        rs.getString("country"),
                        rs.getString("notes")),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()))
        .optional();
  }

  /**
   * Stores the new fields and raises the version number by 1, only when the stored version is
   * {@code version} (BR-07).
   *
   * @return whether a row was changed
   */
  public boolean update(UUID groupId, UUID contactId, int version, ContactData data, Instant now) {
    return bind(
                jdbc.sql(
                    "UPDATE contact SET version = version + 1, updated_at = :now,"
                        + " first_name = :firstName, last_name = :lastName, email = :email,"
                        + " phone = :phone, company = :company, job_title = :jobTitle,"
                        + " street = :street, postal_code = :postalCode, city = :city,"
                        + " country = :country, notes = :notes,"
                        + " first_name_norm = :firstNameNorm, last_name_norm = :lastNameNorm,"
                        + " company_norm = :companyNorm, email_norm = :emailNorm,"
                        + " search_norm = :searchNorm WHERE id = :id AND group_id = :groupId"
                        + " AND version = :version AND deleted_at IS NULL"),
                data)
            .param("id", contactId)
            .param("groupId", groupId)
            .param("version", version)
            .param("now", Timestamp.from(now))
            .update()
        == 1;
  }

  private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec sql, ContactData d) {
    return sql.param("firstName", d.firstName())
        .param("lastName", d.lastName())
        .param("email", d.email())
        .param("phone", d.phone())
        .param("company", d.company())
        .param("jobTitle", d.jobTitle())
        .param("street", d.street())
        .param("postalCode", d.postalCode())
        .param("city", d.city())
        .param("country", d.country())
        .param("notes", d.notes())
        .param("firstNameNorm", Normaliser.normalise(d.firstName()))
        .param("lastNameNorm", Normaliser.normalise(d.lastName()))
        .param("companyNorm", Normaliser.normalise(d.company()))
        .param("emailNorm", d.email())
        .param("searchNorm", searchText(d));
  }

  /** First name, last name, e-mail, company and city, normalised, joined by a space. */
  private static String searchText(ContactData d) {
    StringBuilder text = new StringBuilder();
    for (String part :
        new String[] {d.firstName(), d.lastName(), d.email(), d.company(), d.city()}) {
      if (part != null) {
        text.append(text.isEmpty() ? "" : " ").append(Normaliser.normalise(part));
      }
    }
    return text.toString();
  }
}
