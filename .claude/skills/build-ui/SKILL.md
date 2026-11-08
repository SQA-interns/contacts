---
name: build-ui
description: Build or restyle one screen so it matches its approved wireframe and the UI baseline (tokens, components, responsive layout, the four states, accessible), and check it with screenshots and an accessibility scan. Used by /implement for every screen of a story and by /fix for UI findings.
---

> For: agent · Owner: Design lead · Agent: read-only

Reads: the screen's entry in the specification; the AC that mention it; its approved wireframe (`docs/wireframes/WF-nn_*`); `general/design/ui-baseline.md`; `project/02_design/ui-design.md`; after `/design`, the design decisions (`docs/decisions-log.md`, records with an `Overrides:` line).

## Procedure

1. Until `/design` has run, build in the wireframe theme (UI-26..29); afterwards in the final theme.
2. Build the structure from the component library so it matches the wireframe at each width: layout (UI-08..11, UI-22..25), then components (UI-12..17), only tokens for colour, type, spacing and radius (UI-04..07). Styling in style or token files, logic in the component (UI-03); user-facing text from the message catalog when there is one (UI-30).
3. All four states (UI-14) and the busy, error and confirmation feedback (UI-12, UI-13, UI-15); dates, numbers and currency by the project-wide facts (UI-18).
4. Accessibility (UI-19..21): semantic elements and landmarks, labels and names, keyboard order, visible focus, target sizes, reduced motion. Run the automated check on every state; fix every Critical and Serious violation.
5. Screenshots at the three widths (UI-22) with the seed data into `out/logs/screens/<screen>/`; fix overflow, overlap, unstyled elements, uneven spacing and broken alignment until none is left.
6. Run the screen's tests (they find elements by role, label or text).
