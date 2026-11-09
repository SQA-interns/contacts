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
import si.contactgroups.domain.ContactPage;
import si.contactgroups.persistence.ContactRepository;

class SearchServiceTest {

  private final ContactRepository contacts = mock(ContactRepository.class);
  private final SearchService service = new SearchService(contacts);
  private final UUID group = UUID.randomUUID();

  @Test
  void refusesAPageBelowOneOrNotANumber() {
    assertThatThrownBy(() -> service.list(group, "0", null, null, null))
        .isInstanceOf(InvalidQueryException.class);
    assertThatThrownBy(() -> service.list(group, "abc", null, null, null))
        .isInstanceOf(InvalidQueryException.class);
  }

  @Test
  void refusesAnUnknownColumnOrDirection() {
    assertThatThrownBy(() -> service.list(group, null, null, "notes", null))
        .isInstanceOf(InvalidQueryException.class);
    assertThatThrownBy(() -> service.list(group, null, null, "company", "up"))
        .isInstanceOf(InvalidQueryException.class);
  }

  @Test
  void aPageBeyondTheEndIsEmptyWithTheTotalAndReadsNoRows() {
    when(contacts.count(group, List.of())).thenReturn(60);

    ContactPage page = service.list(group, "99", null, null, null);

    assertThat(page.items()).isEmpty();
    assertThat(page.total()).isEqualTo(60);
    assertThat(page.page()).isEqualTo(99);
    verify(contacts).count(group, List.of());
    verifyNoMoreInteractions(contacts);
  }

  @Test
  void passesTheWordsOfTheSearchTextAsPatternsToCountAndPage() {
    when(contacts.count(group, List.of("%ana%", "%nov%"))).thenReturn(1);

    ContactPage page = service.list(group, null, " ANA  nov", null, null);

    assertThat(page.total()).isEqualTo(1);
    verify(contacts).page(group, List.of("%ana%", "%nov%"), "lastName", false, 0, 25);
  }

  @Test
  void readsTheOffsetOfThePageInTheChosenOrder() {
    when(contacts.count(group, List.of())).thenReturn(60);
    when(contacts.page(group, List.of(), "company", true, 50, 25)).thenReturn(List.of());

    ContactPage page = service.list(group, "3", null, "company", "desc");

    assertThat(page.pageSize()).isEqualTo(25);
    verify(contacts).page(group, List.of(), "company", true, 50, 25);
  }
}
