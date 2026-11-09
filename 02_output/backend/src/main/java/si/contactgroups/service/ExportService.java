package si.contactgroups.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.contactgroups.domain.ContactFilter;
import si.contactgroups.domain.ExportColumns;
import si.contactgroups.domain.ExportContact;
import si.contactgroups.persistence.ContactRepository;
import si.contactgroups.persistence.GroupRepository;

/** The Excel export of a Selection (US-011, BR-03). */
@Service
public class ExportService {

  /** The most Contacts one Export file holds (R-011-05). */
  public static final int LIMIT = 10_000;

  private static final ZoneId ZONE = ZoneId.of("Europe/Ljubljana");

  private final ContactRepository contacts;
  private final GroupRepository groups;
  private final HistoryWriter history;
  private final Clock clock;

  public ExportService(
      ContactRepository contacts, GroupRepository groups, HistoryWriter history, Clock clock) {
    this.contacts = contacts;
    this.groups = groups;
    this.history = history;
    this.clock = clock;
  }

  /**
   * Exports the selected Contacts that are still there (R-011-02, R-011-10).
   *
   * @throws ExportTooManyException when more than {@link #LIMIT} of them remain (R-011-05)
   */
  @Transactional
  public ExportFile exportIds(UUID groupId, UUID userId, List<UUID> contactIds, String lang) {
    return export(groupId, userId, contacts.exportableIds(groupId, contactIds), lang);
  }

  /**
   * Exports the Contacts that match the filter, the Selection "all results" (R-010-05).
   *
   * @throws ExportTooManyException when more than {@link #LIMIT} match (R-011-05)
   */
  @Transactional
  public ExportFile exportFilter(UUID groupId, UUID userId, ContactFilter filter, String lang) {
    if (contacts.count(groupId, filter) > LIMIT) {
      throw new ExportTooManyException(LIMIT);
    }
    return export(groupId, userId, contacts.idsMatching(groupId, filter), lang);
  }

  private ExportFile export(UUID groupId, UUID userId, List<UUID> ids, String lang) {
    if (ids.size() > LIMIT) {
      throw new ExportTooManyException(LIMIT);
    }
    List<ExportContact> rows = contacts.forExport(groupId, ids);
    List<String> keys = ExportColumns.attributeKeys(rows);
    List<String> header = new java.util.ArrayList<>(ExportColumns.header(lang));
    header.addAll(keys);
    byte[] bytes =
        XlsxWriter.write(header, rows.stream().map(c -> ExportColumns.row(c, keys, lang)).toList());
    Instant now = clock.instant();
    history.contactsExported(groupId, userId, rows.size(), now);
    String name = ExportColumns.fileName(groups.name(groupId), LocalDate.ofInstant(now, ZONE));
    return new ExportFile(name, bytes, rows.size());
  }
}
