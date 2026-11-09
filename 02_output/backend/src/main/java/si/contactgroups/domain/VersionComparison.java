package si.contactgroups.domain;

import java.util.List;
import si.contactgroups.domain.VersionDiff.Difference;

/** A Version compared with the live Contact (contract {@code VersionComparison}, R-022-04). */
public record VersionComparison(int number, List<Difference> differences) {}
