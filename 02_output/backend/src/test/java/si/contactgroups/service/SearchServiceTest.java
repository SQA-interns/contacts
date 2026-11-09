package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import si.contactgroups.domain.ContactFilter;
import si.contactgroups.domain.ContactPage;
import si.contactgroups.domain.Facets;
import si.contactgroups.persistence.ContactRepository;

class SearchServiceTest {

  private final ContactRepository contacts = mock(ContactRepository.class);
  private final SearchService service = new SearchService(contacts);
  private final UUID group = UUID.randomUUID();
  private static final ContactFilter FILTER = ContactFilter.NONE;

  private static ContactFilter search(String q) {
    return FilterParser.parse(q, null, null, null, null, null, null, null, null);
  }

  @Test
  void refusesAPageBelowOneOrNotANumber() {
    assertThatThrownBy(() -> service.list(group, "0", FILTER, null, null))
        .isInstanceOf(InvalidQueryException.class);
    assertThatThrownBy(() -> service.list(group, "abc", FILTER, null, null))
        .isInstanceOf(InvalidQueryException.class);
  }

  @Test
  void refusesAnUnknownColumnOrDirection() {
    assertThatThrownBy(() -> service.list(group, null, FILTER, "notes", null))
        .isInstanceOf(InvalidQueryException.class);
    assertThatThrownBy(() -> service.list(group, null, FILTER, "company", "up"))
        .isInstanceOf(InvalidQueryException.class);
  }

  @Test
  void aPageBeyondTheEndIsEmptyWithTheTotalAndReadsNoRows() {
    when(contacts.count(group, FILTER)).thenReturn(60);

    ContactPage page = service.list(group, "99", FILTER, null, null);

    assertThat(page.items()).isEmpty();
    assertThat(page.total()).isEqualTo(60);
    assertThat(page.page()).isEqualTo(99);
    verify(contacts).count(group, FILTER);
    verifyNoMoreInteractions(contacts);
  }

  @Test
  void passesTheWordsOfTheSearchTextAsPatternsToCountAndPage() {
    when(contacts.count(group, ContactFilter.searching(List.of("%ana%", "%nov%")))).thenReturn(1);

    ContactPage page = service.list(group, null, search(" ANA  nov"), null, null);

    assertThat(page.total()).isEqualTo(1);
    verify(contacts)
        .page(group, ContactFilter.searching(List.of("%ana%", "%nov%")), "lastName", false, 0, 25);
  }

  @Test
  void readsTheOffsetOfThePageInTheChosenOrder() {
    when(contacts.count(group, FILTER)).thenReturn(60);
    when(contacts.page(group, FILTER, "company", true, 50, 25)).thenReturn(List.of());

    ContactPage page = service.list(group, "3", FILTER, "company", "desc");

    assertThat(page.pageSize()).isEqualTo(25);
    verify(contacts).page(group, FILTER, "company", true, 50, 25);
  }

  @Test
  void passesTheFilterToCountAndPage() {
    ContactFilter filter =
        FilterParser.parse(null, List.of("a"), "any", null, "SI", null, null, null, null);
    when(contacts.count(group, filter)).thenReturn(1);

    service.list(group, null, filter, null, null);

    verify(contacts).page(group, filter, "lastName", false, 0, 25);
  }

  @Test
  void facetsComeFromTheGroupOnly() {
    Facets facets = new Facets(List.of("a"), List.of("Acme"), List.of("SI"), List.of("k"));
    when(contacts.facets(group)).thenReturn(facets);

    assertThat(service.facets(group)).isEqualTo(facets);
  }
}
