# Backend foundation verification

Verified locally on 2026-09-29 against the current implementation.

| Check | Result |
| --- | --- |
| `./gradlew check bootJar` using Gradle 8.14.3 | Passed |
| Fast tests (API, token validation, repository) | 14 passed; 0 skipped |
| PostgreSQL 16 Testcontainers tests | 16 passed; 0 skipped |
| Flyway V1 + Hibernate schema validation on PostgreSQL | Passed |
| Concurrent edits and case closure/order creation race | Passed |
| Atomic audit rollback and audit-write failure rollback | Passed |
| Database foreign-key enforcement | Passed |
| `docker compose config --quiet` | Passed |
| Packaged JAR + local PostgreSQL + Keycloak 26.7.4 | Started successfully |
| `python3 scripts/smoke_api.py` with real signed tokens | Passed |
| `git diff --check` | Passed |

The smoke test verified anonymous/invalid-token rejection, viewer write denial,
order progression, premature closure rejection, stale order edits, final case
closure, five attributable audit events, and authenticated OpenAPI access. It
created one closed fictional case in the local development database.

Fast and integration tests used the Java 17 Gradle toolchain. The packaged-JAR
smoke run used the host Java 20 runtime. The Docker Compose database and identity
services were exercised; the backend Docker image itself has not been built or
run in this verification. The GitHub Actions workflow is configured but has not
yet run remotely. A browser frontend and public deployment remain future work.

Generated test reports are under `backend-spring/build/reports/tests/`. Re-run
checks after implementation changes; these results describe this milestone,
not a permanent certification of future changes.
