package si.contactgroups.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import si.contactgroups.domain.ContactData;
import si.contactgroups.domain.ContactListItem;
import si.contactgroups.domain.Normaliser;
import si.contactgroups.domain.StoredContact;

/** The Contacts of a Group (table {@code contact}); every method is scoped to one Group. */
@Repository
public class ContactRepository {

  private static final String GROUP_ID = "groupId";
  private static final String COLUMNS =
      "id, version, first_name, last_name, email, phone, company, job_title, street,"
          + " postal_code, city, country, notes, created_at, updated_at";

  /** The columns the Contact list can be sorted by (contract parameter {@code sort}). */
  public static final Set<String> SORT_COLUMNS =
      Set.of("lastName", "firstName", "email", "company", "city", "country", "tags");

  /** Sort expression of each column: normalised text, so case and accents do not count. */
  private static final Map<String, String> SORT_KEY =
      Map.of(
          "lastName", "c.last_name_norm",
          "firstName", "c.first_name_norm",
          "email", "c.email_norm",
          "company", "c.company_norm",
          "city", "c.city_norm",
          "country", "c.country",
          "tags",
              "(SELECT min(lower(t.name)) FROM contact_tag ct JOIN tag t ON t.id = ct.tag_id"
                  + " WHERE ct.contact_id = c.id)");

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
                    + " country, notes, first_name_norm, last_name_norm, city_norm, company_norm, email_norm,"
                    + " search_norm) VALUES (:groupId, :createdBy, :now, :now, :firstName,"
                    + " :lastName, :email, :phone, :company, :jobTitle, :street, :postalCode,"
                    + " :city, :country, :notes, :firstNameNorm, :lastNameNorm, :cityNorm, :companyNorm,"
                    + " :emailNorm, :searchNorm) RETURNING id"),
            data)
        .param(GROUP_ID, groupId)
        .param("createdBy", createdBy)
        .param("now", Timestamp.from(now))
        .query(UUID.class)
        .single();
  }

  /**
   * The number of Contacts of the Group that are not in Trash and match every word.
   *
   * @param patterns LIKE patterns from {@code SearchWords}; empty means no search
   */
  public int count(UUID groupId, List<String> patterns) {
    return bindWords(
            jdbc.sql(
                "SELECT count(*) FROM contact c WHERE c.group_id = :groupId"
                    + " AND c.deleted_at IS NULL"
                    + wordConditions(patterns)),
            patterns)
        .param(GROUP_ID, groupId)
        .query(Integer.class)
        .single();
  }

  /**
   * Every word must be in the search text of the Contact, in a Tag name or in an Attribute value
   * (R-008-01, R-008-02); the patterns are bound, never joined into the SQL (R-008-06).
   */
  private static String wordConditions(List<String> patterns) {
    StringBuilder sql = new StringBuilder();
    for (int i = 0; i < patterns.size(); i++) {
      sql.append(
          " AND (c.search_norm LIKE :word"
              + i
              + " ESCAPE '\\'"
              + " OR EXISTS (SELECT 1 FROM contact_tag ct JOIN tag t ON t.id = ct.tag_id"
              + " WHERE ct.contact_id = c.id AND t.name_norm LIKE :word"
              + i
              + " ESCAPE '\\')"
              + " OR EXISTS (SELECT 1 FROM attribute a WHERE a.contact_id = c.id"
              + " AND a.value_norm LIKE :word"
              + i
              + " ESCAPE '\\'))");
    }
    return sql.toString();
  }

  private static JdbcClient.StatementSpec bindWords(
      JdbcClient.StatementSpec sql, List<String> patterns) {
    for (int i = 0; i < patterns.size(); i++) {
      sql = sql.param("word" + i, patterns.get(i));
    }
    return sql;
  }

  /**
   * One page of the Contacts not in Trash. The text columns sort with empty values last, ties by
   * last name, first name and id so that the order is stable.
   *
   * @param column one of {@link #SORT_COLUMNS}
   */
  public List<ContactListItem> page(
      UUID groupId,
      List<String> patterns,
      String column,
      boolean descending,
      int offset,
      int limit) {
    String key = SORT_KEY.get(column);
    String dir = descending ? "DESC" : "ASC";
    return bindWords(
            jdbc.sql(
            "SELECT c.id, c.first_name, c.last_name, c.email, c.company, c.city, c.country,"
                + " c.version, c.created_at,"
                + " COALESCE((SELECT array_agg(t.name ORDER BY lower(t.name), t.name)"
                + " FROM contact_tag ct JOIN tag t ON t.id = ct.tag_id"
                + " WHERE ct.contact_id = c.id), '{}') AS tags"
                + " FROM contact c WHERE c.group_id = :groupId AND c.deleted_at IS NULL"
                + wordConditions(patterns)
                + " ORDER BY ("
                + key
                + " IS NULL OR "
                + key
                + " = '') ASC, "
                + key
                + " "
                + dir
                + ", c.last_name_norm ASC NULLS LAST, c.first_name_norm ASC NULLS LAST, c.id"
                + " LIMIT :limit OFFSET :offset"),
            patterns)
        .param(GROUP_ID, groupId)
        .param("limit", limit)
        .param("offset", offset)
        .query(
            (rs, n) ->
                new ContactListItem(
                    rs.getObject("id", UUID.class),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("company"),
                    rs.getString("city"),
                    rs.getString("country"),
                    List.of((String[]) rs.getArray("tags").getArray()),
                    rs.getInt("version"),
                    rs.getTimestamp("created_at").toInstant()))
        .list();
  }

  /** Whether the Group has the Contact, in Trash or not. */
  public boolean existsIncludingTrash(UUID groupId, UUID contactId) {
    return jdbc.sql("SELECT EXISTS (SELECT 1 FROM contact WHERE id = :id AND group_id = :groupId)")
        .param("id", contactId)
        .param(GROUP_ID, groupId)
        .query(Boolean.class)
        .single();
  }

  /** The Contact of the Group that is not in Trash; empty when there is none (BR-01). */
  public Optional<StoredContact> find(UUID groupId, UUID contactId) {
    return jdbc.sql(
            "SELECT "
                + COLUMNS
                + " FROM contact WHERE id = :id AND group_id = :groupId AND deleted_at IS NULL")
        .param("id", contactId)
        .param(GROUP_ID, groupId)
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
                        + " first_name_norm = :firstNameNorm, last_name_norm = :lastNameNorm, city_norm = :cityNorm,"
                        + " company_norm = :companyNorm, email_norm = :emailNorm,"
                        + " search_norm = :searchNorm WHERE id = :id AND group_id = :groupId"
                        + " AND version = :version AND deleted_at IS NULL"),
                data)
            .param("id", contactId)
            .param(GROUP_ID, groupId)
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
        .param("cityNorm", Normaliser.normalise(d.city()))
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
