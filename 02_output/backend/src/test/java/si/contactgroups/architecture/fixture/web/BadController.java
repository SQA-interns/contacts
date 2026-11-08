package si.contactgroups.architecture.fixture.web;

import si.contactgroups.architecture.fixture.persistence.Repo;

/** Fixture: a controller must not skip the service layer (violates AR-01). */
public class BadController {
  public String run() {
    return new Repo().load();
  }
}
