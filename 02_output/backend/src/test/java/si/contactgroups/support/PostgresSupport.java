package si.contactgroups.support;

import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * One throwaway PostgreSQL per test JVM (environments.md, "test"). Same major version as
 * production.
 */
public final class PostgresSupport {

  public static final String APP_USER = "contactgroups_app";
  public static final String APP_PASSWORD = "app-test-password";

  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6");

  static {
    POSTGRES.start();
  }

  private PostgresSupport() {}

  public static String jdbcUrl() {
    return POSTGRES.getJdbcUrl();
  }

  public static String ownerUser() {
    return POSTGRES.getUsername();
  }

  public static String ownerPassword() {
    return POSTGRES.getPassword();
  }
}
