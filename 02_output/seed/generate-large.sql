-- 10,000 fictional Contacts in one Group for the performance checks (NFR-01..04). Run as the owner role after
-- seed.sql. Names are combinations of generic given names and surnames; e-mail addresses end in example.test.
BEGIN;
CREATE FUNCTION pg_temp.norm(t text) RETURNS text LANGUAGE sql IMMUTABLE AS
$$ SELECT lower(translate(coalesce(t, ''), 'čšžćđČŠŽĆĐ', 'cszcdcszcd')) $$;

INSERT INTO contact_group (id, name) VALUES ('00000000-0000-4000-8000-0000000000c3', 'Large Co') ON CONFLICT DO NOTHING;
INSERT INTO member (group_id, user_id, role) VALUES
  ('00000000-0000-4000-8000-0000000000c3', '00000000-0000-4000-8000-0000000000a1', 'OWNER'),
  ('00000000-0000-4000-8000-0000000000c3', '00000000-0000-4000-8000-0000000000a2', 'EDITOR')
ON CONFLICT DO NOTHING;

INSERT INTO tag (id, group_id, name, name_norm)
SELECT ('00000000-0000-4000-8000-0000000001' || lpad(t::text, 2, '0'))::uuid, '00000000-0000-4000-8000-0000000000c3', 'tag' || lpad(t::text, 2, '0'), 'tag' || lpad(t::text, 2, '0')
FROM generate_series(1, 10) t ON CONFLICT DO NOTHING;

WITH names AS (
  SELECT g,
         (ARRAY['Ana','Boris','Cene','Dana','Eva','Filip','Gaja','Hana','Ivan','Jana','Klemen','Lara','Miha','Nina','Oskar','Petra'])[1 + g % 16] AS fn,
         (ARRAY['Novak','Kovač','Škerlj','Horvat','Čosić','Zupan','Vidmar','Kos','Kralj','Mlakar','Potočnik','Oblak','Golob','Koren','Bizjak','Zajc','Krajnc','Hribar','Kotnik','Kavčič'])[1 + (g / 16) % 20] AS ln,
         (ARRAY['Acme d.o.o.','Beta d.o.o.','Gamma d.o.o.','Delta d.o.o.','Epsilon GmbH','Zeta d.o.o.','Eta s.p.','Theta d.o.o.'])[1 + g % 8] AS company
  FROM generate_series(1, 10000) g
)
INSERT INTO contact (id, group_id, first_name, last_name, email, phone, company, city, country,
                     first_name_norm, last_name_norm, company_norm, email_norm, search_norm, created_by)
SELECT ('00000000-0000-4000-8000-' || lpad(g::text, 12, '0'))::uuid, '00000000-0000-4000-8000-0000000000c3',
       fn, ln, 'person' || g || '@example.test', '+386 40 ' || lpad(g::text, 6, '0'), company,
       (ARRAY['Ljubljana','Maribor','Celje','Kranj','Koper'])[1 + g % 5], 'SI',
       pg_temp.norm(fn), pg_temp.norm(ln), pg_temp.norm(company), 'person' || g || '@example.test',
       pg_temp.norm(concat_ws(' ', fn, ln, 'person' || g || '@example.test', company, (ARRAY['Ljubljana','Maribor','Celje','Kranj','Koper'])[1 + g % 5])),
       '00000000-0000-4000-8000-0000000000a1'
FROM names ON CONFLICT DO NOTHING;

INSERT INTO contact_tag (contact_id, tag_id)
SELECT ('00000000-0000-4000-8000-' || lpad(g::text, 12, '0'))::uuid, ('00000000-0000-4000-8000-0000000001' || lpad((1 + g % 10)::text, 2, '0'))::uuid
FROM generate_series(1, 10000) g ON CONFLICT DO NOTHING;

INSERT INTO attribute (contact_id, key, value, value_norm)
SELECT ('00000000-0000-4000-8000-' || lpad(g::text, 12, '0'))::uuid, 'kompetenca', (ARRAY['Python','Java','SQL','React'])[1 + g % 4], lower((ARRAY['Python','Java','SQL','React'])[1 + g % 4])
FROM generate_series(1, 10000, 3) g ON CONFLICT DO NOTHING;
COMMIT;
