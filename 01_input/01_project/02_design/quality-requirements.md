# Quality requirements

> For: agent (reads), human (reviews) · Owner: QA lead · Agent: read-only

Adds to `general/quality/` (definition of done, test strategy, severity scale); do not repeat those here. Project done criteria (`DoD-P`) are in `project/01_requirements/intent.md`. The numbers below are the developer's proposal from Q-50 and Q-35; the customer gave none (OQ-P04).

## Non-functional requirements

| ID | Category (performance, availability, accessibility, localisation, usability, …) | Requirement | How it is checked |
|---|---|---|---|
| NFR-01 | Performance | In a Group of up to 10,000 Contacts, the contact list, search and filter show results in under 1 second (Q-35, Q-50). | Runtime demonstration on the deployed system with 10,000 generated Contacts; median and 95th percentile of 50 requests per operation are recorded. |
| NFR-02 | Performance | Opening a Contact takes under 500 ms (Q-50). | Same demonstration, 50 opens. |
| NFR-03 | Performance | Exporting 1,000 Contacts takes under 10 seconds (Q-50). | Same demonstration, 5 exports. |
| NFR-04 | Capacity | The system supports up to 100 registered Users, 20 of them at the same time, and Groups of up to 10,000 Contacts (Q-50). | Load script with 20 concurrent Users doing list, search, edit and export for 5 minutes on the deployed system; no error and NFR-01 holds. |
| NFR-05 | Availability | Availability of 99 % per month, no on-call duty, planned maintenance announced (Q-50). | Documented in the install guide (health check, restart policy); not measurable before release, so checked by review of the Compose setup. |
| NFR-06 | Recoverability | The database is backed up daily, backups are kept 14 days, and a restore has been tested once before release (Q-50). | Backup and restore commands in the install and backup guide; one restore performed and recorded (DoD-P07). |
| NFR-07 | Compatibility | The UI works in the current and the previous version of Chrome, Firefox, Safari and Edge (Q-50). | Playwright runs the E2E scenarios in Chromium, Firefox and WebKit; Edge is covered by Chromium [ASSUMPTION]. |
| NFR-08 | Responsiveness | The UI works from a 320 px wide window up, desktop first, no horizontal scrolling, at the three widths of UI-22 (Q-50, Q-53; UI-08). | Playwright screenshots at 375, 768 and 1440 px for every screen. |
| NFR-09 | Accessibility | The main flows (sign in, list, create or edit a Contact, export) meet WCAG 2.2 level AA. The interview asks for 2.1 level AA; the UI baseline (UI-19) is stricter and applies (Q-50). | Automated accessibility scan with no Critical or Serious violation (UI "Done for a screen"), plus keyboard-only walkthrough of the four flows. |
| NFR-10 | Usability | A new user needs no manual; every screen has a clear empty state, loading state and error message (UI-14) (Q-50). The proposed check "a new user signs in, creates a Group and adds a first Contact in under 2 minutes without help" is open (OQ-P02). | Wireframe review; E2E-01 passes with at most 12 user actions from the first page to the first Contact [ASSUMPTION]. |
| NFR-11 | Localisation | Every UI text exists in Slovene and English in message catalogs; dates, times and numbers follow the project-wide facts (Q-44, UI-30). | A test compares both catalogs key by key; screenshots in both languages. |
| NFR-12 | Deployability | The system starts from the delivered container images and a Docker Compose file with PostgreSQL, configured only by environment variables, on any host with Docker (Q-52). | A clean-host start from the images in the runtime demonstration. |
| NFR-13 | Licensing | All dependencies have permissive licences (Apache, MIT, BSD) and the product contains no copyleft code (Q-51). | Licence scan of both components; result in the verification report. |
| NFR-14 | Data integrity | A save never overwrites a newer version silently; a failed save stores nothing (principle P-2). | Acceptance tests AC-003-06 and the validation edge cases of US-003. |
| NFR-15 | Documentation | An OpenAPI description, an install and backup guide and a short user guide in Slovene are delivered (Q-55). | Release checklist (DoD-P04). |

## Thresholds

These thresholds are the **quality gate** of the project, per component, over the scope and with the tests defined in `general/testing/measurement.md`. The agent works until each component meets them, then stops adding tests for that component (rules and stop rule: "Quality gate" in `general/testing/measurement.md`). Keep the format `>= <number> %`. The interview gives no thresholds, so the defaults apply to both components.

| Measure | Threshold |
|---|---|
| Line coverage (all levels together) | >= 95 % |
| Mutation score (adjusted) | >= 90 % |
| Branch coverage | record only |

## Mutation scope

The paths or packages whose code is mutated (default: all production code of the component). Selection rule: validation, security, persistence and business-rule code. Rules: `general/testing/measurement.md`.

| Component | Paths or packages | Exclusions and reason |
|---|---|---|
| backend | all production code under `src/main/java/si/contactgroups` | None. |
| frontend | all production code under `src/` | None. |

## Overrides

None.
