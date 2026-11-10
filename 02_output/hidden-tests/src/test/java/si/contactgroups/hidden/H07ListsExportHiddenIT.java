package si.contactgroups.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Hidden cases of US-007, US-008, US-009, US-011 and BR-04 (list, search, filter, export). */
class H07ListsExportHiddenIT extends HiddenBase {

  U ana;
  G acme;

  @BeforeEach
  void setUp() {
    ana = user("Ana Novak", "ana@acme.com");
    acme = group(ana, "Acme");
  }

  void many(int n) {
    for (int i = 1; i <= n; i++) {
      contact(ana, acme, "firstName", "I", "lastName", String.format("P%03d", i));
    }
  }

  List<String> last(String q) {
    return texts(page(ana, acme, q).path("items"), "lastName");
  }

  @Test
  @DisplayName("R-007-01 boundary 50 Contacts make two full pages, 51 make a third with one")
  void r00701() {
    many(51);
    assertThat(last("page=2")).hasSize(25);
    assertThat(last("page=3")).containsExactly("P051");
    assertThat(page(ana, acme, "").path("total").asInt()).isEqualTo(51);
  }

  @Test
  @DisplayName("R-007-02 combination without last name come last; ties of last name ordered by first name ignoring case")
  void r00702() {
    contact(ana, acme, "firstName", "beta", "lastName", "Kos");
    contact(ana, acme, "company", "Brez imen");
    contact(ana, acme, "firstName", "Alfa", "lastName", "Kos");
    contact(ana, acme, "firstName", "Zeta", "lastName", "Arh");
    assertThat(texts(page(ana, acme, "").path("items"), "firstName")).containsExactly("Zeta", "Alfa", "beta", null);
  }

  @Test
  @DisplayName("R-007-03 other sorting by city descending ignores accents: Šentjur, Sežana, Celje")
  void r00703() {
    for (String c : new String[] {"Sežana", "Celje", "Šentjur"}) {
      contact(ana, acme, "firstName", c, "city", c);
    }
    assertThat(texts(page(ana, acme, "sort=city&dir=desc").path("items"), "city")).containsExactly("Šentjur", "Sežana", "Celje");
  }

  @Test
  @DisplayName("R-007-04 other page 2 continues the descending last-name order")
  void r00704() {
    many(30);
    assertThat(last("sort=lastName&dir=desc&page=2")).containsExactly("P005", "P004", "P003", "P002", "P001");
  }

  @Test
  @DisplayName("R-007-07 combination neither a Contact in Trash nor one of Beta is listed")
  void r00707() {
    G beta = group(ana, "Beta");
    contact(ana, beta, "lastName", "Betin");
    String t = contact(ana, acme, "lastName", "Trashed");
    contact(ana, acme, "lastName", "Ostane");
    trash(ana, acme, t);
    assertThat(last("")).containsExactly("Ostane");
  }

  @Test
  @DisplayName("R-007-08 boundary the first page after the last is empty with the total")
  void r00708() {
    many(25);
    var p = page(ana, acme, "page=2");
    assertThat(p.path("items")).isEmpty();
    assertThat(p.path("total").asInt()).isEqualTo(25);
  }

  @Test
  @DisplayName("R-008-01 other e-mail, company, city and Tag are searched")
  void r00801() {
    String a = contact(ana, acme, "email", "iskana@posta.si");
    String b = contact(ana, acme, "company", "Iskalnik d.o.o.");
    String c = contact(ana, acme, "firstName", "C", "city", "Ivančna Gorica");
    String d = contact(ana, acme, "firstName", "D", "tags", arr("ivancni"));
    assertThat(ids(ana, acme, "q=posta")).containsExactly(a);
    assertThat(ids(ana, acme, "q=iskalnik")).containsExactly(b);
    assertThat(ids(ana, acme, "q=" + enc("ivancna gorica"))).containsExactly(c);
    assertThat(ids(ana, acme, "q=ivancni")).containsExactly(d);
  }

  @Test
  @DisplayName("R-008-02 other street, postal code, country and notes are not searched")
  void r00802() {
    contact(ana, acme, "firstName", "M", "street", "Kardeljeva 9", "postalCode", "4321", "country", "HR", "notes", "skrito");
    for (String q : new String[] {"Kardeljeva", "4321", "HR", "skrito"}) {
      assertThat(total(ana, acme, "q=" + q)).as(q).isZero();
    }
  }

  @Test
  @DisplayName("R-008-03 other a search shows page 1 with the number of matches even when page 2 is asked first")
  void r00803() {
    many(30);
    var p = page(ana, acme, "q=P02");
    assertThat(p.path("total").asInt()).isEqualTo(10);
    assertThat(p.path("page").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("R-008-05 other a search text of a tab and spaces is no search")
  void r00805() {
    many(3);
    assertThat(total(ana, acme, "q=" + enc("\t  "))).isEqualTo(3);
  }

  @Test
  @DisplayName("R-008-06 invalid '_' matches only a real underscore")
  void r00806() {
    contact(ana, acme, "firstName", "a_b");
    contact(ana, acme, "firstName", "axb");
    assertThat(texts(page(ana, acme, "q=a_b").path("items"), "firstName")).containsExactly("a_b");
  }

  @Test
  @DisplayName("R-008-07 combination a match in Trash and one in Beta are not found")
  void r00807() {
    G beta = group(ana, "Beta");
    contact(ana, beta, "lastName", "Iskan");
    trash(ana, acme, contact(ana, acme, "lastName", "Iskan"));
    assertThat(total(ana, acme, "q=iskan")).isZero();
  }

  void tagged() {
    contact(ana, acme, "firstName", "A", "tags", arr("x", "y", "z"), "country", "AT", "company", "Čevlji d.o.o.");
    contact(ana, acme, "firstName", "B", "tags", arr("x", "y"), "country", "SI", "company", "cevlji D.O.O.");
    contact(ana, acme, "firstName", "C", "tags", arr("z"), "country", "AT", "attributes", attrs("Kompetenca", "SQL"));
    contact(ana, acme, "firstName", "D", "attributes", attrs("kompetenca", "java"));
  }

  List<String> first(String q) {
    return texts(page(ana, acme, q).path("items"), "firstName");
  }

  @Test
  @DisplayName("R-009-01 combination 'has all' of three Tags gives only A")
  void r00901() {
    tagged();
    assertThat(first("tags=x&tags=y&tags=z&tagMode=all")).containsExactly("A");
  }

  @Test
  @DisplayName("R-009-02 other 'has any' of y and z gives A, B and C")
  void r00902() {
    tagged();
    assertThat(first("tags=y&tags=z&tagMode=any")).containsExactlyInAnyOrder("A", "B", "C");
  }

  @Test
  @DisplayName("R-009-03 combination Without-Tags x and z gives only D")
  void r00903() {
    tagged();
    assertThat(first("withoutTags=x&withoutTags=z")).containsExactly("D");
  }

  @Test
  @DisplayName("R-009-04 other the country filter AT gives A and C")
  void r00904() {
    tagged();
    assertThat(first("country=AT")).containsExactlyInAnyOrder("A", "C");
  }

  @Test
  @DisplayName("R-009-05 other the company filter 'CEVLJI d.o.o.' matches both spellings")
  void r00905() {
    tagged();
    assertThat(first("company=" + enc("CEVLJI d.o.o."))).containsExactlyInAnyOrder("A", "B");
  }

  @Test
  @DisplayName("R-009-06 other the key KOMPETENCA with value sql matches C")
  void r00906() {
    tagged();
    assertThat(first("attrKey=KOMPETENCA&attrValue=sql")).containsExactly("C");
  }

  @Test
  @DisplayName("R-009-07 other the key kompetenca exists for C and D")
  void r00907() {
    tagged();
    assertThat(first("attrKey=kompetenca&attrExists=true")).containsExactlyInAnyOrder("C", "D");
  }

  @Test
  @DisplayName("R-009-08 combination country AT, Tag z and search A give only A")
  void r00908() {
    tagged();
    assertThat(first("country=AT&tags=z&tagMode=all&q=a")).containsExactly("A");
  }

  @Test
  @DisplayName("R-009-09 other a filtered request without page gives page 1 and the count")
  void r00909() {
    tagged();
    var p = page(ana, acme, "country=AT");
    assertThat(p.path("page").asInt()).isEqualTo(1);
    assertThat(p.path("total").asInt()).isEqualTo(2);
  }

  @Test
  @DisplayName("R-009-10 other without any filter the full list is back")
  void r00910() {
    tagged();
    assertThat(total(ana, acme, "country=SI")).isEqualTo(1);
    assertThat(total(ana, acme, "")).isEqualTo(4);
  }

  @Test
  @DisplayName("R-009-11 other companies and countries offered are only those of Acme")
  void r00911() {
    tagged();
    G beta = group(ana, "Beta");
    contact(ana, beta, "firstName", "X", "country", "HR", "company", "Tuje");
    var f = ana.get(acme.p() + "/facets").is(200).json();
    assertThat(texts(f.path("countries"), null)).containsExactlyInAnyOrder("AT", "SI");
    assertThat(texts(f.path("companies"), null)).doesNotContain("Tuje");
  }

  @Test
  @DisplayName("R-009-12 other a combination that matches nothing gives an empty page")
  void r00912() {
    tagged();
    assertThat(total(ana, acme, "country=SI&tags=z&tagMode=all")).isZero();
  }

  @Test
  @DisplayName("R-011-02 boundary a Selection of one Contact gives a file with one row")
  void r01102() {
    String c = contact(ana, acme, "firstName", "Sama");
    var f = xlsx(export(ana, acme, "sl", obj("contactIds", arr(c))).is(200).bytes());
    assertThat(f.sheets()).isEqualTo(1);
    assertThat(f.rows()).hasSize(1);
  }

  @Test
  @DisplayName("R-011-03 combination Attribute columns zeta, Alfa are ordered Alfa, zeta; Tags sorted ignoring case")
  void r01103() {
    String c = contact(ana, acme, "firstName", "M", "tags", arr("beta", "Alfa"), "attributes", attrs("zeta", "1", "Alfa", "2"));
    var f = xlsx(export(ana, acme, "en", obj("contactIds", arr(c))).is(200).bytes());
    assertThat(f.header().subList(12, f.header().size())).containsExactly("Alfa", "zeta");
    assertThat(f.col("Tags")).containsExactly("Alfa, beta");
  }

  @Test
  @DisplayName("R-011-04 other the Group name A*B?C gives A_B_C_<date>.xlsx")
  void r01104() {
    clock.setNow(Instant.parse("2026-03-01T12:00:00Z"));
    G odd = group(ana, "A*B?C");
    String c = contact(ana, odd, "firstName", "M");
    String d = export(ana, odd, "en", obj("contactIds", arr(c))).is(200).headers().firstValue("Content-Disposition").orElse("");
    assertThat(d).contains("A_B_C_2026-03-01.xlsx");
  }

  @Test
  @DisplayName("R-011-05 boundary a filter Selection of 10,500 Contacts is refused and writes no entry")
  void r01105() {
    bulk(acme.id(), ana.userId(), 10_500, "Masa");
    export(ana, acme, "en", obj("filter", obj())).is(422, "export.too_many");
    assertThat(activity(ana, acme, "action=CONTACTS_EXPORTED")).isEmpty();
  }

  @Test
  @DisplayName("R-011-06 other the notes @SUM(A1) and the first name +1 stay text")
  void r01106() {
    String c = contact(ana, acme, "firstName", "+1", "notes", "@SUM(A1)");
    var f = xlsx(export(ana, acme, "en", obj("contactIds", arr(c))).is(200).bytes());
    assertThat(f.nonText()).isZero();
    assertThat(f.col("Notes")).containsExactly("@SUM(A1)");
    assertThat(f.col("First name")).containsExactly("+1");
  }

  @Test
  @DisplayName("R-011-07 other an export of 2 Contacts records the number 2")
  void r01107() {
    String a = contact(ana, acme, "firstName", "A");
    String b = contact(ana, acme, "firstName", "B");
    export(ana, acme, "en", obj("contactIds", arr(a, b))).is(200);
    var e = activity(ana, acme, "action=CONTACTS_EXPORTED");
    assertThat(e).hasSize(1);
    assertThat(e.get(0).path("details").toString()).contains("2");
  }

  @Test
  @DisplayName("R-011-08 role a Viewer's export by filter creates no file and no entry")
  void r01108() {
    contact(ana, acme, "firstName", "A");
    U viola = member(acme, ana, "Viola Vidmar", "viola@acme.com", "VIEWER");
    export(viola, acme, "en", obj("filter", obj())).is(403);
    assertThat(activity(ana, acme, "action=CONTACTS_EXPORTED")).isEmpty();
  }

  @Test
  @DisplayName("R-011-09 other the country AT reads Avstrija in Slovene and Austria in English")
  void r01109() {
    String c = contact(ana, acme, "firstName", "M", "country", "AT");
    assertThat(xlsx(export(ana, acme, "sl", obj("contactIds", arr(c))).is(200).bytes()).col("Država")).containsExactly("Avstrija");
    assertThat(xlsx(export(ana, acme, "en", obj("contactIds", arr(c))).is(200).bytes()).col("Country")).containsExactly("Austria");
  }

  @Test
  @DisplayName("R-011-10 other a Tag renamed after the Selection appears with its new name")
  void r01110() {
    String c = contact(ana, acme, "firstName", "M", "tags", arr("star"));
    ana.patch(acme.p() + "/tags/" + tagId(ana, acme, "star"), obj("name", "nov")).is(200);
    assertThat(xlsx(export(ana, acme, "en", obj("contactIds", arr(c))).is(200).bytes()).col("Tags")).containsExactly("nov");
  }

  @Test
  @DisplayName("BR-04 combination a Contact in Trash is left out of a filter export and of the duplicate search")
  void br04() {
    contact(ana, acme, "firstName", "A", "email", "x@acme.com");
    trash(ana, acme, contact(ana, acme, "firstName", "B", "email", "x@acme.com"));
    var f = xlsx(export(ana, acme, "en", obj("filter", obj())).is(200).bytes());
    assertThat(f.col("First name")).containsExactly("A");
    assertThat(list(ana.get(acme.p() + "/duplicates").is(200).json())).isEmpty();
  }
}
