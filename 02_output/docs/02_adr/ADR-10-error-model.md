# ADR-10: Error format

- Status: accepted (S3 /plan)
- Drivers: R-018-09, SR-12, AR-08, R-019-07, SB-07

## Decision

Every failure is JSON `{ "code": "contact.not_found", "message": "Contact not found", "fieldErrors": [{"field":"email","code":"validation.email_invalid"}], "current": {...}? }`. `code` is a stable dotted machine code; `message` is a short English text for developers; the frontend translates `code` (and `fieldErrors[].code`) through the message catalog. `current` is present only on 409 (the current Contact). Status mapping: 400 malformed body or unknown property, 405 method not allowed, 415 unsupported media type, 401 token, 403 role or unverified e-mail, 404 not found or not a Member, 409 version conflict or duplicate Group name or Role conflict (last Owner), 413 body over 1 MB, 422 validation (BR-06 etc.) and Selection over 10,000, 429 rate limit. `ApiExceptionHandler` is the only place that writes an error; stack traces, SQL and class names never leave the server (ES-07).

## Consequences

- One schema in the OpenAPI contract for all errors.
- The code list is part of the specification and the catalog parity test checks that every code has a text in both languages.
