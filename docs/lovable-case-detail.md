# Next Lovable milestone

Paste the text below into the existing Lovable project.

---

Continue the Order Case Platform with one bounded milestone: read-only case details, linked provisioning orders and case activity. Keep the current visual design and queue behavior. Keep mocks isolated and all API calls centralized; Spring remains the only business backend. Real login, live integration and new write workflows remain deferred.

First fix the verification setup: package.json currently runs `tsgo --noEmit` but does not declare the package supplying tsgo. Use `tsc --noEmit` with the existing TypeScript dependency, or explicitly declare and lock the chosen compiler. Verify from a clean install. Lint currently reports seven warnings: six in stock UI components and one in src/components/case-queue.tsx. Move that component's shared schema/constants into an appropriate module if needed; do not disable lint rules. In the pagination test, replace the swallowed waitFor failure and open-ended loops with bounded, deterministic checks.

Make each case title a keyboard-accessible link to /cases/{id}. Preserve queue search, status and page when returning to the queue. Direct links and reloads must work.

On the detail screen show the title, description, status with icon and text, ID, UTC created/updated times, and a clear return link. Show linked orders and an activity timeline, each independently paginated with loading, empty and retryable error states. Show a useful case-not-found state for 404, and distinct 401/403 states. Keep mock-data and development-role disclosures visible at mobile and desktop widths. Both roles can read these screens.

Extend the existing API adapter with these exact read operations:

- GET /api/cases/{id}: returns id, title, description (nullable), status, createdAt, updatedAt, version.
- GET /api/orders?caseId={id}&page=0&size=20: Spring Page of orders, ordered by createdAt descending then id. Each order contains id, caseId, type (string), status (PENDING, SENT, COMPLETED or FAILED), createdAt, version. These are service provisioning tasks, not purchases.
- GET /api/cases/{id}/audit?page=0&size=20: Spring Page of both case and order events, oldest first by timestamp then id. Each event contains id, caseId, entityType, entityId, eventType, actor, timestamp and payload (JSON object). Render useful event summaries with safe text rendering and an optional expandable payload; tolerate unfamiliar event types and payload fields.

Spring Page contains content, number (zero-based), size, totalElements, totalPages, first, last, empty and numberOfElements. Timestamps are UTC ISO-8601. Preserve the existing response types and environment configuration. The live API requires bearer authentication later; mocks are the working development mode for this milestone.

Add deterministic mock detail/order/audit fixtures under src/mocks/, with multiple pages, a case without orders, and coherent history. Ensure newly created mock cases can also be opened. Do not fake persistence across reloads or fabricate real authenticated actors. Provide a documented test-only way to exercise error responses without exposing implementation controls as product features.

Add regression tests for opening the correct case, returning to the filtered/paged queue, direct navigation, unknown case IDs, independent order/activity pagination, and empty/error states. Retain all existing tests. Run tests, typecheck, lint and build; inspect the page at 390px and desktop widths. Report exact results and distinguish automated coverage from manual browser checks.

Stop after this read-only milestone. Do not implement case edits, order creation/status changes, deletion, real login, deployment, a new database or a replacement backend yet.
