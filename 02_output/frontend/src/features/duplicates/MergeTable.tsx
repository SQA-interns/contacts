import { useId, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import type { Contact, ContactField } from '@/api/contacts';
import type { Language } from '@/i18n';
import { formatDate } from '@/i18n/format';
import { contactName } from './contactName';
import type { AttributeRow, FieldRow } from './preselect';

/** The values the user has chosen: a field's value (`null` is empty), and an Attribute's by lower-case key. */
export interface Choices {
  fields: Record<string, string | null>;
  attributes: Record<string, string | undefined>;
}

interface MergeTableProps {
  contacts: Contact[];
  fieldRows: FieldRow[];
  attributeRows: AttributeRow[];
  tags: string[][];
  choices: Choices;
  /** Keys of Attributes the user has to choose yet (AC-026-03). */
  missing: string[];
  onField: (field: ContactField, value: string | null) => void;
  onAttribute: (key: string, value: string) => void;
}

const CELL =
  'block px-1 py-2 before:block before:text-xs before:text-text-muted before:content-[attr(data-label)] md:table-cell md:border-b md:p-3 md:before:hidden';
const ROW =
  'mb-3 block rounded-[12px] border bg-surface-raised p-3 md:mb-0 md:table-row md:rounded-none md:border-0 md:bg-transparent md:p-0';

/** SC-18 / WF-18: the chosen Contacts side by side, field by field, with a choice of the value to keep (R-026-01). */
export function MergeTable({
  contacts,
  fieldRows,
  attributeRows,
  tags,
  choices,
  missing,
  onField,
  onAttribute,
}: MergeTableProps) {
  const { t, i18n } = useTranslation();
  const language = i18n.language as Language;
  const labels = contacts.map((contact, i) => ({
    heading: i === 0 ? t('merge.survivor') : t('merge.other', { n: i + 1 }),
    name: contactName(contact, t('contacts.untitled')),
    created: t('merge.createdOn', { date: formatDate(contact.createdAt, language) }),
  }));
  return (
    <div className="overflow-x-auto">
      <table role="table" className="block w-full border-collapse text-left md:table">
        <caption className="sr-only">{t('merge.caption')}</caption>
        <thead className="sr-only md:not-sr-only md:table-header-group">
          <tr role="row">
            <th role="columnheader" scope="col" className="border-b p-3 font-semibold">
              {t('merge.field')}
            </th>
            {labels.map((label) => (
              <th key={label.heading} role="columnheader" scope="col" className="border-b p-3 align-top">
                <span className="font-semibold">{label.heading}</span>
                <span className="block font-normal break-words">{label.name}</span>
                <span className="block text-xs font-normal text-text-muted">{label.created}</span>
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="block md:table-row-group">
          {fieldRows.map((row) => (
            <Row key={row.field} label={t(`contacts.fields.${row.field}`)} cells={labels}>
              {(i, describedBy) => (
                <ValueCell
                  name={`field-${row.field}`}
                  values={row.values}
                  index={i}
                  radios={row.needsChoice}
                  chosen={choices.fields[row.field] ?? null}
                  describedBy={describedBy}
                  onChoose={(value) => onField(row.field, value)}
                />
              )}
            </Row>
          ))}
          <Row label={t('merge.tags')} cells={labels}>
            {(i) => <TagCell tags={tags[i] ?? []} />}
          </Row>
          {attributeRows.map((row) => (
            <Row
              key={row.key.toLowerCase()}
              label={t('merge.attribute', { key: row.key })}
              cells={labels}
              error={missing.includes(row.key) ? t('merge.chooseValue') : undefined}
            >
              {(i, describedBy) => (
                <AttributeCell
                  row={row}
                  index={i}
                  chosen={choices.attributes[row.key.toLowerCase()]}
                  describedBy={describedBy}
                  onChoose={(value) => onAttribute(row.key, value)}
                />
              )}
            </Row>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Row({
  label,
  cells,
  error,
  children,
}: {
  label: string;
  cells: { heading: string; name: string }[];
  error?: string;
  children: (index: number, describedBy: string) => ReactNode;
}) {
  const id = useId();
  return (
    <tr role="row" className={ROW}>
      <th
        role="rowheader"
        scope="row"
        id={id}
        className="block px-1 py-2 font-semibold break-words md:table-cell md:border-b md:p-3"
      >
        {label}
        {error ? (
          <span role="alert" className="block text-sm font-semibold text-danger">
            {error}
          </span>
        ) : null}
      </th>
      {cells.map((cell, i) => (
        <td key={cell.heading} data-label={`${cell.heading}: ${cell.name}`} className={`${CELL} break-words`}>
          {children(i, id)}
        </td>
      ))}
    </tr>
  );
}

function Option({
  name,
  value,
  checked,
  describedBy,
  onChoose,
}: {
  name: string;
  value: string | null;
  checked: boolean;
  describedBy: string;
  onChoose: () => void;
}) {
  const { t } = useTranslation();
  return (
    <label className="flex min-h-11 items-center gap-2">
      <input
        type="radio"
        name={name}
        checked={checked}
        aria-describedby={describedBy}
        onChange={onChoose}
        className="size-5 shrink-0 accent-primary"
      />
      <span className={value === null ? 'text-sm text-text-muted' : 'break-words'}>
        {value ?? t('merge.empty')}
      </span>
    </label>
  );
}

/** One value of a field: a radio for the first Contact that has it; the same value again, or no real choice, is plain text. */
function ValueCell({
  name,
  values,
  index,
  radios,
  chosen,
  describedBy,
  onChoose,
}: {
  name: string;
  values: (string | null)[];
  index: number;
  radios: boolean;
  chosen: string | null;
  describedBy: string;
  onChoose: (value: string | null) => void;
}) {
  const { t } = useTranslation();
  const value = values[index] ?? null;
  if (!radios || values.indexOf(value) !== index) {
    return value === null ? (
      <span className="text-sm text-text-muted">{t('merge.empty')}</span>
    ) : (
      <span>{value}</span>
    );
  }
  return (
    <Option
      name={name}
      value={value}
      checked={chosen === value}
      describedBy={describedBy}
      onChoose={() => onChoose(value)}
    />
  );
}

function AttributeCell({
  row,
  index,
  chosen,
  describedBy,
  onChoose,
}: {
  row: AttributeRow;
  index: number;
  chosen: string | undefined;
  describedBy: string;
  onChoose: (value: string) => void;
}) {
  const { t } = useTranslation();
  const value = row.values[index] ?? null;
  if (row.needsChoice && value !== null && row.values.indexOf(value) === index) {
    return (
      <Option
        name={`attribute-${row.key.toLowerCase()}`}
        value={value}
        checked={chosen === value}
        describedBy={describedBy}
        onChoose={() => onChoose(value)}
      />
    );
  }
  if (value === null) return <span className="text-text-muted">{t('merge.none')}</span>;
  const onlyHere = row.values.filter((v) => v !== null).length === 1;
  return (
    <span>
      {value}
      {onlyHere ? <span className="block text-sm text-text-muted">{t('merge.onlyHere')}</span> : null}
    </span>
  );
}

function TagCell({ tags }: { tags: string[] }) {
  return (
    <ul className="flex flex-wrap gap-1">
      {tags.map((tag) => (
        <li key={tag} className="rounded-full border bg-surface px-2 text-sm break-all">
          {tag}
        </li>
      ))}
    </ul>
  );
}
