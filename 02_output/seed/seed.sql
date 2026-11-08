-- Local seed: fake users, Groups and Contacts. No real personal data: names are generic, e-mail addresses end in
-- example.com or example.test, phone numbers are invented. Safe to run repeatedly. Run as the owner role.
BEGIN;

-- same folding as the backend Normaliser (ADR-06) for the characters used below
CREATE FUNCTION pg_temp.norm(t text) RETURNS text LANGUAGE sql IMMUTABLE AS
$$ SELECT lower(translate(coalesce(t, ''), 'čšžćđČŠŽĆĐ', 'cszcdcszcd')) $$;

INSERT INTO app_user (id, firebase_uid, email, display_name) VALUES
  ('00000000-0000-4000-8000-0000000000a1', 'seed-ana',   'ana@example.com',   'Ana Seed'),
  ('00000000-0000-4000-8000-0000000000a2', 'seed-boris', 'boris@example.com', 'Boris Seed'),
  ('00000000-0000-4000-8000-0000000000a3', 'seed-cene',  'cene@example.com',  'Cene Seed'),
  ('00000000-0000-4000-8000-0000000000a4', 'seed-unverified', 'neza@example.com', 'Neža Seed')
ON CONFLICT DO NOTHING;

INSERT INTO contact_group (id, name) VALUES
  ('00000000-0000-4000-8000-0000000000c1', 'Acme'),
  ('00000000-0000-4000-8000-0000000000c2', 'Beta')
ON CONFLICT DO NOTHING;

INSERT INTO member (group_id, user_id, role) VALUES
  ('00000000-0000-4000-8000-0000000000c1', '00000000-0000-4000-8000-0000000000a1', 'OWNER'),
  ('00000000-0000-4000-8000-0000000000c1', '00000000-0000-4000-8000-0000000000a2', 'EDITOR'),
  ('00000000-0000-4000-8000-0000000000c1', '00000000-0000-4000-8000-0000000000a3', 'VIEWER'),
  ('00000000-0000-4000-8000-0000000000c2', '00000000-0000-4000-8000-0000000000a2', 'OWNER')
ON CONFLICT DO NOTHING;

INSERT INTO invitation (group_id, email, role, status, created_at, expires_at, created_by) VALUES
  ('00000000-0000-4000-8000-0000000000c1', 'neza@example.com', 'EDITOR', 'PENDING', now(), now() + interval '14 days', '00000000-0000-4000-8000-0000000000a1')
ON CONFLICT DO NOTHING;

-- Contacts of Acme (n = suffix of the id). Two share an e-mail address (duplicate search), one has an accent in the name.
WITH rows(n, first_name, last_name, email, phone, company, job_title, city, country, notes, trash_days) AS (VALUES
  (1,  'Ana',    'Novak',   'ana.novak@example.com',   '+386 40 000 001', 'Acme d.o.o.', 'Direktorica', 'Ljubljana', 'SI', 'Fictional contact.', NULL::int),
  (2,  'Boris',  'Kovač',   'boris.kovac@example.com', '+386 40 000 002', 'Acme d.o.o.', 'Vodja',       'Maribor',   'SI', NULL, NULL),
  (3,  'Cene',   'Škerlj',  'cene.skerlj@example.com', '+386 40 000 003', 'Beta d.o.o.', NULL,          'Celje',     'SI', NULL, NULL),
  (4,  'Dana',   'Horvat',  'dana.horvat@example.test','+386 40 000 004', 'Beta d.o.o.', 'Analitičarka','Koper',     'SI', NULL, NULL),
  (5,  'Eva',    'Čosić',   'eva.cosic@example.test',  '+385 91 000 005', 'Gamma d.o.o.','Svetovalka',  'Zagreb',    'HR', NULL, NULL),
  (6,  'Filip',  'Zupan',   'filip.zupan@example.test',NULL,              'Gamma d.o.o.', NULL,         'Kranj',     'SI', NULL, NULL),
  (7,  'Gaja',   'Vidmar',  'gaja.vidmar@example.test','+386 40 000 007', NULL,           NULL,         'Ljubljana', 'SI', 'Prefers e-mail.', NULL),
  (8,  'Hana',   'Kos',     'duplicate@example.test',  '+386 40 000 008', 'Delta d.o.o.', NULL,         'Ptuj',      'SI', NULL, NULL),
  (9,  'Hana',   'Kos',     'duplicate@example.test',  '+386 40 000 009', 'Delta d.o.o.', NULL,         'Ptuj',      'SI', 'Second record of the same person.', NULL),
  (10, 'Ivan',   'Kralj',   'ivan.kralj@example.test', '+43 660 000010',  'Epsilon GmbH', 'Manager',    'Wien',      'AT', NULL, NULL),
  (11, 'Jana',   'Mlakar',  'jana.mlakar@example.test',NULL,              'Acme d.o.o.', NULL,          'Ljubljana', 'SI', NULL, NULL),
  (12, 'Klemen', 'Potočnik','klemen.potocnik@example.test','+386 40 000 012','Beta d.o.o.',NULL,         'Novo mesto','SI', NULL, NULL),
  (13, 'Lara',   'Oblak',   'lara.oblak@example.test', NULL,              NULL,           NULL,         'Bled',      'SI', 'In Trash, 3 days.', 3),
  (14, 'Miha',   'Golob',   'miha.golob@example.test', NULL,              NULL,           NULL,         'Kamnik',    'SI', 'In Trash, 29 days.', 29)
)
INSERT INTO contact (id, group_id, first_name, last_name, email, phone, company, job_title, city, country, notes,
                     first_name_norm, last_name_norm, company_norm, email_norm, search_norm, created_by, deleted_at)
SELECT ('00000000-0000-4000-8000-00000000e' || lpad(n::text, 3, '0'))::uuid,
       '00000000-0000-4000-8000-0000000000c1', first_name, last_name, lower(email), phone, company, job_title, city, country, notes,
       pg_temp.norm(first_name), pg_temp.norm(last_name), pg_temp.norm(company), lower(email),
       pg_temp.norm(concat_ws(' ', first_name, last_name, email, company, city)),
       '00000000-0000-4000-8000-0000000000a1',
       CASE WHEN trash_days IS NULL THEN NULL ELSE now() - make_interval(days => trash_days) END
FROM rows
ON CONFLICT DO NOTHING;

INSERT INTO tag (id, group_id, name, name_norm) VALUES
  ('00000000-0000-4000-8000-0000000000d1', '00000000-0000-4000-8000-0000000000c1', 'neObvescaj', 'neobvescaj'),
  ('00000000-0000-4000-8000-0000000000d2', '00000000-0000-4000-8000-0000000000c1', 'udelezenec2025', 'udelezenec2025'),
  ('00000000-0000-4000-8000-0000000000d3', '00000000-0000-4000-8000-0000000000c1', 'udelezenec205', 'udelezenec205'),
  ('00000000-0000-4000-8000-0000000000d4', '00000000-0000-4000-8000-0000000000c1', 'stranka', 'stranka')
ON CONFLICT DO NOTHING;

INSERT INTO contact_tag (contact_id, tag_id)
SELECT ('00000000-0000-4000-8000-00000000e' || lpad(n::text, 3, '0'))::uuid, t::uuid
FROM (VALUES (2, '00000000-0000-4000-8000-0000000000d1'), (3, '00000000-0000-4000-8000-0000000000d1'),
             (1, '00000000-0000-4000-8000-0000000000d2'), (4, '00000000-0000-4000-8000-0000000000d2'),
             (5, '00000000-0000-4000-8000-0000000000d3'), (6, '00000000-0000-4000-8000-0000000000d3'),
             (1, '00000000-0000-4000-8000-0000000000d4'), (2, '00000000-0000-4000-8000-0000000000d4')) AS v(n, t)
ON CONFLICT DO NOTHING;

INSERT INTO attribute (contact_id, key, value, value_norm)
SELECT ('00000000-0000-4000-8000-00000000e' || lpad(n::text, 3, '0'))::uuid, k, v, pg_temp.norm(v)
FROM (VALUES (1, 'kompetenca', 'Python'), (2, 'kompetenca', 'Java'), (3, 'ime žene', 'Mojca'), (5, 'jezik', 'hrvaščina'),
             (10, 'jezik', 'nemščina')) AS a(n, k, v)
ON CONFLICT DO NOTHING;

INSERT INTO contact_version (contact_id, number, user_id, snapshot)
SELECT id, 1, created_by, jsonb_build_object('firstName', first_name, 'lastName', last_name, 'email', email, 'company', company, 'tags', '[]'::jsonb, 'attributes', '[]'::jsonb)
FROM contact WHERE group_id = '00000000-0000-4000-8000-0000000000c1'
ON CONFLICT DO NOTHING;

INSERT INTO history_entry (group_id, user_id, user_name, user_email, action, contact_id, details)
SELECT group_id, created_by, 'Ana Seed', 'ana@example.com', 'CONTACT_CREATED', id,
       jsonb_build_object('lastName', jsonb_build_object('new', last_name))
FROM contact c WHERE group_id = '00000000-0000-4000-8000-0000000000c1'
  AND NOT EXISTS (SELECT 1 FROM history_entry h WHERE h.contact_id = c.id);

COMMIT;
