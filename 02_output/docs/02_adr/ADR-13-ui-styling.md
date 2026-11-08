# ADR-13: Tokens, themes and components

- Status: accepted (S3 /plan)
- Drivers: UI-01..07, UI-26..29, UI-03 override, UI-19..21, ES-12

## Decision

Tailwind CSS 4 with CSS variables for the semantic tokens of UI-04 (`--color-background`, `surface`, `surface-raised`, `border`, `text`, `text-muted`, `primary`, `primary-foreground`, `success`, `warning`, `danger`, `ring`) plus spacing, radius, font and breakpoint tokens. Two theme files, `styles/themes/wireframe.css` and `styles/themes/final.css`, define the same variables; `data-theme="wireframe"` is set on `<html>` in the skeleton, and `final` is ready with the baseline palette (indigo-600) and dark mode through `prefers-color-scheme`. The wireframe theme has greys only, dashed 1 px borders, no shadow or animation (UI-26). Product name, logo placeholder and brand colour live in `src/brand.ts` and `styles/brand.css` only (override of UI-03). shadcn components (Radix primitives, `class-variance-authority`, Lucide icons) are copied into `src/components/ui/` and read only tokens. Layout: mobile 375, tablet 768, desktop 1440 with breakpoints 640/768/1024/1280, content width 1200, gutters 16/24. `prefers-reduced-motion` turns motion off.

## Consequences

- `/design` changes token files and `brand.*` only; no component or test changes.
- Tables below 768 px become stacked cards through one `ResponsiveTable` component.
