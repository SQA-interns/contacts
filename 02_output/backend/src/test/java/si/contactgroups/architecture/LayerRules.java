package si.contactgroups.architecture;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.lang.ArchRule;

/**
 * AR-01: web -> service -> persistence; jobs call services. Layers may be empty (the skeleton has
 * no logic yet).
 */
final class LayerRules {

  private LayerRules() {}

  static ArchRule layers(String root) {
    return layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .withOptionalLayers(true)
        .layer("Web")
        .definedBy(root + ".web..")
        .layer("Service")
        .definedBy(root + ".service..")
        .layer("Persistence")
        .definedBy(root + ".persistence..")
        .layer("Job")
        .definedBy(root + ".job..")
        .whereLayer("Web")
        .mayNotBeAccessedByAnyLayer()
        .whereLayer("Job")
        .mayNotBeAccessedByAnyLayer()
        .whereLayer("Service")
        .mayOnlyBeAccessedByLayers("Web", "Job")
        .whereLayer("Persistence")
        .mayOnlyBeAccessedByLayers("Service", "Job");
  }
}
