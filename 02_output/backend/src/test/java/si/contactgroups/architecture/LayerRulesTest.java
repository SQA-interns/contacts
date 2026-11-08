package si.contactgroups.architecture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import si.contactgroups.architecture.fixture.persistence.Repo;
import si.contactgroups.architecture.fixture.service.Svc;
import si.contactgroups.architecture.fixture.web.BadController;

/** The layer rule must fail when a controller calls a repository (AR-01). */
class LayerRulesTest {

  private static final String ROOT = "si.contactgroups.architecture.fixture";

  @Test
  void controllerCallingRepositoryIsAViolation() {
    assertThatThrownBy(
            () ->
                LayerRules.layers(ROOT)
                    .check(new ClassFileImporter().importClasses(BadController.class, Repo.class)))
        .hasMessageContaining("BadController");
  }

  @Test
  void serviceCallingRepositoryIsAllowed() {
    assertThatCode(
            () ->
                LayerRules.layers(ROOT)
                    .check(new ClassFileImporter().importClasses(Svc.class, Repo.class)))
        .doesNotThrowAnyException();
  }
}
