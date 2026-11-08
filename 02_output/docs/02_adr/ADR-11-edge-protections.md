# ADR-11: Rate limit, request size, HTTPS, CORS, headers, logs

- Status: accepted (S3 /plan)
- Drivers: SR-07, SR-08, SB-04, SB-06, SB-10, R-018-10..13,16, D-19(f)

## Decision

Filter order: `RequestSizeFilter` (1 MB by `MAX_REQUEST_SIZE`, counted while reading when `Content-Length` is absent; 413) -> `HttpsFilter` (when `REQUIRE_HTTPS=true`, a request without `X-Forwarded-Proto: https` is refused with 403 `https.required`; `/health` excluded so the container health check works) -> CORS (one origin, `CORS_ALLOWED_ORIGIN`) -> `FirebaseTokenFilter` -> `RateLimitFilter`. The limit is Bucket4j with one bucket per User id: 100 tokens refilled intervally per minute (`RATE_LIMIT_PER_MINUTE`), every authenticated request counts, 4xx included, answer 429 `rate.limited` with `Retry-After` from the 101st (D-19f). The frontend debounces typing; the load script stays at or below 100 requests per User per minute. Responses carry `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store`, `Content-Security-Policy: default-src 'none'` (API). `SecurityLogger` writes one line for failed authentication, 401, 403, 404 and 429 with time, method, path pattern, status and User id, and never the query string, body or token. Server log retention (30 days) is the operator's.

## Consequences

- `REQUIRE_HTTPS` is a new key (D-29); local and test keep it `false`.
- In-memory buckets are per instance; the product runs one backend instance.
