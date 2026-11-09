package si.contactgroups.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import si.contactgroups.domain.AttributePair;
import si.contactgroups.domain.ContactData;
import si.contactgroups.domain.ContactFilter;
import si.contactgroups.domain.ContactListItem;
import si.contactgroups.domain.DuplicateCandidate;
import si.contactgroups.domain.ExpiredContact;
import si.contactgroups.domain.ExportContact;
import si.contactgroups.domain.Facets;
import si.contactgroups.domain.Normaliser;
import si.contactgroups.domain.StoredContact;
import si.contactgroups.domain.TrashedContact;

/** The Contacts of a Group (table {@code contact}); every method is scoped to one Group. */
@Repository
public class ContactRepository {

  private static final String GROUP_ID = "groupId";
  private static final String IDS = "ids";
  private static final String FIRST_NAME = "first_name";
  private static final String LAST_NAME = "last_name";
  private static final String EMAIL = "email";
  private static final String PHONE = "phone";
  private static final String COMPANY = "company";
  private static final String CITY = "city";
  private static final String COUNTRY = "country";
  private static final String COLUMNS =
      "id, version, first_name, last_name, email, phone, company, job_title, street,"
          + " postal_code, city, country, notes, created_at, updated_at";

  /** The columns of a {@link ContactListItem}; the caller adds FROM contact c. */
  private static final String LIST_ITEM_SELECT =
      "SELECT c.id, c.first_name, c.last_name, c.email, c.company, c.city, c.country,"
          + " c.version, c.created_at,"
          + " COALESCE((SELECT array_agg(t.name ORDER BY lower(t.name), t.name)"
          + " FROM contact_tag ct JOIN tag t ON t.id = ct.tag_id"
          + " WHERE ct.contact_id = c.id), '{}') AS tags";

  /** The columns the Contact list can be sorted by (contract parameter {@code sort}). */
  public static final Set<String> SORT_COLUMNS =
      Set.of("lastName", "firstName", EMAIL, COMPANY, CITY, COUNTRY, "tags");

  /** Sort expression of each column: normalised text, so case and accents do not count. */
  private static final Map<String, String> SORT_KEY =
      Map.of(
          "lastName",
          "c.last_name_norm",
          "firstName",
          "c.first_name_norm",
          EMAIL,
          "c.email_norm",
          COMPANY,
          "c.company_norm",
          CITY,
          "c.city_norm",
          COUNTRY,
          "c.country",
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

  /** The number of Contacts of the Group that are not in Trash and match the search and filters. */
  public int count(UUID groupId, ContactFilter filter) {
    return FilterSql.bind(
            jdbc.sql(
                "SELECT count(*) FROM contact c WHERE c.group_id = :groupId"
                    + " AND c.deleted_at IS NULL"
                    + conditions(filter)),
            filter)
        .param(GROUP_ID, groupId)
        .query(Integer.class)
        .single();
  }

  /**
   * Search and filters joined by AND (R-009-08). Every word must be in the search text of the
   * Contact, in a Tag name or in an Attribute value (R-008-01, R-008-02). All values are bound,
   * never joined into the SQL (R-008-06); only the numbers of the parameters are.
   */
  private static String conditions(ContactFilter filter) {
    return wordConditions(filter.words()) + FilterSql.conditions(filter);
  }

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

  /**
   * The values the filters offer for the Group (R-009-11): Tags of Contacts not in Trash, and the
   * companies, countries and Attribute keys of those Contacts, sorted without case and accents.
   */
  public Facets facets(UUID groupId) {
    return new Facets(
        facetValues(
            "SELECT min(t.name) FROM contact_tag ct JOIN tag t ON t.id = ct.tag_id"
                + " JOIN contact c ON c.id = ct.contact_id"
                + " WHERE c.group_id = :groupId AND c.deleted_at IS NULL"
                + " GROUP BY t.name_norm ORDER BY t.name_norm",
            groupId),
        facetValues(
            "SELECT min(c.company) FROM contact c WHERE c.group_id = :groupId"
                + " AND c.deleted_at IS NULL AND c.company_norm IS NOT NULL AND c.company_norm <> ''"
                + " GROUP BY c.company_norm ORDER BY c.company_norm",
            groupId),
        facetValues(
            "SELECT DISTINCT c.country FROM contact c WHERE c.group_id = :groupId"
                + " AND c.deleted_at IS NULL AND c.country IS NOT NULL ORDER BY c.country",
            groupId),
        facetValues(
            "SELECT min(a.key) FROM attribute a JOIN contact c ON c.id = a.contact_id"
                + " WHERE c.group_id = :groupId AND c.deleted_at IS NULL"
                + " GROUP BY lower(a.key) ORDER BY lower(a.key)",
            groupId));
  }

  private List<String> facetValues(String sql, UUID groupId) {
    return jdbc.sql(sql).param(GROUP_ID, groupId).query(String.class).list();
  }

  /**
   * One page of the Contacts not in Trash. The text columns sort with empty values last, ties by
   * last name, first name and id so that the order is stable.
   *
   * @param column one of {@link #SORT_COLUMNS}
   */
  public List<ContactListItem> page(
      UUID groupId,
      ContactFilter filter,
      String column,
      boolean descending,
      int offset,
      int limit) {
    String key = SORT_KEY.get(column);
    String dir = descending ? "DESC" : "ASC";
    return FilterSql.bind(
            jdbc.sql(
                LIST_ITEM_SELECT
                    + " FROM contact c WHERE c.group_id = :groupId AND c.deleted_at IS NULL"
                    + conditions(filter)
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
            filter)
        .param(GROUP_ID, groupId)
        .param("limit", limit)
        .param("offset", offset)
        .query((rs, n) -> listItemOf(rs))
        .list();
  }

  /**
   * Every Contact of the Group not in Trash with its phone number, for the duplicate search
   * (US-024); oldest first.
   */
  public List<DuplicateCandidate> duplicateCandidates(UUID groupId) {
    return jdbc.sql(
            LIST_ITEM_SELECT
                + ", c.phone FROM contact c WHERE c.group_id = :groupId AND c.deleted_at IS NULL"
                + " ORDER BY c.created_at, c.id")
        .param(GROUP_ID, groupId)
        .query((rs, n) -> new DuplicateCandidate(listItemOf(rs), rs.getString(PHONE)))
        .list();
  }

  private static ContactListItem listItemOf(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new ContactListItem(
        rs.getObject("id", UUID.class),
        rs.getString(FIRST_NAME),
        rs.getString(LAST_NAME),
        rs.getString(EMAIL),
        rs.getString(COMPANY),
        rs.getString(CITY),
        rs.getString(COUNTRY),
        List.of((String[]) rs.getArray("tags").getArray()),
        rs.getInt("version"),
        rs.getTimestamp("created_at").toInstant());
  }

  /** The ids of the Contacts of the Group, not in Trash, that match the search and filters. */
  public List<UUID> idsMatching(UUID groupId, ContactFilter filter) {
    return FilterSql.bind(
            jdbc.sql(
                "SELECT c.id FROM contact c WHERE c.group_id = :groupId"
                    + " AND c.deleted_at IS NULL"
                    + conditions(filter)
                    + " ORDER BY c.last_name_norm NULLS LAST, c.first_name_norm NULLS LAST, c.id"),
            filter)
        .param(GROUP_ID, groupId)
        .query(UUID.class)
        .list();
  }

  /** Those of the given ids that are Contacts of the Group not in Trash, in list order. */
  public List<UUID> exportableIds(UUID groupId, List<UUID> contactIds) {
    return jdbc.sql(
            "SELECT id FROM contact WHERE group_id = :groupId AND deleted_at IS NULL"
                + " AND id = ANY(:ids)"
                + " ORDER BY last_name_norm NULLS LAST, first_name_norm NULLS LAST, id")
        .param(GROUP_ID, groupId)
        .param(IDS, contactIds.toArray(UUID[]::new))
        .query(UUID.class)
        .list();
  }

  /** The data, Tags and Attributes of the Contacts, in the order of {@code contactIds}. */
  public List<ExportContact> forExport(UUID groupId, List<UUID> contactIds) {
    UUID[] ids = contactIds.toArray(UUID[]::new);
    Map<UUID, List<String>> tags = tagsOf(groupId, ids);
    Map<UUID, List<AttributePair>> attributes = attributesOf(groupId, ids);
    Map<UUID, ContactData> data = dataOf(groupId, ids);
    return contactIds.stream()
        .map(
            id ->
                new ExportContact(
                    data.get(id),
                    tags.getOrDefault(id, List.of()),
                    attributes.getOrDefault(id, List.of())))
        .toList();
  }

  private Map<UUID, ContactData> dataOf(UUID groupId, UUID[] ids) {
    Map<UUID, ContactData> data = new HashMap<>();
    jdbc.sql(
            "SELECT "
                + COLUMNS
                + " FROM contact WHERE group_id = :groupId AND deleted_at IS NULL"
                + " AND id = ANY(:ids)")
        .param(GROUP_ID, groupId)
        .param(IDS, ids)
        .query((rs, n) -> data.put(rs.getObject("id", UUID.class), dataOf(rs)))
        .list();
    return data;
  }

  private static ContactData dataOf(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new ContactData(
        rs.getString(FIRST_NAME),
        rs.getString(LAST_NAME),
        rs.getString(EMAIL),
        rs.getString(PHONE),
        rs.getString(COMPANY),
        rs.getString("job_title"),
        rs.getString("street"),
        rs.getString("postal_code"),
        rs.getString(CITY),
        rs.getString(COUNTRY),
        rs.getString("notes"));
  }

  private Map<UUID, List<String>> tagsOf(UUID groupId, UUID[] ids) {
    Map<UUID, List<String>> tags = new HashMap<>();
    jdbc.sql(
            "SELECT ct.contact_id, t.name FROM contact_tag ct JOIN tag t ON t.id = ct.tag_id"
                + " WHERE t.group_id = :groupId AND ct.contact_id = ANY(:ids)")
        .param(GROUP_ID, groupId)
        .param(IDS, ids)
        .query(
            (rs, n) ->
                tags.computeIfAbsent(rs.getObject("contact_id", UUID.class), k -> new ArrayList<>())
                    .add(rs.getString("name")))
        .list();
    return tags;
  }

  private Map<UUID, List<AttributePair>> attributesOf(UUID groupId, UUID[] ids) {
    Map<UUID, List<AttributePair>> attributes = new HashMap<>();
    jdbc.sql(
            "SELECT a.contact_id, a.key, a.value FROM attribute a JOIN contact c ON c.id = a.contact_id"
                + " WHERE c.group_id = :groupId AND a.contact_id = ANY(:ids)"
                + " ORDER BY lower(a.key), a.key")
        .param(GROUP_ID, groupId)
        .param(IDS, ids)
        .query(
            (rs, n) ->
                attributes
                    .computeIfAbsent(rs.getObject("contact_id", UUID.class), k -> new ArrayList<>())
                    .add(new AttributePair(rs.getString("key"), rs.getString("value"))))
        .list();
    return attributes;
  }

  /** Whether the Group has the Contact, in Trash or not. */
  public boolean existsIncludingTrash(UUID groupId, UUID contactId) {
    return jdbc.sql("SELECT EXISTS (SELECT 1 FROM contact WHERE id = :id AND group_id = :groupId)")
        .param("id", contactId)
        .param(GROUP_ID, groupId)
        .query(Boolean.class)
        .single();
  }

  /**
   * Puts the Contact in Trash, only when it is not there yet; of two requests at once, one changes
   * the row (R-012-09, EX-012-04).
   *
   * @return whether the Contact was moved by this call
   */
  public boolean moveToTrash(UUID groupId, UUID contactId, Instant now) {
    return jdbc.sql(
                "UPDATE contact SET deleted_at = :now WHERE id = :id AND group_id = :groupId"
                    + " AND deleted_at IS NULL")
            .param("id", contactId)
            .param(GROUP_ID, groupId)
            .param("now", Timestamp.from(now))
            .update()
        == 1;
  }

  /**
   * Puts the Contact in Trash only when it is not there yet and still has version {@code version},
   * so a merge never trashes a Contact that changed after the user saw it (R-026-08).
   *
   * @return whether the Contact was moved by this call
   */
  public boolean moveToTrashAtVersion(UUID groupId, UUID contactId, int version, Instant now) {
    return jdbc.sql(
                "UPDATE contact SET deleted_at = :now WHERE id = :id AND group_id = :groupId"
                    + " AND version = :version AND deleted_at IS NULL")
            .param("id", contactId)
            .param(GROUP_ID, groupId)
            .param("version", version)
            .param("now", Timestamp.from(now))
            .update()
        == 1;
  }

  /**
   * Takes the Contact out of Trash when it went in after {@code cutoff}; fields, Tags, Attributes
   * and version number stay as they are (R-012-04, R-012-05).
   *
   * @return whether the Contact was restored
   */
  public boolean restoreFromTrash(UUID groupId, UUID contactId, Instant cutoff) {
    return jdbc.sql(
                "UPDATE contact SET deleted_at = NULL WHERE id = :id AND group_id = :groupId"
                    + " AND deleted_at > :cutoff")
            .param("id", contactId)
            .param(GROUP_ID, groupId)
            .param("cutoff", Timestamp.from(cutoff))
            .update()
        == 1;
  }

  /**
   * Deletes a Contact that is in Trash with its Tags links, Attributes and Versions (cascade); of
   * two requests at once, one deletes the row (R-013-01, EX-013-04).
   *
   * @return whether the Contact was deleted by this call
   */
  public boolean deleteFromTrash(UUID groupId, UUID contactId) {
    return jdbc.sql(
                "DELETE FROM contact WHERE id = :id AND group_id = :groupId"
                    + " AND deleted_at IS NOT NULL")
            .param("id", contactId)
            .param(GROUP_ID, groupId)
            .update()
        == 1;
  }

  /** The Contacts of all Groups that went to Trash at or before {@code cutoff} (R-013-04). */
  @GlobalScope
  public List<ExpiredContact> listExpired(Instant cutoff) {
    return jdbc.sql(
            "SELECT id, group_id FROM contact WHERE deleted_at <= :cutoff ORDER BY deleted_at")
        .param("cutoff", Timestamp.from(cutoff))
        .query(
            (rs, n) ->
                new ExpiredContact(
                    rs.getObject("group_id", UUID.class), rs.getObject("id", UUID.class)))
        .list();
  }

  /**
   * The Contacts of the Group that went to Trash after {@code cutoff}, the latest first, with the
   * name of the user of the last deletion entry of the History (D-18f).
   */
  public List<TrashedContact> listTrash(UUID groupId, Instant cutoff) {
    return jdbc.sql(
            "SELECT c.id, c.first_name, c.last_name, c.email, c.company, c.deleted_at,"
                + " (SELECT h.user_name FROM history_entry h WHERE h.contact_id = c.id"
                + " AND h.action = 'CONTACT_DELETED' ORDER BY h.occurred_at DESC, h.id DESC"
                + " LIMIT 1) AS deleted_by"
                + " FROM contact c WHERE c.group_id = :groupId AND c.deleted_at > :cutoff"
                + " ORDER BY c.deleted_at DESC, c.id")
        .param(GROUP_ID, groupId)
        .param("cutoff", Timestamp.from(cutoff))
        .query(
            (rs, n) ->
                new TrashedContact(
                    rs.getObject("id", UUID.class),
                    rs.getString(FIRST_NAME),
                    rs.getString(LAST_NAME),
                    rs.getString(EMAIL),
                    rs.getString(COMPANY),
                    rs.getTimestamp("deleted_at").toInstant(),
                    rs.getString("deleted_by")))
        .list();
  }

  /** Whether the Contact of the Group is in Trash now. */
  public boolean isInTrash(UUID groupId, UUID contactId) {
    return jdbc.sql(
            "SELECT EXISTS (SELECT 1 FROM contact WHERE id = :id AND group_id = :groupId"
                + " AND deleted_at IS NOT NULL)")
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
                        rs.getString(FIRST_NAME),
                        rs.getString(LAST_NAME),
                        rs.getString(EMAIL),
                        rs.getString(PHONE),
                        rs.getString(COMPANY),
                        rs.getString("job_title"),
                        rs.getString("street"),
                        rs.getString("postal_code"),
                        rs.getString(CITY),
                        rs.getString(COUNTRY),
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
        .param(EMAIL, d.email())
        .param(PHONE, d.phone())
        .param(COMPANY, d.company())
        .param("jobTitle", d.jobTitle())
        .param("street", d.street())
        .param("postalCode", d.postalCode())
        .param(CITY, d.city())
        .param(COUNTRY, d.country())
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
