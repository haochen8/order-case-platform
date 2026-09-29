# Delivery roadmap

## 1. Backend foundation

Implemented and verified locally; see [verification results](verification.md).

Acceptance: authenticated read/write roles; persistent transactional audit;
foreign keys; stale-write rejection; complete case/order lifecycle; repeatable
local infrastructure; fast and PostgreSQL verification; executable JAR; documented
contract and architecture. Verification results must be recorded after execution.

## 2. Frontend and demo

Queue, case details, orders, history and operator actions are implemented against
isolated mocks and reviewed. Core OIDC login and the full operator workflow now
pass local live verification; see [results and remaining checks](live-integration-verification.md).
Renewal/expiry, two-tab conflicts and 390px live-mode checks also pass.
Current milestone: deployment preparation and a controlled reviewer demo.

- Lovable frontend in its own Git-synced repository, using the API contract.
- Case queue, detail, order progression, and history screens.
- OIDC PKCE login, role-aware controls, error/conflict states, responsive layout,
  keyboard navigation and accessible form labels.
- Deterministic fictional demo data and a safe reset process.
- Browser acceptance test: login → create case → create order → send → complete
  → close → inspect actor/history; viewer cannot modify data.

Acceptance: the complete workflow works in a real browser against PostgreSQL;
no mock-only success path. Keep part of the Lovable credit budget for integration
and accessibility fixes rather than spending it all on visual generation.

## 3. Public portfolio release

- Hosted backend/database/identity provider and published frontend with HTTPS.
- Environment configuration, dependency/container scans, migration deployment
  process, health checks, backup/recovery notes, and smoke checks.
- Seeded reviewer experience; avoid an unrestricted public write account against
  shared persistent data.
- Screenshots, short demo recording, and a README that links both repositories
  and the live demo and explains measured engineering tradeoffs.
- Resume claims based only on implemented, tested behavior and real measurements.

## Skills and agent workflow

Custom SKILL.md files are not required. AGENTS.md holds repository conventions;
this roadmap and the API contract hold project decisions and acceptance criteria.
A reusable skill should describe a repeatable workflow, not duplicate the backlog.

Matt Pocock's skill collection is a candidate for targeted TDD and review use:
https://github.com/mattpocock/skills
There are multiple pstack forks; choose the exact source and inspect its tool,
model, and subagent assumptions before adoption. One cross-agent mirror is:
https://github.com/backnotprop/pstack
No third-party skill pack has been installed as part of this milestone.

For future parallel work: assign non-overlapping backend/frontend/delivery tasks,
fix shared API decisions first, and integrate through reviewed changes with
checks. A skill installation is not a substitute for these boundaries.
