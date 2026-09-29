# Deployment runbook

Status: release preparation, not a public deployment. The frontend remains in
Lovable; this is an optional portable single-host backend/identity setup. A host,
DNS names and operating budget have not been selected. Use fictional demo data.
See the [local verification record](deployment-verification.md) for checks and limits.

## Layout and prerequisites

Use `deploy/compose.yml` by itself, never merged with the root development file.
Caddy terminates HTTPS for separate API and identity DNS names. Only ports 80/443
are published. Business and identity data use separate PostgreSQL databases and
persistent volumes. The API runs as a non-root user with a read-only filesystem.
Keycloak uses production `start`, PostgreSQL, and a generated realm without demo
users or the local password-grant client. Admin/master and management paths are
not exposed through Caddy. Internal HTTP assumes a trusted single-host Docker
network; this is not a highly available deployment.

Start with a Linux host with Docker Compose, at least 4 GB RAM and adequate disk
for databases, images and backups; measure usage before increasing load. Restrict
SSH to administrators, allow inbound 80/443, and do not publish database ports.
Point API and identity DNS A/AAAA records at that host (remove unusable AAAA
records). Know the final HTTPS frontend origin before generating inputs.

## Prepare a release

From a reviewed, tested checkout at the repository root:

```bash
python3 -m unittest discover -s deploy -p 'test_*.py'
cd backend-spring
GRADLE_USER_HOME=../.gradle-home ./gradlew check bootJar
cd ..
RELEASE=$(git rev-parse --short HEAD)
docker build -t order-case-platform-api:$RELEASE backend-spring
python3 deploy/prepare.py \
  --api-host api.example.com --auth-host auth.example.com \
  --frontend-origin https://your-project.lovable.app \
  --email you@example.com --api-image order-case-platform-api:$RELEASE
docker compose --env-file deploy/.env -f deploy/compose.yml config --quiet
```

Replace the example values. Build on the target architecture or publish a tested
multi-platform image to your registry. The generator creates private, ignored
`deploy/.env` and `deploy/generated/case-platform-realm.json`, refuses to overwrite
existing inputs, and prints no secrets. Keep an encrypted off-host copy of these
inputs; never paste the `.env` into Lovable or commit it. An earlier local dry run
may have left ignored inputs using `.example.test`: move those aside and generate
fresh inputs for the actual host. Do not reuse dry-run databases or credentials.
Docker administrators can inspect container environment secrets; restrict host
and Docker access accordingly.

Scan the API and infrastructure images before public release, review findings,
and record/pin the exact tested image digests in your release configuration.
PostgreSQL 16 and Caddy 2 tags intentionally allow patch updates during preparation;
resolve them to digests for a reproducible deployed release. Record source commit,
image digests, deployment date, schema version and backup location.

## Start and provision identity

```bash
docker compose --env-file deploy/.env -f deploy/compose.yml up -d
docker compose --env-file deploy/.env -f deploy/compose.yml ps
```

Allow time for first Keycloak initialization and certificate issuance. Check logs
locally without publishing credentials or tokens. Provision accounts through a
private administration session on the host, for example Keycloak's `kcadm.sh`
inside the identity container against `http://localhost:8080`, realm `master`.
It can prompt for the bootstrap administrator password from the private `.env`.
Do not expose the admin console publicly just to create users. Establish a named
administrator, remove the temporary bootstrap administrator, and maintain a tested
recovery procedure. Bootstrap environment variables are for first initialization;
changing their value later does not rotate an existing user's password.

Create individually attributable users in `case-platform`. Assign realm `VIEWER`
to reviewers and `OPERATOR` only to trusted operators. Registration is disabled;
no public write account is supplied. Configure recovery/email deliberately before
enabling password reset. Keycloak skips realm import if the realm already exists:
subsequent client/role changes must be applied to the existing realm, not merely
edited in the JSON. Preserve identity data and signing keys in backups.

## Configure Lovable

Set these public build-time settings, then rebuild/republish the frontend:

```dotenv
VITE_USE_MOCKS=false
VITE_API_BASE_URL=https://api.example.com
VITE_OIDC_ISSUER=https://auth.example.com/realms/case-platform
VITE_OIDC_CLIENT_ID=case-platform-web
```

No client secret belongs in the browser. The generated client allows only the
exact `/auth/callback` redirect, frontend origin and `/` logout redirect. A domain
change requires updating the existing identity client, API CORS and frontend build
together. Preview origins are not automatically trusted. Ensure direct routes such
as `/cases/{id}` and `/auth/callback` work after a full reload on the frontend host.

## Public release gates

- Check HTTPS chains and redirects for both DNS names; Caddy needs reachable
  ports 80/443 and working DNS for automatic certificates.
- `/actuator/health` returns aggregate UP only; unauthenticated `/api/cases`
  returns 401. Untrusted CORS origins receive no allow-origin header.
- `/admin/`, `/realms/master/`, `/metrics` and API documentation are unavailable
  through public ingress. OIDC discovery advertises the public HTTPS issuer.
- Sign in as Viewer and Operator through Lovable; verify role restrictions,
  creation/editing, conflict handling, order progression, closure and activity.
  Repeat refresh, expired/revoked session, logout, callback and mobile checks.
- Restart containers and verify business records and users survive. Test backup
  restore before launch. Set up host/disk monitoring, HTTPS/health alerts, backup
  scheduling and an operator responsible for patching and recovery.
- Decide how fictional portfolio data is seeded and refreshed. Do not offer a
  shared public operator password or erase persisted data as an automatic reset.

These gates require the actual host and domains; local checks do not prove them.

## Backup and recovery

```bash
./deploy/backup.sh
```

This creates permission-restricted PostgreSQL custom-format dumps for **both**
databases under ignored `backups/<UTC timestamp>/`. A `.partial` file indicates an
incomplete dump. Encrypt/copy completed backups off-host and apply a documented
retention policy. Schedule the script only after the destination, encryption and
failure alerting are configured. Dumps run sequentially: use a maintenance window
if you need a coordinated application/identity recovery point. Also preserve the
private release inputs, image digests and Caddy certificate data securely.

Prove recovery in a separate isolated stack with empty databases and matching
PostgreSQL/Keycloak versions. Stop its API and identity service, restore each dump
with `pg_restore --exit-on-error --no-owner` using that database's owner, then start
identity/API. Never run a restore against the active deployment. Verify Flyway
history, case/order/activity counts, user identities, roles and real sign-in;
record restore duration and backup age. Only switch traffic after validation.

Before an upgrade, take and verify backups and review migrations. Flyway runs on
API startup. Rolling back the image is safe only when the schema remains compatible;
otherwise prefer a forward fix, or restore both databases during an explicit
maintenance window, accepting loss of changes after the backup. Do not delete
volumes or use `down -v` as a deployment or rollback step.

## Primary references

- [Keycloak production configuration](https://www.keycloak.org/server/configuration-production)
- [Keycloak reverse proxy](https://www.keycloak.org/server/reverseproxy)
- [Realm import behavior](https://www.keycloak.org/server/importExport)
- [Caddy automatic HTTPS](https://caddyserver.com/docs/automatic-https)
