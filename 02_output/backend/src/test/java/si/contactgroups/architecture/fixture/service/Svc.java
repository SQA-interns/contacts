package si.contactgroups.architecture.fixture.service;

import si.contactgroups.architecture.fixture.persistence.Repo;

/** Fixture: a service may call a repository. */
public class Svc {
  public String run() {
    return new Repo().load();
  }
}
