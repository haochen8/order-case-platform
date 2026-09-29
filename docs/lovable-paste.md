# Order Case Platform — paste into Lovable

## First iteration scope

Build only the application shell and case queue in this first iteration. Use isolated development mocks matching the data models below. The Spring Boot API currently runs locally and has not been publicly deployed. Keep its base URL configurable. Do not attempt live API or OIDC connections yet. Represent viewer/operator behavior with a clearly marked development-only role switch; this is not real authentication. Do not create a replacement backend or database. Stop after this first screen so I can review it before adding case details, order management, and real login.

## Product and design brief

Build a professional service fulfillment operations console called Order Case
Platform. This is a portfolio application for handling customer service cases
and provisioning work orders. The Spring Boot REST API in the attached contract
is the sole source of business data and business rules.

Create a responsive React/TypeScript frontend with a restrained light theme,
clear typography, accessible contrast, and status labels that do not depend on
color alone. Use a compact sidebar and a spacious main workspace. Prioritize
usable forms and clear data tables over decorative dashboards.

Target routes for the complete frontend (only implement the case queue in this first iteration):
1. Case queue: searchable, filterable by status, server-paginated table with title,
   status, creation date, and a create-case action for operators.
2. Case detail: title/description/status, edit dialog, linked orders, add-order
   dialog, valid order progression actions, and a paginated activity timeline.
3. Login/session states using an OIDC provider and Authorization Code with PKCE.

Use environment-configured API base URL, OIDC issuer, and client ID. Use a typed
API module and an established OIDC client. The local identity provider is
`http://localhost:9090/realms/case-platform`, client `case-platform-web`.
Keep tokens in memory. Never put a client secret in browser code. VIEWER is
read-only; OPERATOR can write. Backend permissions remain authoritative.

Handle loading, empty search results, field validation, permission failures,
expired sessions, network failures, and version conflicts. For HTTP 409, explain
that the data changed or a workflow rule prevents the action, offer refresh, and
preserve the user's draft where possible. Do not silently overwrite changes.
Disable duplicate submission and do not blindly retry POST requests.

Use real API data when connected. Keep any development mocks clearly isolated.
Do not add a Supabase/Lovable business database or a second workflow backend.
Do not invent customer, billing, analytics, assignment, or SLA endpoints.

## API and workflow reference

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
`actor`, `timestamp`, and `payload`. Case update payloads include before/after
snapshots; order updates include previousStatus/status. History is append-only
through the API, not cryptographically tamper-proof against database admins.

## Errors

Validation/type errors: 400. Missing/invalid credentials: 401. Wrong role: 403.
Missing resource: 404. Stale version or conflicting workflow/data: 409.
Unexpected failures: 500, details logged server-side.

Application errors use `{ "timestamp", "status", "error", "message", "path" }`.
CORS rejection is handled by the security framework and may return plain text.
The UI must handle non-JSON/network failures too.

## Data model reference

Derived from the exported OpenAPI schema. Request requirements below come from the schema; additional business rules and validation are specified above. Response required-field metadata is incomplete in the generated schema, so do not infer that every response field is optional. Case descriptions can be null. Use the documented status codes above (creation 201, deletion 204); generated OpenAPI currently labels these as generic 200 responses.

### AuditEventResponse

| Field | Type | Constraints |
| --- | --- | --- |
| actor | string | — |
| caseId | string (uuid) | — |
| entityId | string (uuid) | — |
| entityType | string | — |
| eventType | string | — |
| id | string (uuid) | — |
| payload | object | — |
| timestamp | string (date-time) | — |

### CaseCreateRequest

| Field | Type | Constraints |
| --- | --- | --- |
| description | string | maxLength 4000 |
| title | string | Required; maxLength 255 |

### CaseResponse

| Field | Type | Constraints |
| --- | --- | --- |
| createdAt | string (date-time) | — |
| description | string | — |
| id | string (uuid) | — |
| status | OPEN / IN_PROGRESS / CLOSED | — |
| title | string | — |
| updatedAt | string (date-time) | — |
| version | integer (int64) | — |

### CaseUpdateRequest

| Field | Type | Constraints |
| --- | --- | --- |
| description | string | maxLength 4000 |
| status | OPEN / IN_PROGRESS / CLOSED | — |
| title | string | maxLength 255 |
| version | integer (int64) | Required |

### OrderCreateRequest

| Field | Type | Constraints |
| --- | --- | --- |
| caseId | string (uuid) | Required |
| status | PENDING / SENT / COMPLETED / FAILED | — |
| type | string | Required; maxLength 255 |

### OrderResponse

| Field | Type | Constraints |
| --- | --- | --- |
| caseId | string (uuid) | — |
| createdAt | string (date-time) | — |
| id | string (uuid) | — |
| status | PENDING / SENT / COMPLETED / FAILED | — |
| type | string | — |
| version | integer (int64) | — |

### OrderUpdateRequest

| Field | Type | Constraints |
| --- | --- | --- |
| status | PENDING / SENT / COMPLETED / FAILED | Required |
| version | integer (int64) | Required |

### PageAuditEventResponse

| Field | Type | Constraints |
| --- | --- | --- |
| content | Array of AuditEventResponse | — |
| empty | boolean | — |
| first | boolean | — |
| last | boolean | — |
| number | integer (int32) | — |
| numberOfElements | integer (int32) | — |
| pageable | PageableObject | — |
| size | integer (int32) | — |
| sort | SortObject | — |
| totalElements | integer (int64) | — |
| totalPages | integer (int32) | — |

### PageCaseResponse

| Field | Type | Constraints |
| --- | --- | --- |
| content | Array of CaseResponse | — |
| empty | boolean | — |
| first | boolean | — |
| last | boolean | — |
| number | integer (int32) | — |
| numberOfElements | integer (int32) | — |
| pageable | PageableObject | — |
| size | integer (int32) | — |
| sort | SortObject | — |
| totalElements | integer (int64) | — |
| totalPages | integer (int32) | — |

### PageOrderResponse

| Field | Type | Constraints |
| --- | --- | --- |
| content | Array of OrderResponse | — |
| empty | boolean | — |
| first | boolean | — |
| last | boolean | — |
| number | integer (int32) | — |
| numberOfElements | integer (int32) | — |
| pageable | PageableObject | — |
| size | integer (int32) | — |
| sort | SortObject | — |
| totalElements | integer (int64) | — |
| totalPages | integer (int32) | — |

### PageableObject

| Field | Type | Constraints |
| --- | --- | --- |
| offset | integer (int64) | — |
| pageNumber | integer (int32) | — |
| pageSize | integer (int32) | — |
| paged | boolean | — |
| sort | SortObject | — |
| unpaged | boolean | — |

### SortObject

| Field | Type | Constraints |
| --- | --- | --- |
| empty | boolean | — |
| sorted | boolean | — |
| unsorted | boolean | — |

