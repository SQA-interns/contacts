package si.contactgroups.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import si.contactgroups.domain.Normaliser;

/** The Tags of a Group and their links to Contacts; every method is scoped to one Group. */
@Repository
public class TagRepository {

  private static final String GROUP_ID = "groupId";
  private static final String CONTACT_ID = "contactId";
  private static final String NAME = "name";

  private final JdbcClient jdbc;

  public TagRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** The names of the Tags on the Contact ordered by name ignoring case. */
  public List<String> names(UUID groupId, UUID contactId) {
    return jdbc.sql(
            "SELECT t.name FROM tag t JOIN contact_tag ct ON ct.tag_id = t.id"
                + " WHERE ct.contact_id = :contactId AND t.group_id = :groupId"
                + " ORDER BY lower(t.name), t.name")
        .param(GROUP_ID, groupId)
        .param(CONTACT_ID, contactId)
        .query(String.class)
        .list();
  }

  /**
   * Puts the Tag on the Contact and creates it in the Group when no Tag has that name ignoring case
   * (R-005-01, R-005-03).
   *
   * @return the name as the Group spells it
   */
  public String attach(UUID groupId, UUID contactId, String name) {
    jdbc.sql(
            "INSERT INTO tag (group_id, name, name_norm) VALUES (:groupId, :name, :norm)"
                + " ON CONFLICT (group_id, lower(name)) DO NOTHING")
        .param(GROUP_ID, groupId)
        .param(NAME, name)
        .param("norm", Normaliser.normalise(name))
        .update();
    return jdbc.sql(
            "WITH t AS (SELECT id, name FROM tag WHERE group_id = :groupId"
                + " AND lower(name) = lower(:name)),"
                + " l AS (INSERT INTO contact_tag (contact_id, tag_id)"
                + " SELECT c.id, t.id FROM t, contact c"
                + " WHERE c.id = :contactId AND c.group_id = :groupId"
                + " ON CONFLICT DO NOTHING)"
                + " SELECT name FROM t")
        .param(GROUP_ID, groupId)
        .param(CONTACT_ID, contactId)
        .param(NAME, name)
        .query(String.class)
        .single();
  }

  /** Removes only the link; the Tag stays in the Group (R-005-06). */
  public void detach(UUID groupId, UUID contactId, String name) {
    jdbc.sql(
            "DELETE FROM contact_tag WHERE contact_id = :contactId AND tag_id IN"
                + " (SELECT id FROM tag WHERE group_id = :groupId AND lower(name) = lower(:name))")
        .param(GROUP_ID, groupId)
        .param(CONTACT_ID, contactId)
        .param(NAME, name)
        .update();
  }
}
