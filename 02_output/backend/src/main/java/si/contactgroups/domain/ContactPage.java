package si.contactgroups.domain;

import java.util.List;

/** One page of the Contact list (contract schema {@code ContactPage}). */
public record ContactPage(List<ContactListItem> items, int page, int pageSize, int total) {}
