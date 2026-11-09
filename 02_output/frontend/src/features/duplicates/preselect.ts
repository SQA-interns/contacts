import { CONTACT_FIELDS, type Contact, type ContactField, type ContactFields } from '@/api/contacts';

/** One field of the Merge screen: the value of each Contact (`null` when empty) and the default. */
export interface FieldRow {
  field: ContactField;
  values: (string | null)[];
  /** False when all Contacts have the same value, or none has one (EX-026-02). */
  needsChoice: boolean;
  preselected: string | null;
}

/** One Attribute key of the Merge screen, matched ignoring case (EX-026-08). */
export interface AttributeRow {
  key: string;
  values: (string | null)[];
  /** True when the Contacts have different values for the key (R-026-04). */
  needsChoice: boolean;
  /** `null` while the user has to choose. */
  preselected: string | null;
}

/** Contract schema `MergeRequest` (`tags` is not sent: the Survivor gets all Tags, R-026-03). */
export interface MergeRequestBody {
  survivorId: string;
  contacts: { id: string; version: number }[];
  fields: ContactFields;
  attributes: { key: string; value: string }[];
}

type Chosen = Record<string, string | undefined>;

const sameKey = (a: string) => a.toLowerCase();

/** The Survivor first: the oldest Contact, with equal times the smaller id (R-026-02, EX-026-03); then the others by age. */
export function orderForMerge(contacts: Contact[]): Contact[] {
  return [...contacts].sort(
    (a, b) => Date.parse(a.createdAt) - Date.parse(b.createdAt) || a.id.localeCompare(b.id),
  );
}

const textOf = (value: string | null | undefined): string | null => (value?.trim() ? value : null);

/** The 11 fields with the first non-empty value of the Survivor, then of the next oldest, preselected (R-026-02). */
export function fieldRows(ordered: Contact[]): FieldRow[] {
  return CONTACT_FIELDS.map((field) => {
    const values = ordered.map((contact) => textOf(contact[field]));
    return {
      field,
      values,
      needsChoice: new Set(values).size > 1,
      preselected: values.find((value) => value !== null) ?? null,
    };
  });
}

/** The Attribute keys of all Contacts in the order they first appear, with the value of each Contact (R-026-04). */
export function attributeRows(ordered: Contact[]): AttributeRow[] {
  const keys = new Map<string, string>();
  for (const contact of ordered) {
    for (const pair of contact.attributes) {
      if (!keys.has(sameKey(pair.key))) keys.set(sameKey(pair.key), pair.key);
    }
  }
  return [...keys].map(([lower, key]) => {
    const values = ordered.map(
      (contact) => contact.attributes.find((pair) => sameKey(pair.key) === lower)?.value ?? null,
    );
    const present = [...new Set(values.filter((value): value is string => value !== null))];
    return {
      key,
      values,
      needsChoice: present.length > 1,
      preselected: present.length === 1 ? (present[0] ?? null) : null,
    };
  });
}

/** The Tags of all Contacts without repeats, ignoring case, the Survivor's first (R-026-03). */
export function combinedTags(ordered: Contact[]): string[] {
  const tags = new Map<string, string>();
  for (const tag of ordered.flatMap((contact) => contact.tags)) {
    if (!tags.has(sameKey(tag))) tags.set(sameKey(tag), tag);
  }
  return [...tags.values()];
}

/** The keys whose values differ between the Contacts and that the user has not chosen yet (AC-026-03). */
export function missingChoices(rows: AttributeRow[], chosen: Chosen): string[] {
  return rows
    .filter((row) => row.needsChoice && chosen[sameKey(row.key)] === undefined)
    .map((row) => row.key);
}

/** The request of a confirmed merge; the version of each Contact is the one the screen loaded (R-026-08). */
export function mergeRequest(
  ordered: Contact[],
  fields: ContactFields,
  rows: AttributeRow[],
  chosen: Chosen,
): MergeRequestBody {
  const [survivor] = ordered;
  if (!survivor) throw new Error('A merge needs Contacts');
  return {
    survivorId: survivor.id,
    contacts: ordered.map((contact) => ({ id: contact.id, version: contact.version })),
    fields,
    attributes: rows.flatMap((row) => {
      const value = row.needsChoice ? chosen[sameKey(row.key)] : row.preselected;
      return value === undefined || value === null ? [] : [{ key: row.key, value }];
    }),
  };
}
