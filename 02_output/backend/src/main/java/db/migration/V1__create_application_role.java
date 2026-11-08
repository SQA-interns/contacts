package db.migration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.regex.Pattern;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Creates (or updates the password of) the restricted application role (ADR-03, AR-09). A Java
 * migration is used so that the password is bound as a value and never pasted into SQL text: the
 * statement is built by PostgreSQL itself with {@code format('%I', ...)} and {@code '%L'}.
 */
public class V1__create_application_role extends BaseJavaMigration {

  private static final Pattern ROLE_NAME = Pattern.compile("[a-z_][a-z0-9_]{0,62}");

  @Override
  public void migrate(Context context) throws SQLException {
    Map<String, String> placeholders = context.getConfiguration().getPlaceholders();
    String role = placeholders.get("app_user");
    String password = placeholders.get("app_password");
    if (role == null || !ROLE_NAME.matcher(role).matches()) {
      throw new IllegalStateException("DB_APP_USER must match " + ROLE_NAME.pattern());
    }
    if (password == null || password.isEmpty()) {
      throw new IllegalStateException("DB_APP_PASSWORD must not be empty");
    }
    boolean exists;
    try (PreparedStatement ps =
        context.getConnection().prepareStatement("SELECT 1 FROM pg_roles WHERE rolname = ?")) {
      ps.setString(1, role);
      try (ResultSet rs = ps.executeQuery()) {
        exists = rs.next();
      }
    }
    String verb =
        exists ? "ALTER ROLE %I WITH LOGIN PASSWORD %L" : "CREATE ROLE %I WITH LOGIN PASSWORD %L";
    String statement;
    try (PreparedStatement ps =
        context.getConnection().prepareStatement("SELECT format(?, ?, ?)")) {
      ps.setString(1, verb);
      ps.setString(2, role);
      ps.setString(3, password);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        statement = rs.getString(1);
      }
    }
    try (Statement st = context.getConnection().createStatement()) {
      st.execute(statement);
    }
  }
}
