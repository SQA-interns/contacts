# Local seed

Fake data for the local environment; no real personal data. `scripts/seed-local.sh` creates, in the Firebase Auth emulator only, the accounts below (password `Seed-Passw0rd!`, a test value that exists only in the emulator), then loads `seed.sql` (Groups `Acme` and `Beta`, 14 Contacts, Tags, Attributes, History, one pending Invitation) and with `--large` also `generate-large.sql` (Group `Large Co`, 10,000 Contacts). The script is safe to run again.

| Account | Role |
|---|---|
| ana@example.com | Owner of Acme and Large Co |
| boris@example.com | Editor of Acme and Large Co, Owner of Beta |
| cene@example.com | Viewer of Acme |
| neza@example.com | e-mail not verified; has a pending Invitation to Acme |

`seed-auth.mjs` refuses any address that is not a local emulator.
