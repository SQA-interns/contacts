package si.contactgroups.web;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import si.contactgroups.domain.Action;
import si.contactgroups.domain.Facets;
import si.contactgroups.security.RequiresAction;
import si.contactgroups.service.SearchService;

/** The values the filters of the Contact list offer (contract {@code getFacets}, R-009-11). */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/facets")
public class FacetController {

  private final SearchService search;

  public FacetController(SearchService search) {
    this.search = search;
  }

  @GetMapping
  @RequiresAction(Action.READ_CONTACTS)
  public Facets getFacets(@PathVariable UUID groupId) {
    return search.facets(groupId);
  }
}
