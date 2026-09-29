# Workflow and REST contract

## Scope and identity

One organization, two roles: VIEWER (read) and OPERATOR (read/write). There is no
multi-tenant isolation in this release. Cases describe service work; orders are
provisioning tasks within a case, not shopping-cart purchases.

All `/api` requests require an OAuth bearer access token. Tokens must have the
configured issuer, valid signature and expiry, audience `case-platform-api`, a
nonblank subject no longer than 255 characters, and `roles: ["VIEWER"]` or
`roles: ["OPERATOR"]`. The actor in history is the token subject. `X-Actor` is
ignored. The only anonymous operational endpoint is `/actuator/health` and its
health subpaths. Health is backed by Actuator rather than a constant UP response.

For the browser use Authorization Code + PKCE, client `case-platform-web` in the
local realm. Keep tokens in memory, and never put a client secret in frontend
code. Local redirect origins are restricted to `http://localhost:5173`; configure
exact deployed origins in both the identity provider and backend for hosting.

## Cases

- New cases start OPEN.
- OPEN → IN_PROGRESS or CLOSED; IN_PROGRESS → CLOSED.
- CLOSED is terminal. Title and description can still be corrected with a version.
- Closing requires no PENDING or SENT orders. FAILED and COMPLETED are terminal
  outcomes that permit closure. Retrying failed work means creating a new order
  while the case is still active.
- Delete only cases with no orders. History remains readable after deletion.

## Orders

- New orders start PENDING, and require an existing, non-closed case.
- PENDING → SENT or FAILED; SENT → COMPLETED or FAILED.
- COMPLETED and FAILED are terminal. Orders have no delete endpoint.
- Repeating the current status is a no-op; it does not create another audit event.
- All modifications acquire the parent case lock before loading mutable state.

## Endpoints

| Method | Path | Input / behavior |
| --- | --- | --- |
| GET | `/api/cases` | `page=0`, `size=20`, optional `status`, `search` |
| POST | `/api/cases` | `{ "title": "Install fiber", "description": "..." }` |
| GET | `/api/cases/{id}` | Case response |
| PUT | `/api/cases/{id}` | Required `version`; optional `title`, `description`, `status` |
| DELETE | `/api/cases/{id}?version=0` | Required current version; 204 if removed |
| GET | `/api/cases/{id}/audit` | Paginated case AND order events, oldest first |
| GET | `/api/orders` | Pagination; optional `caseId` |
| POST | `/api/orders` | `{ "caseId": "UUID", "type": "FIBER_INSTALL" }`; optional status must be PENDING |
| GET | `/api/orders/{id}` | Order response |
| PUT | `/api/orders/{id}` | `{ "version": 0, "status": "SENT" }` |

Pagination is zero-based, size 1–200. Lists expose Spring Page JSON, including
`content`, `number`, `size`, `totalElements`, and `totalPages`. Case/order lists
sort by createdAt descending with id as a tie-breaker. Audit sorts by timestamp
ascending with id as a tie-breaker. Timestamps are UTC ISO-8601 values.

Case responses contain `id`, `title`, `description`, `status`, `createdAt`,
`updatedAt`, and `version`. Order responses contain `id`, `caseId`, `type`,
`status`, `createdAt`, and `version`. Updates return the new version. Never
silently retry a 409 with an overwritten version; reload and let the user review.

PUT currently has partial-update semantics: omitted or null fields are unchanged.
Send an empty description to clear it; blank titles are rejected. Version is
required even for a no-op. POST is not idempotent: disable duplicate submission
in the UI and do not automatically retry creation after an ambiguous timeout.

Audit entries contain `id`, `caseId`, `entityType`, `entityId`, `eventType`,
`actor`, `timestamp`, and `payload`. `CASE_CREATED` has an `after` snapshot;
`CASE_UPDATED` has `before` and `after` snapshots; `CASE_DELETED` has `before`.
Each case snapshot contains `title`, `description`, and `status`.
`ORDER_CREATED` has `type` and `status`; `ORDER_UPDATED` has `previousStatus`
and `status`. History is append-only
through the API, not cryptographically tamper-proof against database admins.

## Errors

Validation/type errors: 400. Missing/invalid credentials: 401. Wrong role: 403.
Missing resource: 404. Stale version or conflicting workflow/data: 409.
Unexpected failures: 500, details logged server-side.

Application errors use `{ "timestamp", "status", "error", "message", "path" }`.
CORS rejection is handled by the security framework and may return plain text.
The UI must handle non-JSON/network failures too.

## Lovable handoff

Build a responsive operations console with a searchable case queue, case detail,
linked order table, forms, and a paginated activity timeline. Use this contract
and `/v3/api-docs`. Configure the API base URL and OIDC issuer/client as environment
settings. Implement loading, empty, validation, permission, conflict, and network
failure states. Hide write actions for viewers, with backend enforcement intact.
Do not create a second business database or duplicate workflow rules in a serverless
backend. Mock data is for isolated UI development only; the acceptance demo must
use the real API. The frontend repository and hosted URLs are not created yet.
