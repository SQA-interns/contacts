package si.contactgroups.acceptance.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import si.contactgroups.support.PostgresSupport;

/**
 * Direct database access with the owner role, used only to empty the database between tests, to
 * load large fixtures the way {@code seed/generate-large.sql} does, and to check that purged rows
 * are gone (AC-013-04, AC-020-04). The schema is the persistence contract (spec, AR-04).
 */
public final class Db {

  private Db() {}

  private static Connection connect() throws SQLException {
    return DriverManager.getConnection(
        PostgresSupport.jdbcUrl(), PostgresSupport.ownerUser(), PostgresSupport.ownerPassword());
  }

  /** Empties every table of the product (not the migration history). */
  public static void truncateAll() {
    try (Connection c = connect();
        Statement s = c.createStatement()) {
      List<String> tables = new ArrayList<>();
      try (ResultSet rs =
          s.executeQuery(
              "select tablename from pg_tables where schemaname = 'public'"
                  + " and tablename <> 'flyway_schema_history'")) {
        while (rs.next()) {
          tables.add("\"" + rs.getString(1) + "\"");
        }
      }
      if (!tables.isEmpty()) {
        s.execute("truncate table " + String.join(", ", tables) + " cascade");
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Number of rows of {@code table} whose {@code column} equals {@code id}. */
  public static long countWhere(String table, String column, String id) {
    try (Connection c = connect();
        PreparedStatement s =
            c.prepareStatement(
                "select count(*) from \"" + table + "\" where \"" + column + "\" = ?::uuid")) {
      s.setString(1, id);
      try (ResultSet rs = s.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Inserts {@code count} Contacts with last name {@code Bulk}, first name {@code Person<n>} and
   * company {@code Bulk d.o.o.} into a Group, as {@code seed/generate-large.sql} does; returns
   * nothing because the tests read them through the REST interface.
   */
  public static void insertContacts(String groupId, String createdByUserId, int count) {
    String sql =
        "insert into contact (id, group_id, first_name, last_name, company,"
            + " first_name_norm, last_name_norm, company_norm, email_norm, search_norm, created_by)"
            + " select gen_random_uuid(), ?::uuid, 'Person' || g, 'Bulk', 'Bulk d.o.o.',"
            + " 'person' || g, 'bulk', 'bulk d.o.o.', null,"
            + " 'person' || g || ' bulk bulk d.o.o.', ?::uuid from generate_series(1, ?) g";
    try (Connection c = connect();
        PreparedStatement s = c.prepareStatement(sql)) {
      s.setString(1, groupId);
      s.setString(2, createdByUserId);
      s.setInt(3, count);
      s.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Replaces a text in the stored content of one Version, whatever its JSON layout (AC-023-04:
   * content that an earlier rule accepted).
   */
  public static void replaceInVersion(String contactId, int number, String from, String to) {
    String sql =
        "update contact_version set snapshot = replace(snapshot::text, ?, ?)::jsonb"
            + " where contact_id = ?::uuid and number = ?";
    try (Connection c = connect();
        PreparedStatement s = c.prepareStatement(sql)) {
      s.setString(1, from);
      s.setString(2, to);
      s.setString(3, contactId);
      s.setInt(4, number);
      if (s.executeUpdate() != 1) {
        throw new IllegalStateException("no Version " + number + " of " + contactId);
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String randomId() {
    return UUID.randomUUID().toString();
  }
}
