package si.contactgroups.web;

import java.util.List;
import si.contactgroups.domain.SoleOwnerGroup;

/** The 409 answer of an account deletion: the error format with the Groups to deal with first. */
public record SoleOwnerConflict(String code, String message, List<SoleOwnerGroup> groups) {}
