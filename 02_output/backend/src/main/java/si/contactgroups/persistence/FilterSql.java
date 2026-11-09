package si.contactgroups.persistence;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import si.contactgroups.domain.ContactFilter;

/** The SQL conditions of the filters of US-009 and their bound values; nothing is concatenated. */
final class FilterSql {

  private static final String TAG_OF_CONTACT =
      "EXISTS (SELECT 1 FROM contact_tag ct JOIN tag t ON t.id = ct.tag_id"
          + " WHERE ct.contact_id = c.id AND t.name_norm ";

  private FilterSql() {}

  /** The conditions, each starting with {@code AND}; empty when no filter is active. */
  static String conditions(ContactFilter f) {
    return tagConditions(f) + valueConditions(f) + attributeCondition(f);
  }

  private static String tagConditions(ContactFilter f) {
    StringBuilder sql = new StringBuilder();
    if (f.anyTag() && !f.tags().isEmpty()) {
      sql.append(" AND ").append(TAG_OF_CONTACT).append("IN (:tags))");
    } else if (!f.anyTag()) {
      for (int i = 0; i < f.tags().size(); i++) {
        sql.append(" AND ").append(TAG_OF_CONTACT).append("= :tag").append(i).append(")");
      }
    }
    if (!f.withoutTags().isEmpty()) {
      sql.append(" AND NOT ").append(TAG_OF_CONTACT).append("IN (:withoutTags))");
    }
    return sql.toString();
  }

  private static String valueConditions(ContactFilter f) {
    return (f.country() == null ? "" : " AND c.country = :country")
        + (f.company() == null ? "" : " AND c.company_norm = :company");
  }

  private static String attributeCondition(ContactFilter f) {
    if (f.attrKey() == null) {
      return "";
    }
    return " AND EXISTS (SELECT 1 FROM attribute a WHERE a.contact_id = c.id"
        + " AND lower(a.key) = :attrKey"
        + (f.attrValue() == null ? ")" : " AND a.value_norm = :attrValue)");
  }

  /** Binds the search words and every value of the active filters. */
  static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec sql, ContactFilter f) {
    JdbcClient.StatementSpec spec = sql;
    for (int i = 0; i < f.words().size(); i++) {
      spec = spec.param("word" + i, f.words().get(i));
    }
    return bindValues(bindTags(spec, f), f);
  }

  private static JdbcClient.StatementSpec bindTags(JdbcClient.StatementSpec sql, ContactFilter f) {
    JdbcClient.StatementSpec spec = sql;
    if (f.anyTag()) {
      spec = f.tags().isEmpty() ? spec : spec.param("tags", f.tags());
    } else {
      List<String> tags = f.tags();
      for (int i = 0; i < tags.size(); i++) {
        spec = spec.param("tag" + i, tags.get(i));
      }
    }
    return f.withoutTags().isEmpty() ? spec : spec.param("withoutTags", f.withoutTags());
  }

  private static JdbcClient.StatementSpec bindValues(
      JdbcClient.StatementSpec sql, ContactFilter f) {
    JdbcClient.StatementSpec spec = sql;
    if (f.country() != null) {
      spec = spec.param("country", f.country());
    }
    if (f.company() != null) {
      spec = spec.param("company", f.company());
    }
    if (f.attrKey() != null) {
      spec = spec.param("attrKey", f.attrKey());
    }
    return f.attrValue() == null ? spec : spec.param("attrValue", f.attrValue());
  }
}
