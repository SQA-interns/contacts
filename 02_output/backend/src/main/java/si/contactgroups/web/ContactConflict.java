package si.contactgroups.web;

import si.contactgroups.domain.Contact;

/** The 409 answer of a stale save: the error format with the current Contact (R-003-07). */
public record ContactConflict(String code, String message, Contact current) {}
