# Architecture decisions

## One deployable backend

Use a Spring Boot modular service with PostgreSQL. A separate audit microservice
would introduce delivery failure modes before there is a product need for one.
The frontend can be developed in Lovable and versioned in its own synced repo.

## Transactional history

Persist events using the same database transaction as each business change.
Audit recording requires an existing transaction. Failure to persist history
fails the write. Keep case IDs in audit records without a foreign key so history
remains queryable after deletion. Case history also includes linked order events.
If external delivery is required later, introduce an outbox and idempotent
consumer; do not replace the transaction with a best-effort HTTP call.

## Concurrency and integrity

A SQL foreign key restricts deletion of cases with orders. Versions detect stale
client edits. A parent case row lock serializes changes to a case and its orders,
including creation, closure, status changes, and deletion, to enforce invariants
that span multiple rows. The service locks the parent before loading mutable
order state; database version checks provide an additional safeguard.

Tradeoff: simultaneous writes to a single case serialize. This is acceptable for
a human-operated workflow. Measure contention before choosing a more complex
approach. No unverified performance or scalability claims are made.

## Identity boundary

The service validates externally issued JWTs and never trusts a user-supplied
actor header. Signature, issuer, audience, expiration, and subject are validated;
roles come from a documented claim. Browser login uses OIDC Authorization Code
with PKCE. CSRF is disabled only because the API accepts bearer headers and has
no cookie/session authentication. If cookies are introduced, revisit CSRF.
Keycloak in Compose provides disposable local identities; production identity
provisioning is a later deployment task.

## Schema and verification

Flyway creates the production schema and Hibernate validates mappings against it.
H2 tests give fast API feedback but are not evidence of PostgreSQL compatibility.
Mandatory Testcontainers tests exercise the real migration, API, and constraints.
CI runs both suites and builds an executable JAR, catching packaging failures
that a unit-only build would miss.
