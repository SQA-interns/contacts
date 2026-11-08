# UI design

> For: agent (reads), human (reviews) · Owner: Design lead · Agent: read-only

Project overrides of `general/design/ui-baseline.md`. Everything not listed here follows the baseline. The customer gave no brand, logo, colours or reference products; the first version uses the neutral default of Q-53, and the brand comes later from the customer's feedback (OQ-P08).

## Brand and tone

| Item | Value | Source |
|---|---|---|
| Primary colour | None given. The baseline accent applies (UI-04); the brand colour replaces it later, in the token file only. | Q-53 |
| Logo | None. The product name as text. The logo is a replaceable item in one place. | Q-53 |
| Typeface | None given. The baseline applies (UI-06). | Q-53 |
| Tone of the copy | Friendly and short. Product name: "Contact Groups" (Slovene "Skupine kontaktov"), working title. | Q-53 |
| Reference products | None given. Which screens of the SCM example the customer likes is unknown (OQ-P03). | Q-53 |

## Overrides

| Baseline rule | Override | Reason | Source |
|---|---|---|---|
| UI-08 | Desktop first: layouts are designed first for the desktop width and then adapted down to a narrow phone window; the product still works from 320 px without horizontal scrolling and is checked at the three widths of UI-22. | The users work at a desk with long lists; the customer answer asks for desktop first and responsive down to a phone. | Q-53, Q-50 |
| UI-03 | The product name, the logo and the colours live in one place (the token and brand file), so that the customer's brand can replace them without touching a component. | The brand is expected from the customer later. | Q-53 |
