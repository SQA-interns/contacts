package si.contactgroups.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Architecture constraints a tool can check (docs/02_adr/ADR-01, AR-01, AR-05, AR-10). */
@AnalyzeClasses(packages = "si.contactgroups", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  /** AR-01: web -> service -> persistence, no layer calls a layer above it. */
  @ArchTest static final ArchRule layersAreRespected = LayerRules.layers("si.contactgroups");

  /** AR-01: only repositories touch the database. */
  @ArchTest
  static final ArchRule onlyPersistenceUsesJdbc =
      noClasses()
          .that()
          .resideOutsideOfPackages(
              "si.contactgroups.persistence..", "si.contactgroups.config..", "db.migration..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("java.sql..", "org.springframework.jdbc..")
          .because("controllers and services call repositories, never the database (AR-01)");

  @ArchTest
  static final ArchRule domainDependsOnNothingElse =
      noClasses()
          .that()
          .resideInAPackage("si.contactgroups.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "si.contactgroups.web..",
              "si.contactgroups.service..",
              "si.contactgroups.persistence..",
              "si.contactgroups.security..",
              "org.springframework..");

  /** AR-05: no hand-built statements; repositories bind named parameters. */
  @ArchTest
  static final ArchRule noRawJdbcStatements =
      noClasses()
          .that()
          .resideOutsideOfPackage("db.migration..")
          .should()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.sql.Statement")
          .because("SQL is always parameterised (AR-05)");

  /** AR-10: times come from the injected Clock, in UTC. */
  @ArchTest
  static final ArchRule noAmbientTime =
      noClasses()
          .should()
          .callMethod(java.time.LocalDateTime.class, "now")
          .orShould()
          .callMethod(java.time.LocalDate.class, "now")
          .orShould()
          .callMethod(java.time.Instant.class, "now")
          .orShould()
          .callMethod(java.time.ZonedDateTime.class, "now")
          .orShould()
          .callMethod(java.time.ZoneId.class, "systemDefault")
          .orShould()
          .callMethod(System.class, "currentTimeMillis")
          .because("use the injected Clock (AR-10)");

  /** AR-02 over the real repositories; the self-test below proves the rule can fail. */
  @ArchTest
  static final ArchRule repositoriesTakeGroupIdFirst =
      RepositoryScopeRule.rule("si.contactgroups.persistence..");
}
