# UI baseline

> For: agent · Owner: Design lead · Agent: read-only

The default look and behaviour of every user interface, so that a product is modern, responsive and consistent even when the customer gave no design. It applies whenever `project/02_design/ui-design.md` or a design decision (`docs/decisions-log.md`, records with an `Overrides: UI-nn` line) does not say otherwise. Following it is required: "do not invent behaviour" applies to functionality, not to presentation. Procedures: `skills/wireframes`, `skills/build-ui`, `skills/design`.

## Design stages

| Stage | Session | Result | Theme |
|---|---|---|---|
| 1 Wireframes | `/wireframes` (phase 2) | every screen as a grey wireframe at the three widths (UI-22), approved by the developer | wireframe |
| 2 Built screens | `/implement` (phase 4) | the working application looks like the approved wireframes: complete, without colour (milestone) | wireframe |
| 3 Final design | `/design` (phase 4, after the last increment) | colour, typography, imagery, motion and responsive polish | final |

*Why:* structure and flow are agreed while they are cheap to change; colour and motion come last, in token files only, so they never touch logic or tests.

## Stack

| ID | Rule |
|---|---|
| UI-01 | Use a design-token based styling system and an accessible component library, pinned in `tech-stack.md`. Default for a React web app: Tailwind CSS with CSS-variable tokens, shadcn/ui components (Radix primitives) and Lucide icons. For another stack, choose the equivalent in `/to-spec`. |
| UI-02 | No unstyled browser defaults are visible anywhere: every element uses the tokens and components. |
| UI-03 | Styles live in style or token files (`general/testing/measurement.md`, "Presentation is not logic"); components contain logic and structure. The wireframe and the final theme are two token sets; switching them changes no component. |

## Tokens

| ID | Rule |
|---|---|
| UI-04 | Colours are semantic tokens, never raw values in components: background, surface, surface-raised, border, text, text-muted, primary, primary-foreground, success, warning, danger, focus ring. Final default palette: neutral greys with one primary accent (indigo-600, or the brand colour from `ui-design.md`). |
| UI-05 | Light and dark mode from the same tokens; follow the system setting; text contrast at least 4.5:1 (3:1 for large text and UI parts) in both. |
| UI-06 | Typography: one sans-serif family (system UI stack or Inter); scale 12 / 14 / 16 / 20 / 24 / 32 px; body 16 px with line height 1.5; two weights (400, 600). |
| UI-07 | Spacing on a 4 px grid (4, 8, 12, 16, 24, 32, 48); corner radius 8 px for controls, 12 px for cards; thin 1 px borders; at most one subtle shadow level. |

## Wireframe theme

| ID | Rule |
|---|---|
| UI-26 | Greys only: white page, light grey surfaces, mid-grey 1 px dashed borders for regions and controls, dark grey text. No colour, image, shadow, gradient or animation. |
| UI-27 | Real structure and words: header with logo placeholder, product name, navigation, search and user menu; page titles, section headings, labels, column headers, tabs, buttons and the footer use the final wording (glossary terms, ES-13). |
| UI-28 | Dynamic content is shown as grey placeholder bars of plausible length; metrics and charts as labelled boxes; images and the logo as labelled boxes. |
| UI-29 | Every screen and every state (UI-14) exists at the three widths (UI-22), with the layout each width will have in the final design. |

## Layout and responsiveness

| ID | Rule |
|---|---|
| UI-08 | Mobile first; works from 320 px wide without horizontal scrolling; breakpoints 640, 768, 1024, 1280 px. |
| UI-09 | Content width at most 1200 px, centred, with 16 px side gutters on mobile and 24 px or more on larger screens. |
| UI-10 | Every page has a header with the product name and navigation, a clear page title, and one primary action at most. |
| UI-11 | Related content is grouped in cards or sections with consistent spacing. |
| UI-22 | Every screen is designed, built and checked at three widths: **mobile 375 px, tablet 768 px, desktop 1440 px** (screenshots in that order, named `mobile`, `tablet`, `desktop`). Every other file refers to "the three widths (UI-22)". |
| UI-23 | Mobile: one column; navigation collapses into a menu button; primary action reachable without scrolling or as a sticky bar; forms one field per row. |
| UI-24 | Tablet: two columns where content allows (list and detail, form and preview); navigation visible or in a collapsible side bar. |
| UI-25 | Desktop: multi-column layout as in the wireframes, side panels open, tables with all columns; hover states never carry information that touch users cannot reach. |

## Components and states

| ID | Rule |
|---|---|
| UI-12 | Buttons have three levels (primary, secondary, ghost), a visible hover, active, focus and disabled state, and a busy state with a spinner while an action runs. |
| UI-13 | Every form field has a visible label, a hint where useful, a required marker, and an inline error next to the field; errors are announced to assistive technology; the entered values are kept on error. |
| UI-14 | Every screen that loads data has a loading state (skeleton), an empty state (short explanation and the next action), an error state (plain message, retry, no technical detail) and the success state. |
| UI-15 | Feedback for completed actions uses a toast or an inline confirmation; destructive actions ask for confirmation. |
| UI-16 | Tables become stacked cards or scroll inside their container below 768 px; long text truncates with the full value available. |
| UI-17 | Icons from the one icon set only, always with a text label or an accessible name. |
| UI-18 | Dates, times, numbers and currency are formatted as `business-rules.md` (project-wide facts) defines. |
| UI-30 | When the project-wide facts list more than one language, every user-facing text comes from a message catalog, never from code. |

## Accessibility and motion

| ID | Rule |
|---|---|
| UI-19 | WCAG 2.2 AA: semantic HTML, landmarks, one `h1` per page, every image with alternative text, every control reachable and usable by keyboard in a logical order. |
| UI-20 | A visible focus ring on every interactive element; touch targets at least 44 × 44 px. |
| UI-21 | Motion is short (150–250 ms), only for state changes, and off when the user prefers reduced motion. |

## Done for a screen

A screen is done when, at the three widths (UI-22), and in the final stage also in light and dark mode, it matches its approved wireframe in structure and layout, meets every rule above for its stage, has all four states (UI-14), passes the automated accessibility check with no Critical or Serious violation, and its screenshots show no overflow, overlap or unstyled element.
