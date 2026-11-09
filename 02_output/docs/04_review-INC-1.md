# Review of INC-1

> For: agent (writes), human (reads) · Written in: phase 4 (`/review`) · Frame: criticise · No fixes here.

Range: `1035623^..HEAD` (first code commit to `067ed13`), stories US-001..US-019 of INC-1. Round 2: the first round (`ae71513`) found two High findings; `578fdf5` (Group shell, D-61) was the fix. This round re-checks every first-round finding and adds the checks the first round did not do (own screenshots at the three widths, live API calls again).

## How it was checked

| Check | Method | Coverage |
|---|---|---|
| Test-first evidence | every `feat` commit body searched for `Red:` lines (round 1: 81 `feat` commits); since `ae71513` the only code commit is `578fdf5` (`fix:`), its body has a `Red:` line | all |
| Authorisation, validation, errors, headers | round 1 live calls; round 2 repeated the malformed `page` calls and `/health` | Activity, Contacts, History, health |
| Output encoding, logs | round 1 grep (`dangerouslySetInnerHTML`, `console.*`, log statements); no code outside the Group shell changed since | whole frontend and backend |
| Wireframe comparison | **own screenshots** with Playwright (Chromium) of WF-05, WF-06, WF-07, WF-08, WF-09, WF-10, WF-11, WF-12, WF-13, WF-19 at 375, 768 and 1440 px as Owner Ana of Group Acme (seed data), plus a horizontal-overflow test per screen; read against the approved wireframes | 30 screenshots in `logs/screens/review-INC-1/`; WF-01..WF-04 not re-shot (unchanged since round 1, screens of the implementing sessions exist) |
| Group shell | code read (`features/groups/GroupShell.tsx`), tab bar at 768 and 1440 px, menu button at 375 px in all 30 screenshots | Owner Role only; the Viewer shell is covered by the implementer's checks (`logs/screens/WF-06_contact-list/group-shell-viewer-*.png`), not re-shot here |
| Rule-to-test mapping | sampled for US-017 and the security stories in round 1, not repeated | partial |

Not done: no axe scan of my own; I did not read every test of every story; Viewer and Editor screens were not re-shot. Data changed by this review: the stack's seed (Auth emulator users and `seed.sql`) was loaded into the running test stack because the rebuilt stack had no users.

## Status of the findings of round 1

| ID | Severity | Status | Evidence |
|---|---|---|---|
| F-01 | High | **Closed** | `GroupShell` is the layout route of all Group screens: Group bar (name, Role badge), tab bar "Kontakti, Značke, Koš, Dejavnost, Člani, Povabila" at 768 and 1440 px, menu button "Meni skupine: <screen>" at 375 px; seen on WF-06..WF-13 in my screenshots. Tab labels and order match the wireframes except the two deliberate gaps below. Test-first: `578fdf5` has `Red:`. |
| F-02 | High | **Closed** | "Značke" is a tab (menu entry on a phone); `WF-09_tags_*.png` reached by the shell. |
| F-03 | Medium | **Open** | live: `GET /api/v1/groups/{g}/activity?page=abc` and `…/contacts/{c}/history?page=abc` still answer 500 `server.error`; the Contact list answers 400 `request.malformed`. |
| F-04 | Medium | **Open** | live: `GET /health` still answers `{"groups":["liveness","readiness"],"status":"UP"}` (AC-018-09). |
| F-05 | Medium | **Closed** | screens WF-07, WF-08 and WF-19 now exist at the three widths (`logs/screens/review-INC-1/`) and were compared; findings from that comparison are F-10 and F-11. |
| F-06 | Medium | **Closed in part** | WF-13 date fields now show `dd. mm. yyyy` (Slovene) and the table dates `9. 11. 2026 13:32`. "Počisti filtre" is still a button below the filters; the wireframe has a text link (kept in F-07). |
| F-07 | Low | **Open** | WF-13 has only "Prejšnja"/"Naslednja" and "Stran 1 od 1"; the Contact list (WF-06) now shows page buttons. Page numbers on WF-13 and the text-link form of "Počisti filtre" (F-06) are wireframe details; no rule asks for either. |
| F-08 | Low | **Open, cannot close in INC-1** | frozen tests of INC-2 stories (US-020, US-024, US-026) and AC-006-04, AC-014-03 (D-58 g), AC-019-06 (D-61 g, a frozen helper that looks for the Slovene table name) fail; the release gate must re-run them. |
| F-09 | Low | **Open (hygiene)** | eleven scaffolding `feat` commits without a `Red:` line, as listed in round 1; nothing new. |

## New findings

| ID | Severity | Story | Finding |
|---|---|---|---|
| F-10 | Medium | US-003, US-004, US-005 (WF-07, Contact page) | At 375 px the Contact page scrolls sideways: `scrollWidth` 402 px against a 375 px viewport (UI-22, UI-25: no horizontal scroll). The two `<dl className="grid grid-cols-[max-content_1fr] …">` of the details and attributes (`features/contacts/ContactPage.tsx:207` and `:241`) size the label column to its longest label ("Ulica in hišna številka") and the value column cannot shrink; the `section` ends at x = 402. Other screens at 375 px do not overflow. Evidence: `logs/screens/review-INC-1/WF-07_contact_mobile.png` (page wider than the header). The wireframe stacks label above value on a phone. |
| F-11 | Low | US-018, US-019 (WF-19) | The 404 page has no `h1`. The wireframe has `<h1>Strani ni mogoče najti</h1>`; `NotFoundPage.tsx` renders `EmptyState`, which draws an `h2` (`components/EmptyState.tsx:7`). A page without a level-one heading; the same `EmptyState` is used for other states where an `h2` below the page `h1` is correct. Evidence: `WF-19_not-found_*.png`, DOM check (`h1` count 0). |
| F-12 | Low | US-007, US-009 (WF-06) | On the desktop Contact list the "Značke" column is clipped at the table edge: chips read "udelezenec20…" and are cut, not wrapped or truncated with an ellipsis (`ContactTable.tsx:140-155`, `truncate` cell with a chip container). Evidence: `WF-06_contacts_desktop.png`. |
| F-13 | Medium | US-007 | Carried from D-61 item f, still open: a Contact without a last name has an empty link in the name column (`ContactTable.tsx:149`, `{row.lastName \|\| ''}`); a link without text (WCAG 2.4.4, axe `link-name`, Serious). The implementer saw it and left it for `/fix` or the next session on US-007. |

## Checks without a finding

- Authorisation (SB-02, SR-01), validation on the trusted side (SB-01), headers (SB-10), logs (ES-07, SB-07), output encoding: as in round 1; no backend code changed since.
- The Group shell changes only frontend routing and layout: `routes.tsx`, `ContactListPage.tsx` (the four ad hoc buttons removed), `GroupShell.tsx`, two message catalogs; nothing outside the task (ES-10). Tabs by Role follow the rules (R-006-01, R-012-08, R-014-10, R-015-01, R-017-08), the server still enforces them. The Viewer now sees "Člani" (R-015-01 allows it; D-61 b).
- Differences from the wireframes that are decided and not findings: no tab "Podvojeni kontakti" (US-024) and no "Možnosti skupine" menu (US-020) (D-61 c, d); both belong to INC-2 and are named in the handover.
- Wording: Slovene UI text matches the wireframes on all re-shot screens; the language switch SL/EN is present.
- Structure per width: desktop tab bar and filter panel beside the list, tablet tab bar, phone menu and stacked cards (WF-06 mobile), sticky action bar (WF-08 mobile). Matches `wireframes/README.md` ("Layout per width"), except F-10.

## Definition of Done: increment

| DoD-I | Result | Evidence |
|---|---|---|
| DoD-I-01 | Met for INC-1 as far as it can be | every story is "passed" or "passed in part"; the open parts wait for INC-2 stories (F-08) and are re-run at the release gate |
| DoD-I-02 | **Met** | no Critical or High finding open (F-01, F-02 closed) |
| DoD-I-03 | Met | the stack runs, the flows of sign-in, Groups, Contacts, Tags, Trash, Members, Invitations and Activity work at runtime; the Tag list is reachable by tab |
| DoD-I-04 | Met with exceptions | 30 own screenshots match the wireframes in structure and wording; F-10 (phone overflow), F-11, F-12 are the deviations |

## Handover

No Critical or High finding is open, so INC-1 meets `DoD-I`; next is the first story of INC-2, `/implement US-020`. Medium and Low findings: F-03 (US-016, US-017), F-04 (US-018), F-10 (US-003..US-005) and F-13 (US-007) are for `/fix` or the next session on the story, otherwise the release notes; F-06/F-07, F-11, F-12 are Low. F-08 is re-run at the release gate.
