# Lovable milestone: real login and API integration readiness

Paste the text below into the existing Lovable project.

---

Continue from latest GitHub main, preserving the review fix that keeps edit drafts when a conflict reload fails. Build real OIDC login and connect the existing API adapter to bearer authentication. Keep all current screens, workflows, tests and mock mode. Do not create a database, replacement backend, identity service, or server-side business API. Deployment is a later milestone.

## Existing local configuration

- VITE_USE_MOCKS=false selects live mode; true retains the isolated development demo.
- VITE_API_BASE_URL=http://localhost:8080
- VITE_OIDC_ISSUER=http://localhost:9090/realms/case-platform
- VITE_OIDC_CLIENT_ID=case-platform-web (public client, no client secret).
- The frontend runs at http://localhost:5173 locally. Keycloak permits redirects under this origin and the backend permits this origin. Do not replace localhost with 127.0.0.1 or loosen CORS/redirect validation.
- The Spring API requires a valid signed access token with the configured issuer, audience case-platform-api, nonblank subject and top-level roles containing VIEWER or OPERATOR. Keycloak already supplies these claims. An ID token is not an API access token.

## Authentication requirements

Use a maintained OIDC client library with Authorization Code + PKCE S256, state and nonce validation. Declare and lock dependencies. Use provider discovery and a dedicated callback route. Do not implement the cryptographic protocol yourself or use password/direct-grant login. Login must redirect to Keycloak, not collect passwords in this application.

Keep access, refresh and ID tokens in memory. Do not persist them in localStorage, sessionStorage, cookies, URLs or logs. Short-lived redirect transaction state/PKCE verifier may use sessionStorage as required to survive the redirect; remove it after completion. Document the distinction. Avoid browser-only authentication initialization during server rendering, and never share user/session data across server requests.

In live mode, wait for authentication initialization before making protected API requests. Show sign-in, callback progress, callback failure and session-expired states without redirect loops. Preserve an internal return location (including case ID and queue filters) through login; reject external/protocol-relative return URLs. A reload may require a new login redirect to the existing provider session because tokens are memory-only.

Add the bearer access token centrally to all API requests. Coordinate token renewal where supported before expiry; fail to an explicit session-expired state if renewal fails. Never automatically replay a failed mutation after renewal or 401. Preserve drafts for explicit user review/retry. Treat 403 separately from expiration.

Derive live UI permissions from the authenticated roles, defaulting to no access for absent/unrecognized roles. OPERATOR can read/write; VIEWER can read. Remove the simulated role switch in live mode and never let it grant live access. Keep the development role switch and mock-data disclosure in mock mode. Never silently fall back to mocks on authentication/API failure.

Implement logout through the provider's supported flow. Clear tokens and query caches on logout or identity change; prevent requests from the old session repopulating the cache. Ensure no cached records from one user appear for another. Keep bearer credentials restricted to the configured API origin.

## Verification and handoff

Add automated tests for authenticated request headers, missing/expired session behavior, Viewer restrictions, unknown roles, callback/return-path handling, no mutation replay, live-mode isolation from dev roles/mocks, and cache clearing on identity changes. Stub OIDC and API boundaries in unit tests, not the business UI. Retain all existing workflow regression tests.

Run tests, typecheck, lint and build. Update the README and add a non-secret .env.example with exact local settings, setup and callback/logout paths. Record which real-browser authentication scenarios were tested and which were not.

The Spring/Keycloak services currently run only on the developer's computer. A hosted Lovable preview is not evidence of local backend connectivity and is not an allowed redirect origin. Do not add public tunnels or deploy services. If you cannot reach these services, finish the code and automated checks, document the local browser acceptance steps, and explicitly leave live acceptance for our local verification.

Acceptance to document for the local run: operator login → create case → create order → send → complete → close → inspect actor/history; reload confirms PostgreSQL persistence; viewer login cannot mutate; logout removes cached data; stale edits retain drafts. Stop after authentication/integration readiness and report exact results without claiming untested live success.
