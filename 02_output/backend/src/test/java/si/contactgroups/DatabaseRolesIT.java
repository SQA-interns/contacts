package si.contactgroups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import si.contactgroups.support.PostgresSupport;

/**
 * AR-04 (schema only through migrations) and AR-09 (History append-only for the application role,
 * ADR-03): the application connects as a restricted role.
 */
class DatabaseRolesIT {

  private static final String PERMISSION_DENIED = "42501";

  @BeforeAll
  static void migrate() {
    Flyway.configure()
        .dataSource(
            PostgresSupport.jdbcUrl(), PostgresSupport.ownerUser(), PostgresSupport.ownerPassword())
        .placeholders(
            Map.of(
                "app_user", PostgresSupport.APP_USER,
                "app_password", PostgresSupport.APP_PASSWORD))
        .cleanDisabled(true)
        .load()
        .migrate();
  }

  private static Connection app() throws SQLException {
    return DriverManager.getConnection(
        PostgresSupport.jdbcUrl(), PostgresSupport.APP_USER, PostgresSupport.APP_PASSWORD);
  }

  private static UUID newGroupWithEntry(Connection c) throws SQLException {
    UUID group = UUID.randomUUID();
    try (Statement s = c.createStatement()) {
      s.executeUpdate("INSERT INTO contact_group (id, name) VALUES ('" + group + "', 'Acme')");
      s.executeUpdate(
          "INSERT INTO history_entry (group_id, user_name, user_email, action, details) VALUES ('"
              + group
              + "', 'Ana', 'ana@example.com', 'CONTACT_CREATED', '{\"lastName\":{\"new\":\"Novak\"}}')");
    }
    return group;
  }

  @Test
  void applicationRoleCanAppendAndReadHistory() throws Exception {
    try (Connection c = app()) {
      UUID group = newGroupWithEntry(c);
      try (Statement s = c.createStatement();
          var rs =
              s.executeQuery(
                  "SELECT count(*) FROM history_entry WHERE group_id = '" + group + "'")) {
        rs.next();
        assertThat(rs.getInt(1)).isEqualTo(1);
      }
    }
  }

  @Test
  void applicationRoleCannotChangeTheActionOfAnEntry() throws Exception {
    try (Connection c = app()) {
      newGroupWithEntry(c);
      assertThatThrownBy(
              () ->
                  c.createStatement()
                      .executeUpdate("UPDATE history_entry SET action = 'CONTACT_EDITED'"))
          .isInstanceOfSatisfying(
              SQLException.class, e -> assertThat(e.getSQLState()).isEqualTo(PERMISSION_DENIED));
    }
  }

  @Test
  void applicationRoleCannotChangeTimeOrUserName() throws Exception {
    try (Connection c = app()) {
      newGroupWithEntry(c);
      for (String column :
          new String[] {
            "occurred_at = now()", "user_name = 'Eve'", "contact_id = gen_random_uuid()"
          }) {
        assertThatThrownBy(
                () -> c.createStatement().executeUpdate("UPDATE history_entry SET " + column))
            .isInstanceOfSatisfying(
                SQLException.class, e -> assertThat(e.getSQLState()).isEqualTo(PERMISSION_DENIED));
      }
    }
  }

  @Test
  void applicationRoleMayReplaceDetailsAndClearTheEmailOnly() throws Exception {
    try (Connection c = app()) {
      UUID group = newGroupWithEntry(c);
      try (Statement s = c.createStatement()) {
        assertThat(
                s.executeUpdate(
                    "UPDATE history_entry SET details = '{\"lastName\":\"removed\"}' WHERE group_id = '"
                        + group
                        + "'"))
            .isEqualTo(1);
        assertThat(
                s.executeUpdate(
                    "UPDATE history_entry SET user_email = NULL WHERE group_id = '" + group + "'"))
            .isEqualTo(1);
      }
    }
  }

  @Test
  void applicationRoleCannotDeleteHistoryDirectly() throws Exception {
    try (Connection c = app()) {
      newGroupWithEntry(c);
      assertThatThrownBy(() -> c.createStatement().executeUpdate("DELETE FROM history_entry"))
          .isInstanceOfSatisfying(
              SQLException.class, e -> assertThat(e.getSQLState()).isEqualTo(PERMISSION_DENIED));
    }
  }

  @Test
  void deletingAGroupRemovesItsHistoryThroughTheCascade() throws Exception {
    try (Connection c = app()) {
      UUID group = newGroupWithEntry(c);
      try (Statement s = c.createStatement()) {
        assertThat(s.executeUpdate("DELETE FROM contact_group WHERE id = '" + group + "'"))
            .isEqualTo(1);
        try (var rs =
            s.executeQuery("SELECT count(*) FROM history_entry WHERE group_id = '" + group + "'")) {
          rs.next();
          assertThat(rs.getInt(1)).isZero();
        }
      }
    }
  }

  @Test
  void applicationRoleCannotCreateOrAlterTables() throws Exception {
    try (Connection c = app()) {
      for (String ddl :
          new String[] {
            "CREATE TABLE evil (id int)",
            "ALTER TABLE contact ADD COLUMN evil int",
            "DROP TABLE contact CASCADE"
          }) {
        assertThatThrownBy(() -> c.createStatement().execute(ddl))
            .isInstanceOfSatisfying(
                SQLException.class,
                e -> assertThat(e.getSQLState()).isIn(PERMISSION_DENIED, "42501", "42P07"));
      }
    }
  }

  @Test
  void flywayRecordedAllMigrations() throws Exception {
    try (Connection c =
            DriverManager.getConnection(
                PostgresSupport.jdbcUrl(),
                PostgresSupport.ownerUser(),
                PostgresSupport.ownerPassword());
        Statement s = c.createStatement();
        var rs = s.executeQuery("SELECT count(*) FROM flyway_schema_history WHERE success")) {
      rs.next();
      assertThat(rs.getInt(1)).isEqualTo(3);
    }
  }
}
