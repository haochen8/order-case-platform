# Project instructions

Build a service fulfillment portfolio application: operators manage cases and
their provisioning orders; viewers can inspect the work and its audit trail.

## Architecture
- Keep business rules in the Spring services and data in PostgreSQL.
- Keep the Lovable frontend separate and consume the documented REST contract.
- Use Flyway for schema changes; Hibernate validates the schema.
- Persist audit events in the same transaction as the change they describe.
- Derive actors from authenticated identities, never caller-supplied headers.
- Protect edits with versions and preserve database constraints under concurrency.

## Verification
From `backend-spring`, use `GRADLE_USER_HOME=../.gradle-home ./gradlew test` for
fast tests and `GRADLE_USER_HOME=../.gradle-home ./gradlew integrationTest` for
PostgreSQL tests (Docker required). `check` runs both; missing Docker must fail
the integration task rather than report success with skipped tests.

For behavior changes, add regression tests at the API or persistence boundary.
Before delivery, run applicable checks, inspect the diff, and report precisely
which checks passed, failed, or could not run. Update the API/workflow docs when
behavior changes. Do not claim an undeployed feature is live.

## Work organization
Keep tasks bounded with explicit acceptance criteria. Separate ownership of
backend, frontend, and delivery files when parallel work is requested. Coordinate
API and schema changes before dependent work. Prefer existing targeted skills;
create a custom skill only for a workflow that demonstrably repeats.
