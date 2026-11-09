package si.contactgroups.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import si.contactgroups.domain.Normaliser;
import si.contactgroups.domain.TagCount;

/** The Tags of a Group and their links to Contacts; every method is scoped to one Group. */
@Repository
public class TagRepository {

  private static final String GROUP_ID = "groupId";
  private static final String CONTACT_ID = "contactId";
  private static final String NAME = "name";
  private static final String TAG_ID = "tagId";

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

  /** Every Tag of the Group with the number of Contacts that carry it, Trash included. */
  public List<TagCount> counts(UUID groupId) {
    return jdbc.sql(
            "SELECT t.id, t.name, count(ct.contact_id) AS contact_count FROM tag t"
                + " LEFT JOIN contact_tag ct ON ct.tag_id = t.id"
                + " WHERE t.group_id = :groupId GROUP BY t.id, t.name"
                + " ORDER BY lower(t.name), t.name")
        .param(GROUP_ID, groupId)
        .query(
            (rs, i) ->
                new TagCount(
                    rs.getObject("id", UUID.class), rs.getString(NAME), rs.getInt("contact_count")))
        .list();
  }

  /** The Tag with the id in this Group. */
  public Optional<TagCount> find(UUID groupId, UUID tagId) {
    return counts(groupId).stream().filter(t -> t.id().equals(tagId)).findFirst();
  }

  /** Whether another Tag of the Group has the name ignoring case (BR-05). */
  public boolean nameTakenByOther(UUID groupId, UUID tagId, String name) {
    return jdbc.sql(
                "SELECT count(*) FROM tag WHERE group_id = :groupId AND id <> :tagId"
                    + " AND lower(name) = lower(:name)")
            .param(GROUP_ID, groupId)
            .param(TAG_ID, tagId)
            .param(NAME, name)
            .query(Integer.class)
            .single()
        > 0;
  }

  public void rename(UUID groupId, UUID tagId, String name) {
    jdbc.sql(
            "UPDATE tag SET name = :name, name_norm = :norm"
                + " WHERE id = :tagId AND group_id = :groupId")
        .param(GROUP_ID, groupId)
        .param(TAG_ID, tagId)
        .param(NAME, name)
        .param("norm", Normaliser.normalise(name))
        .update();
  }

  /** The ids of all Contacts of the Group, Trash included, that carry the Tag. */
  public List<UUID> contactIds(UUID groupId, UUID tagId) {
    return jdbc.sql(
            "SELECT ct.contact_id FROM contact_tag ct JOIN tag t ON t.id = ct.tag_id"
                + " WHERE t.id = :tagId AND t.group_id = :groupId")
        .param(GROUP_ID, groupId)
        .param(TAG_ID, tagId)
        .query(UUID.class)
        .list();
  }

  /** Deletes the Tag and its links; no Contact is touched (R-006-05). */
  public void delete(UUID groupId, UUID tagId) {
    jdbc.sql("DELETE FROM tag WHERE id = :tagId AND group_id = :groupId")
        .param(GROUP_ID, groupId)
        .param(TAG_ID, tagId)
        .update();
  }
}
