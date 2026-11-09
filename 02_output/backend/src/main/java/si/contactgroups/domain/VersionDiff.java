package si.contactgroups.domain;

import java.util.ArrayList;
import java.util.List;

/** What differs between a Version and the current state of a Contact (R-022-04, D-19b). */
public final class VersionDiff {

  /** One difference; a value missing on one side is {@code null} (a Tag is its own value). */
  public record Difference(String kind, String name, String versionValue, String currentValue) {}

  private VersionDiff() {}

  /** The differences in the order fields, Tags, Attributes; empty when the states are equal. */
  public static List<Difference> between(VersionContent version, VersionContent current) {
    List<Difference> differences = new ArrayList<>();
    ContactChanges.between(version.data(), current.data())
        .forEach(
            (name, change) ->
                differences.add(
                    new Difference("field", name, change.oldValue(), change.newValue())));
    TagChanges tags = TagChanges.between(version.tags(), current.tags());
    tags.removed().forEach(name -> differences.add(new Difference("tag", name, name, null)));
    tags.added().forEach(name -> differences.add(new Difference("tag", name, null, name)));
    AttributeChanges.between(version.attributes(), current.attributes())
        .forEach(
            change ->
                differences.add(
                    new Difference(
                        "attribute", change.key(), change.oldValue(), change.newValue())));
    return differences;
  }
}
