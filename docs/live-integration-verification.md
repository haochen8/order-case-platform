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

## Follow-up edge-case verification — 2026-09-29

Verified frontend commit `51b2918` against backend `654eb6d` in the real local
browser. No application code changes were needed. These checks extend the first
run above; they do not substitute mocked authentication for Keycloak.

### Renewal and expiry recovery

Temporarily set the local web client's access-token lifetime to 90 seconds and
enabled Keycloak login/logout/refresh event recording. The frontend's existing
renewal threshold remained 60 seconds before expiry. Keycloak recorded repeated
successful REFRESH_TOKEN events for case-platform-web while the page and drafts
remained mounted; subsequent real API reads/writes succeeded without another login.

Revoked the fictional operator sessions using the local admin API. Keycloak then
recorded REFRESH_TOKEN_ERROR with invalid_token. The application showed its expiry
warning while keeping the open edit draft. Clicking Save returned the session-expired
message and kept the input. Signing in again returned to the same case; its title
and version were unchanged by the expired save attempt, and its audit list contained
no extra write. The draft is intentionally not persisted across the login redirect:
the banner tells the user to copy edits before signing in.

Selected Keycloak event evidence (Unix milliseconds, no tokens recorded): successful
refreshes at 1790688769769 and 1790688799819; failed refreshes after revocation at
1790688823795 and 1790688829866. Later logins and successful refreshes confirmed
recovery. Original token lifetime and event configuration were restored after testing.

### Concurrent edits

Opened the same case in two independently authenticated tabs. Both began at version
zero. Tab B saved a new title; tab A's stale save received “Case changed; reload it
before retrying” and retained its draft. Reload showed Tab B's current title next to
Tab A's draft without saving. Only a subsequent explicit Save applied Tab A's title
and advanced the version to two. The audit history contained the creation and exactly
the two successful edits.

### Mobile live mode

Verified the actual browser viewport was 390×844, rather than relying on an attempted
resize. Inspected the edit and closure dialogs; labels, fields and buttons were
reachable. Created MOBILE_EDGE_CHECK, sent and completed it, then closed its case.
The order table scrolls horizontally, while the page itself did not overflow the
viewport. Signed out through Keycloak, signed in as Viewer, and navigated by keyboard
to the persisted case: no Edit details or New order controls were present.

The fictional case `d4e9db0b-6402-4b4c-a7e2-ebf0ac70b0a9` remains closed in the local
database with one completed order and seven audit events. Tests did not delete data.

Evidence: [expiry preserves draft](evidence/session-expiry-draft.png),
[390px edit dialog](evidence/mobile-live-edit.png), and
[completed mobile workflow](evidence/mobile-live-completed.png).

No source changes were necessary, so the previously verified 69 frontend tests and
32 backend tests were not rerun for this documentation-only follow-up. Browser
checks above are newly executed evidence. Temporary servers were stopped and the
database volume retained.

## Remaining release checks

The requested local renewal, expiry, concurrency and mobile checks are complete.
No container-image deployment, public hosting, backup/recovery test or production
identity configuration was verified. Local development accounts are not suitable
for unrestricted public deployment.
