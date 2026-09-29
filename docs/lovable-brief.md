# Lovable frontend brief

For a single text-only prompt, copy the contents of [lovable-paste.md](lovable-paste.md).
It includes the first-iteration scope, workflow contract, and data-model tables;
no JSON attachment is required.

The backend milestone passed local verification. Attach
`docs/api-contract.md` and `docs/openapi.json` (exported from the verified API).
Regenerate that schema from `/v3/api-docs` whenever the API changes. Start with the
shell and case queue, review the result, then add the detail workflow. Connect
Git sync early so changes can be reviewed and tested outside Lovable.

## Initial build prompt

Build a professional service fulfillment operations console called Order Case
Platform. This is a portfolio application for handling customer service cases
and provisioning work orders. The Spring Boot REST API in the attached contract
is the sole source of business data and business rules.

Create a responsive React/TypeScript frontend with a restrained light theme,
clear typography, accessible contrast, and status labels that do not depend on
color alone. Use a compact sidebar and a spacious main workspace. Prioritize
usable forms and clear data tables over decorative dashboards.

Implement these routes:
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

## Acceptance walkthrough

With real local services: operator logs in, opens a case, creates an order, sees
that premature case closure is rejected, sends and completes the order, closes
the case, and sees all changes in its timeline. A viewer can inspect the same
records but cannot mutate them. An edit from a stale browser tab receives a
recoverable conflict. Keyboard-only navigation can complete the forms.

## Credit checkpoints

Treat the 100-credit allocation as a ceiling to monitor, not a guarantee of
completion. Review the shell/queue before adding other screens. Reserve a
substantial portion for live integration, accessibility, and error-state fixes.
Avoid repeated whole-app regeneration after the initial layout is accepted.
