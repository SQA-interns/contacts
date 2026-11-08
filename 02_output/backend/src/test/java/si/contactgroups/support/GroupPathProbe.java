package si.contactgroups.support;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import si.contactgroups.domain.Action;
import si.contactgroups.security.RequiresAction;

/**
 * Test-only endpoints with a Group in their path, until the stories of the Group endpoints exist:
 * they let the tests of the authorisation check (ADR-05) run through the real filter chain. Not a
 * part of the application: only the profile {@value #PROFILE}, switched on by {@link
 * ApplicationContextSupport}, loads it.
 */
@Controller
@Profile(GroupPathProbe.PROFILE)
@RequestMapping("/api/v1/groups/{groupId}/probe")
public class GroupPathProbe {

  public static final String PROFILE = "group-path-probe";

  @GetMapping
  @ResponseBody
  @RequiresAction(Action.READ_CONTACTS)
  public String read(@PathVariable String groupId) {
    return "read " + groupId;
  }

  @PostMapping
  @ResponseBody
  @RequiresAction(Action.DELETE_GROUP)
  public String ownerOnly(@PathVariable String groupId) {
    return "owner " + groupId;
  }
}
