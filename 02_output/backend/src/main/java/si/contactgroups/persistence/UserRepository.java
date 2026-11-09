package si.contactgroups.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import si.contactgroups.domain.User;

/** The Users of the product; not scoped to a Group (AR-02). */
@Repository
@GlobalScope
public class UserRepository {

  private static final String COLUMNS = "id, firebase_uid, email, display_name, created_at";

  private final JdbcClient jdbc;

  public UserRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<User> findByFirebaseUid(String firebaseUid) {
    return jdbc.sql("SELECT " + COLUMNS + " FROM app_user WHERE firebase_uid = :uid")
        .param("uid", firebaseUid)
        .query(User.class)
        .optional();
  }

  public Optional<User> findById(UUID id) {
    return jdbc.sql("SELECT " + COLUMNS + " FROM app_user WHERE id = :id")
        .param("id", id)
        .query(User.class)
        .optional();
  }

  /**
   * Inserts the User unless the Firebase identifier or the e-mail is already stored; a concurrent
   * first request of the same person therefore never fails.
   */
  public void insertIfAbsent(
      String firebaseUid, String email, String displayName, Instant createdAt) {
    jdbc.sql(
            "INSERT INTO app_user (firebase_uid, email, display_name, created_at)"
                + " VALUES (:uid, :email, :name, :createdAt) ON CONFLICT DO NOTHING")
        .param("uid", firebaseUid)
        .param("email", email)
        .param("name", displayName)
        .param("createdAt", java.sql.Timestamp.from(createdAt))
        .update();
  }

  /** Deletes the User; the Memberships of the User must be gone (R-021-01). */
  public void delete(UUID id) {
    jdbc.sql("DELETE FROM app_user WHERE id = :id").param("id", id).update();
  }

  /** Locks the row of the User until the end of the transaction (R-002-03, R-020-08). */
  public void lock(UUID id) {
    jdbc.sql("SELECT id FROM app_user WHERE id = :id FOR UPDATE").param("id", id).query().rowSet();
  }
}
