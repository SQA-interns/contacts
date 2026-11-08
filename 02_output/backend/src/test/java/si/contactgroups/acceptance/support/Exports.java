package si.contactgroups.acceptance.support;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Export requests ({@code POST /groups/{g}/exports}) and their file name. */
public final class Exports {

  /** Header of the English file (contract {@code xlsx-export.schema.json}). */
  public static final List<String> HEADER_EN =
      List.of(
          "First name",
          "Last name",
          "E-mail",
          "Phone",
          "Company",
          "Job title",
          "Street and house number",
          "Postal code",
          "City",
          "Country",
          "Notes",
          "Tags");

  /** Header of the Slovene file (contract {@code xlsx-export.schema.json}). */
  public static final List<String> HEADER_SL =
      List.of(
          "Ime",
          "Priimek",
          "E-pošta",
          "Telefon",
          "Podjetje",
          "Delovno mesto",
          "Ulica in hišna številka",
          "Poštna številka",
          "Kraj",
          "Država",
          "Opombe",
          "Značke");

  private static final Pattern PLAIN = Pattern.compile("filename=\"([^\"]+)\"");
  private static final Pattern EXTENDED = Pattern.compile("filename\\*=(?:UTF-8|utf-8)''([^;]+)");

  private Exports() {}

  public static Resp byIds(TestUser user, Grp group, String lang, List<String> contactIds) {
    return user.post(
        group.exports(), Json.obj("selection", Json.obj("contactIds", contactIds), "lang", lang));
  }

  public static Resp byFilter(TestUser user, Grp group, String lang, JsonNode filter) {
    return user.post(
        group.exports(), Json.obj("selection", Json.obj("filter", filter), "lang", lang));
  }

  /** The file name of {@code Content-Disposition} (plain or RFC 5987 form). */
  public static String fileName(Resp answer) {
    String disposition = answer.header("Content-Disposition");
    if (disposition == null) {
      return null;
    }
    Matcher extended = EXTENDED.matcher(disposition);
    if (extended.find()) {
      return URLDecoder.decode(extended.group(1).trim(), StandardCharsets.UTF_8);
    }
    Matcher plain = PLAIN.matcher(disposition);
    return plain.find() ? plain.group(1) : null;
  }
}
