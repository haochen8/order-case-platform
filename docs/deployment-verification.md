# Local release preparation verification — 2026-09-29

No public host was deployed. These checks used the separate `order-case-release`
Compose project, new volumes and generated `.example.test` inputs. The existing
local development database was not used. Temporary loopback mappings exposed API
18080 and identity management 19000 for checks; the committed release file
publishes only Caddy ports 80/443.

| Check | Result |
| --- | --- |
| `./gradlew check bootJar` | Passed: 15 fast + 17 PostgreSQL tests; 0 failures/skips |
| Release input regression tests | 3 passed: HTTPS origins, unsafe host input, no local accounts/grant client |
| Release Docker build | Passed with production profile; API image ID `sha256:33efa8ab59a5f306da3ab041f576c7b4d94231ce16a8aa546351ae0588e146e8` |
| Compose validation | Passed with private generated inputs |
| Caddy configuration validation | Passed |
| Keycloak production startup | PostgreSQL schema initialized, realm imported, readiness UP |
| OIDC discovery | Public HTTPS issuer matched `https://auth.example.test/realms/case-platform` |
| API runtime | Non-root `app`, read-only root filesystem, container healthy, aggregate `{"status":"UP"}` |
| API/CORS through proxy | Unauthenticated read 401; configured-origin preflight 200; foreign-origin preflight 403 with no allow-origin header |
| Ingress restrictions | `/admin/`, `/realms/master/`, `/v3/api-docs`, `/metrics` returned 404 |
| Container restart | API and identity returned to ready; business fixture and realm persisted |
| Private files | Generated environment and realm mode 0600, ignored by Git |
| Backup/restore | Both custom-format dumps restored without errors into fresh scratch databases |
| Restored business data | 1 fictional case, 1 order, 1 activity entry, successful Flyway migration |
| Restored identity data | `master` and `case-platform` realms and web client present |

Proxy route checks used the committed Caddyfile with HTTP-only loopback host
substitutions. They did not issue public certificates or prove real DNS/TLS.
Backup checks prove dump restoration and database contents, not a complete
host-loss recovery or restored-user browser sign-in. Backups remain private and
local; off-host encrypted backup scheduling is not configured.

Before launch, follow the [deployment runbook](deployment.md): choose host/domains,
scan and pin image digests, provision real accounts, configure Lovable, test public
TLS and browser workflows, verify full recovery, and enable operational alerts.
CI now includes release-input tests and an API container build; a local pass is
not a claim that the remote CI run has finished.
