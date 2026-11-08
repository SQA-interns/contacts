package si.contactgroups.acceptance.support;

/** A Group of the product and the paths of its operations (contract {@code openapi.yaml}). */
public record Grp(String id, String name) {

  public String path() {
    return "/groups/" + id;
  }

  public String contacts() {
    return path() + "/contacts";
  }

  public String contacts(String query) {
    return contacts() + (query.isEmpty() ? "" : "?" + query);
  }

  public String contact(String contactId) {
    return contacts() + "/" + contactId;
  }

  public String history(String contactId) {
    return contact(contactId) + "/history";
  }

  public String versions(String contactId) {
    return contact(contactId) + "/versions";
  }

  public String version(String contactId, int number) {
    return versions(contactId) + "/" + number;
  }

  public String toTrash(String contactId) {
    return contact(contactId) + "/trash";
  }

  public String trash() {
    return path() + "/trash/contacts";
  }

  public String trashed(String contactId) {
    return trash() + "/" + contactId;
  }

  public String restore(String contactId) {
    return trashed(contactId) + "/restore";
  }

  public String tags() {
    return path() + "/tags";
  }

  public String tag(String tagId) {
    return tags() + "/" + tagId;
  }

  public String facets() {
    return path() + "/facets";
  }

  public String members() {
    return path() + "/members";
  }

  public String member(String userId) {
    return members() + "/" + userId;
  }

  public String invitations() {
    return path() + "/invitations";
  }

  public String cancelInvitation(String invitationId) {
    return invitations() + "/" + invitationId + "/cancel";
  }

  public String activity(String query) {
    return path() + "/activity" + (query.isEmpty() ? "" : "?" + query);
  }

  public String exports() {
    return path() + "/exports";
  }

  public String duplicates() {
    return path() + "/duplicates";
  }

  public String dismissals() {
    return duplicates() + "/dismissals";
  }

  public String merges() {
    return path() + "/merges";
  }

  public String toGroupTrash() {
    return path() + "/trash";
  }

  public String restoreGroup() {
    return "/group-trash/" + id + "/restore";
  }
}
