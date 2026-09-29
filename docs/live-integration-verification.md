# Local live integration — 2026-09-29

Tested Lovable frontend revision a3f2440 plus the authentication review fixes,
against the packaged Spring API, PostgreSQL 16 and Keycloak 26.7.4. Frontend ran
at http://localhost:5173 with VITE_USE_MOCKS=false. This is local verification,
not a public deployment or an enterprise identity-provider integration.

## Passed in the browser

- Operator Authorization Code + PKCE login through the Keycloak page and callback.
- Create a fictional case, create FIBER_INSTALL order, mark sent, mark completed,
  close case, and inspect five audit events attributed to the operator subject.
- Reload the detail URL, sign in through the existing provider session, and see
  the same persisted closed case, completed order and history.
- Sign out, reach the sign-in gate, and require credentials on the next login.
- Sign in as Viewer and inspect persisted details/history without write controls.
- After the review fix, provider logout uses client_id and confirmation without
  id_token_hint in its URL, then returns to the application sign-in screen.

Evidence: [closed case and audit history](evidence/local-live-workflow.png).
The fictional browser case is 08721e77-2219-48bf-bea7-52e8a6a3fbcd. It remains
in the local development database. A separate smoke-test case was also created.

## Corrections and automated verification

The initial authenticated queue request returned 500: Hibernate bound a null
search parameter such that PostgreSQL attempted lower(bytea). Added explicit
string casts in the repository query and API regression coverage for absent,
empty, status-only and populated searches. The shared test runs against both
H2 and real PostgreSQL. The smoke test now reads queue variants with real tokens.

- Backend check and bootJar: passed; 15 fast and 17 PostgreSQL tests, no skips.
- Real-token smoke: passed, including anonymous/invalid-token rejection, Viewer
  write denial, queue reads, order lifecycle, stale edits, closure and audit.
- Frontend: 69 tests passed, TypeScript/build passed, lint zero errors and six
  existing UI warnings. Added redirect-failure/retry coverage for login/logout.
- Frontend sign-in/sign-out redirect failures now show an error rather than an
  unhandled rejection. Logout no longer puts an ID token in the redirect URL.

## Remaining checks

Timed token renewal, live expiry recovery, two-tab browser conflicts and mobile
live-mode layout were not exercised in this run. Conflict and expired-state
behavior have automated coverage, but that does not replace those browser checks.
No container-image deployment, public hosting, backup/recovery test or production
identity configuration was verified. Local development accounts are not suitable
for unrestricted public deployment.
