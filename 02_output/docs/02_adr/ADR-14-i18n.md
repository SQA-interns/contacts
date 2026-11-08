# ADR-14: Languages and formats

- Status: accepted (S3 /plan)
- Drivers: US-019, UI-18, UI-30, NFR-11, AR-08, D-19(e), D-20

## Decision

i18next with `src/i18n/locales/sl.json` (default) and `en.json`; keys are `area.element.purpose`; plurals use i18next plural suffixes. The chosen language is stored in `localStorage` (`language`), else Slovene (R-019-01/03). Dates and times use `Intl.DateTimeFormat` with time zone `Europe/Ljubljana`: Slovene `d. M. yyyy` and `HH:mm`, English `yyyy-MM-dd` and `HH:mm`, explicit formatters (not the locale default) so the project-wide facts hold; numbers use `Intl.NumberFormat` (`sl-SI`, `en-GB`). Country names come from `Intl.DisplayNames` for the active language; the backend validates codes against the Java ISO 3166-1 list. The export request carries `lang=sl|en` (Slovene when absent); header texts and country names of the file come from backend resource bundles `export_sl.properties`, `export_en.properties`. API errors carry codes (ADR-10), translated in the catalog. A test compares both catalogs key by key and checks that every backend error code has a key.

## Consequences

- No locale detection from the browser.
- Two small resource bundles on the backend must stay in step with the catalog; checked by the same parity test.
