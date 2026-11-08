---
name: design
description: Session D, phase 4, after the last increment. Turn the grey, wireframe-styled application into its final design - colour, typography, imagery, motion and responsive polish for mobile, tablet and desktop - one question at a time with the developer, showing before and after screenshots, changing presentation only. With "Design round: skip" it applies the baseline final theme without questions.
argument-hint: "[resume | answers | baseline]"
disable-model-invocation: true
---

> For: agent · Owner: Design lead · Agent: read-only

## Session

- Id: D, phase 4, once after the last increment. Frame: ask and generate in small loops (generate only with "Design round: skip").
- Reads: `general/design/ui-baseline.md`; `project/02_design/ui-design.md`; `docs/wireframes/README.md`; earlier design decisions in `docs/decisions-log.md` (records with an `Overrides:` line); `project/00_setup/run-config.md` ("Design round"); the running application and its screenshots.
- Writes: token and style files, presentational parts of components, `docs/decisions-log.md`, `docs/00_progress.md`, `out/logs/screens/`.
- Must not: change behaviour, data, validation, routes, structure or any test; ship a choice that breaks WCAG 2.2 AA.
- Exit: the final theme is applied, every topic is decided or the developer stops, and the full suite passes.

You change **presentation only**: tokens, style files, spacing, component variants, icons, imagery, motion, copy tone.

## Procedure

1. Do not ask again what an earlier decision settled.
2. Start the application with the seed data and take the "before" screenshots of every screen at the three widths (UI-22), light and dark, into `out/logs/screens/design-<round>/before/`.
3. **Without the developer** (`run-config.md` "Design round: skip", or the argument `baseline`): switch to the final theme with the baseline defaults and the `ui-design.md` overrides, do steps 5 and 6 for the whole set without asking, and go to step 9.
4. **With the developer:** ask about one topic at a time, in this order, skipping what is already settled:
   1. overall impression and reference products the developer likes;
   2. colour: primary accent, neutrals, light and dark;
   3. typography: typeface, size, weight;
   4. density and spacing, corner radius, borders and shadows;
   5. mobile: navigation menu, sticky actions, how tables and forms collapse (UI-23);
   6. tablet: two-column layouts, side bar (UI-24);
   7. desktop: panels, column use, hover details (UI-25);
   8. components: buttons, forms, tables, empty and error states;
   9. icons, illustrations, logo;
   10. motion and feedback;
   11. tone of the copy (wording only, never meaning).
   Give your recommendation and why. For a large choice (colour, typeface, a responsive pattern) show **two or three variants** as screenshots and let the developer pick; for a small one implement your recommendation directly.
5. Implement the answer in the token and style files first, components only where a token cannot express it (UI-03).
6. Run the UI tests and the accessibility check, and take the "after" screenshots at the three widths. If a choice would break WCAG 2.2 AA (UI-19..21), say so and propose the closest compliant option; never ship a violation.
7. Show before and after and ask: "Is it right?" If not, ask what to change and repeat 5–7. If yes, commit (`refactor:`, style only) and record the decision: non-blocking, title `Design: <topic>`, the developer's answer as `Human response`, a line `Overrides: UI-nn` and the commit.
8. A wish that changes structure or behaviour (a new field, a moved section, a removed screen) is not design: record it as a blocking decision for a changed story or wireframe and continue with the next topic.
9. Run the full suite once; every test must pass. Update `docs/00_progress.md` (next: `/quality-gate`).
