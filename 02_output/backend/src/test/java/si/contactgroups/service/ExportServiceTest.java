package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import si.contactgroups.domain.AttributePair;
import si.contactgroups.domain.ContactData;
import si.contactgroups.domain.ContactFilter;
import si.contactgroups.domain.ExportContact;
import si.contactgroups.persistence.ContactRepository;
import si.contactgroups.persistence.GroupRepository;

/** US-011: the selection, the limit, the file and the History entry. */
class ExportServiceTest {

  private final ContactRepository contacts = mock(ContactRepository.class);
  private final GroupRepository groups = mock(GroupRepository.class);
  private final HistoryWriter history = mock(HistoryWriter.class);
  private final Instant now = Instant.parse("2026-11-08T23:30:00Z");
  private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
  private final ExportService service = new ExportService(contacts, groups, history, clock);
  private final UUID group = UUID.randomUUID();
  private final UUID user = UUID.randomUUID();

  private static ExportContact contact(String last) {
    return new ExportContact(
        new ContactData(null, last, null, null, null, null, null, null, null, null, null),
        List.of(),
        List.<AttributePair>of());
  }

  private static List<UUID> ids(int n) {
    return IntStream.range(0, n).mapToObj(i -> UUID.randomUUID()).toList();
  }

  @Test
  void exportsTheSelectedContactsNamedAfterTheGroupAndTheDayInLjubljana() throws Exception {
    List<UUID> ids = ids(2);
    when(groups.name(group)).thenReturn("Acme");
    when(contacts.exportableIds(group, ids)).thenReturn(ids);
    when(contacts.forExport(group, ids)).thenReturn(List.of(contact("Ena"), contact("Dva")));

    ExportFile file = service.exportIds(group, user, ids, "en");

    assertThat(file.fileName()).isEqualTo("Acme_2026-11-09.xlsx");
    assertThat(file.count()).isEqualTo(2);
    try (var book = new XSSFWorkbook(new ByteArrayInputStream(file.bytes()))) {
      assertThat(book.getSheetAt(0).getLastRowNum()).isEqualTo(2);
    }
    verify(history).contactsExported(group, user, 2, now);
  }

  @Test
  void leavesOutSelectedContactsThatAreNoLongerThereAndCountsOnlyTheExportedRows() {
    List<UUID> ids = ids(3);
    List<UUID> alive = ids.subList(0, 2);
    when(groups.name(group)).thenReturn("Acme");
    when(contacts.exportableIds(group, ids)).thenReturn(alive);
    when(contacts.forExport(group, alive)).thenReturn(List.of(contact("A"), contact("B")));

    assertThat(service.exportIds(group, user, ids, "en").count()).isEqualTo(2);
    verify(history).contactsExported(group, user, 2, now);
  }

  @Test
  void refusesMoreThanTenThousandAndWritesNothing() {
    List<UUID> ids = ids(10_001);
    when(contacts.exportableIds(group, ids)).thenReturn(ids);

    assertThatThrownBy(() -> service.exportIds(group, user, ids, "en"))
        .isInstanceOf(ExportTooManyException.class);
    verify(contacts, never()).forExport(any(), any());
    verify(history, never())
        .contactsExported(any(), any(), org.mockito.ArgumentMatchers.anyInt(), any());
  }

  @Test
  void aFilterSelectionIsRefusedAboveTenThousandByCountingFirst() {
    when(contacts.count(group, ContactFilter.NONE)).thenReturn(10_001);

    assertThatThrownBy(() -> service.exportFilter(group, user, ContactFilter.NONE, "en"))
        .isInstanceOf(ExportTooManyException.class);
    verify(contacts, never()).idsMatching(any(), any());
  }

  @Test
  void aFilterSelectionExportsTheMatchingContacts() {
    List<UUID> ids = ids(1);
    when(groups.name(group)).thenReturn("Acme");
    when(contacts.count(group, ContactFilter.NONE)).thenReturn(1);
    when(contacts.idsMatching(group, ContactFilter.NONE)).thenReturn(ids);
    when(contacts.forExport(group, ids)).thenReturn(List.of(contact("Ena")));

    assertThat(service.exportFilter(group, user, ContactFilter.NONE, "sl").count()).isEqualTo(1);
    verify(history).contactsExported(group, user, 1, now);
  }
}
