---
name: wireframes
description: Session W, phase 2. Draw every screen of the specification as a grey wireframe (real structure and wording, placeholder content, no colour) at mobile, tablet and desktop width, render them to PNG, and get the developer's approval before any screen is built.
argument-hint: "[answers, e.g. 'D-07: 2 WF-03 move filters to the left']"
disable-model-invocation: true
---

> For: agent · Owner: Design lead · Agent: read-only

## Session

- Id: W, phase 2. Frame: generate, then the developer approves.
- Reads: the "User interface" section of `docs/02_specification.md`; per screen the stories and AC that mention it; the glossary in `project/01_requirements/business-rules.md`; `general/design/ui-baseline.md` ("Wireframe theme", "Layout and responsiveness"); `project/02_design/ui-design.md`; `general/rules/decisions.md` ("Decision record").
- Writes: `docs/wireframes/`, `docs/decisions-log.md`, `docs/00_progress.md`.
- Must not: use colour or final styling; add screens or elements no story needs.
- Exit (completes the phase 2 gate): every screen has a wireframe at the three widths (UI-22), and the developer approved them.

The wireframes fix structure, layout and wording; colour and motion come later in `/design`.

## Procedure

1. If the argument holds the developer's changes, record them as `Human response` of the approval decision, apply only those changes (step 3), then continue at step 4.
2. Give each screen an id `WF-nn` in navigation order and list it in `docs/wireframes/README.md`: id, screen, stories and AC, states, files.
3. For each screen write `docs/wireframes/WF-nn_<slug>.html`: static HTML with one shared `docs/wireframes/wireframe.css` that implements the wireframe theme (UI-26..29), the final wording and glossary terms (ES-13), the shared header and footer, and the layout of UI-23..25 per width. Draw the loading, empty and error states as labelled sections.
4. Render every page at the three widths with the browser test runner into `docs/wireframes/WF-nn_<slug>_mobile.png`, `…_tablet.png`, `…_desktop.png` (full page). Look at every image; fix overflow, overlap, cut-off text and uneven spacing.
5. Check: every screen of the inventory has a wireframe; every AC a user sees has its element; navigation reaches every screen; no colour.
6. Commit (`docs:`). Record an approval decision "Approve wireframes" listing the image files, with options: 1 approved with the customer (default), 2 approved by the developer only, 3 change (what, per `WF-nn`). Hand over waiting for it (next: `/wireframes`) and end the session.
7. When approved: record the option chosen and hand over (next: `/acceptance-tests`). The approved wireframes bind `/acceptance-tests`, `/implement` and `/design`; a later change to structure is a decision, not a styling choice.
