# Lovable milestone: case and order actions

Paste the text below into the existing Lovable project.

---

Continue from latest GitHub main, including commit 0e4fad3, which aligns activity summaries and mock histories with Spring. Preserve those changes and the 28 passing tests.

Build operator actions on the existing case-detail page: edit case title/description, advance case status, create a provisioning order, and advance order status. Keep the current design, queue navigation, independent section paging, centralized API adapter and isolated mocks. Only the simulated Operator sees write actions. Keep the development-role and mock-data disclosures visible. Real login, live integration, deletion and deployment remain outside this milestone.

## Exact contract

- PUT /api/cases/{id}: current version plus changed title, description and/or status. Returns updated case with new version. Omitted/null fields are unchanged; send an empty string to clear description. Trim titles; reject blank or over 255 characters. Preserve the description limit.
- Case transitions: OPEN → IN_PROGRESS or CLOSED; IN_PROGRESS → CLOSED. CLOSED is terminal, but title/description remain editable with a version. Closing requires no PENDING or SENT orders. Never infer that there are no active orders from just the displayed page; the backend is authoritative and can reject closure with 409.
- POST /api/orders: {caseId, type}. Type is a nonblank string of at most 255 characters, not an enum. New orders start PENDING and require an existing, non-closed case.
- PUT /api/orders/{id}: {version, status}. PENDING → SENT or FAILED; SENT → COMPLETED or FAILED. COMPLETED/FAILED are terminal. Repeating the current status is a no-op. Orders under closed cases cannot change. No delete or retry endpoint exists; retrying failed work means creating a new order while the case is active.
- Versions are required for updates. On 409, show the server message, retain the draft and offer explicit reload/review. Never overwrite a version and resubmit automatically; require user review and explicit submission after reload.
- Disable duplicate submission and automatic mutation retries. A network failure may follow a successful write: explain this, keep the draft, and offer refresh before the user decides whether to retry.
- Distinguish 400, 401, 403, 404, 409, network and server failures. Safely handle non-JSON errors too.

## Interaction and mock behavior

Use accessible forms with labels, keyboard operation and clear pending/error states. Confirm case closure because it is terminal. After success refresh relevant case, queue, orders and activity queries. Use returned data instead of inventing success. Reset the order list to page zero after creation so the new order is discoverable. Activity remains oldest first, so new events may be on a later page.

Extend only the isolated development mock adapter. Enforce versions, transitions and closure rules across all mock orders. Keep audit shapes aligned with Spring:

- CASE_CREATED: {after: {title, description, status}}.
- CASE_UPDATED: {before: {title, description, status}, after: {title, description, status}}.
- ORDER_CREATED: {type, status}.
- ORDER_UPDATED: {previousStatus, status}.

Do not emit audit events for no-op changes. Actors remain explicitly fictional; data resets on reload. Mock checks support UI development and never replace Spring security or transactional rules.

## Acceptance

Add bounded, deterministic tests for successful edits and order progression, Viewer restrictions, terminal statuses, closure blocked by an active order outside the displayed page, conflicts retaining drafts, explicit reload/review, refreshed lists/history, and ambiguous failures without duplicates or retries. Retain existing tests.

Run all tests, typecheck, lint and build. Check dialogs/errors/actions at 390px and desktop widths. Report exact results and separate automated coverage from manual browser checks. Stop after this milestone; do not claim real authentication, persistence or deployment is complete.
