import type { ContactFields } from '@/api/contacts';

/** How a Contact is named in the duplicate screens: name, else company, else e-mail, else the fallback. */
export function contactName(contact: ContactFields, fallback: string): string {
  const name = [contact.firstName, contact.lastName].filter(Boolean).join(' ');
  return name || contact.company || contact.email || fallback;
}
