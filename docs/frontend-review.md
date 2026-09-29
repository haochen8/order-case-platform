# Lovable frontend review — 2026-09-29

Repository: https://github.com/haochen8/frontend-order-case-platform

Reviewed main at `1686f60b4a641e47f712ed6e06967b1b430748ae` in a disposable local clone. No frontend source changes or pushes were made.

## Findings

- **P2 — Browser navigation is overwritten by stale search input.** `src/routes/index.tsx:46–54` initializes local text once and writes it back whenever q changes. Browser reproduction: search fiber, filter Open, search router, then Back. The URL restored fiber briefly, while the textbox stayed router; the debounce then restored router in the URL and results. Synchronize navigation and input without allowing obsolete debounce work to overwrite history.
- **P2 — An invalid page reports an empty dataset instead of recovering.** `src/routes/index.tsx:138–147` renders pagination without reconciling the requested page with the resolved total. Opening `/?page=99` displayed “47 cases”, “No cases yet” and “Page 100 of 3”. Normalize from the resolved current query, including zero-result handling. The controls also rely on previous data during transitions; guard this and add a regression test. Rapid-click overshoot was not independently reproduced.
- **P2 — Mobile removes the mock disclosure and role controls.** `src/components/app-shell.tsx:27–42` hides the whole development controls container below md. Confirmed at 390×844: the mock-data label, development warning and role selector disappear while New case remains available under the default operator role. Preserve those controls and disclosures on small screens.

## Verification

- Frozen Bun lockfile dependency installation succeeded with lifecycle scripts disabled.
- `bun --bun run build`: passed (client and server build).
- `bun node_modules/typescript/bin/tsc --noEmit`: passed.
- ESLint: failed with 62 `prettier/prettier` errors and eight `react-refresh/only-export-components` warnings.
- No application test suite or test script was present.
- Reviewed the local running queue in the in-app browser, including search-history navigation, an invalid page and a mobile viewport.
- Frontend Git working tree remained clean after checks.

## Assessment and next step

The first-stage architecture follows the brief: centralized API calls, isolated mock data, matching contract types, and no replacement backend. The desktop queue is visually coherent. Deferred case details, orders and real authentication are intentional scope boundaries, not review findings. Live API integration and a deployed production runtime have not been verified.

The README still describes an unrelated “GitHub Connect Hub” project and should be corrected before portfolio use. Fix the queue issues and add focused regression coverage before expanding feature scope. The ready-to-paste request is in [lovable-queue-fixes.md](lovable-queue-fixes.md).

## Follow-up review — queue fixes

Reviewed the subsequent main revision `f515d93` on 2026-09-29. The source addresses the search/history, invalid-page and mobile-disclosure findings, and the README now describes the project. This follow-up used source inspection and automated checks; the mobile browser check reported by Lovable was not independently repeated.

- Production build passed.
- All 10 tests passed under the bundled native Node runtime. Forcing Vitest to run under Bun failed during jsdom initialization; the host's x64 Node also failed during bundler startup. Native Node successfully ran the suite without source changes.
- Direct `tsc --noEmit` passed. **The declared `typecheck` script fails:** it invokes `tsgo`, which is not supplied by a declared dependency. Change it to the already installed `tsc --noEmit`, or declare the intended compiler.
- ESLint completed with zero errors and exactly seven warnings: six in stock UI components and one in the new `case-queue.tsx`. The claim that all seven are stock component warnings is incorrect.
- The pagination regression test contains a swallowed waitFor failure and unbounded while loops; tighten it to make failures clear and bounded.
- No frontend source was changed or pushed during review.

Proceed with read-only case details, linked orders and activity, including the small verification fixes above. The next pasteable request is [lovable-case-detail.md](lovable-case-detail.md). Real authentication, persistence through the API and deployment remain unverified and deferred.

## Case-detail review and integration correction

Reviewed Lovable main `724e792` on 2026-09-29. Independently reran its 22 tests, TypeScript, lint and production build: all passed, with six stock UI lint warnings. The typecheck script now correctly uses the declared TypeScript compiler. This review used source inspection and automated checks; Lovable's reported mobile/browser checks were not repeated.

Found an integration mismatch: summaries and fixtures used CASE_STATUS_CHANGED/ORDER_STATUS_CHANGED with from/to fields. Spring emits CASE_UPDATED with before/after snapshots and ORDER_UPDATED with previousStatus/status. CASE_CREATED also nests fields under after. Real API data would therefore lose useful title/transition descriptions. Fixtures additionally skipped SENT before COMPLETED and allowed closed cases with active orders.

Corrected these in frontend commit `0e4fad3` and pushed to main. Added coverage for real audit payloads and fixture replay checks for legal transitions, versions and case closure. Case 1 now has 48 events, still three pages. After the fix: **28 tests passed, TypeScript passed, production build passed, lint zero errors/six existing UI warnings**. Vitest ran under native Node and the build used Bun. No deployed environment was tested.

Next milestone: [case and order actions](lovable-workflow-actions.md), with conflict handling and mock workflow coverage. Authentication and real integration follow that milestone.

## Operator-action review

Reviewed Lovable main `756a190` on 2026-09-29. All 44 reported tests, TypeScript and the production build passed independently. Lint reported zero errors and six existing stock UI warnings. API methods and transition controls match the intended operator workflow; mock mode remains a development simulation.

Found and reproduced a draft-loss bug: after a version conflict, a failed “Reload latest case” refetch replaced the case body with the full error state and unmounted the edit dialog. React Query can also return cached data alongside the refetch error, which must not be treated as a newly reviewed version.

Fixed the page to retain loaded content and drafts on refetch failure, visibly disclose stale data, and accept a refreshed version only on success. Failed reloads keep the error/retry path instead of clearing it. Added a regression test that fails before the fix and verifies draft retention, no automatic write, and a subsequent successful reload.

Final checks: **45 tests passed across seven files, TypeScript passed, production build passed, lint zero errors/six existing warnings, diff whitespace check passed**. No independent browser/mobile session was run for this review. Real login, live API persistence and deployment are not yet verified.

Next handoff: [real login and API integration readiness](lovable-auth-integration.md). The hosted preview cannot establish local Spring/Keycloak acceptance; that will be tested locally after the authentication code is ready.
