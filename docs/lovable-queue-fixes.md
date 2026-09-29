# Lovable follow-up: queue review fixes

Paste the text below into the existing Lovable project.

---

Keep the current Order Case Platform design and first-stage scope. Fix the existing queue before adding case details, orders or real login. Preserve the centralized API adapter, isolated mocks, documented Spring API contract, and explicit development-only role simulation. Do not add a database or replacement backend.

The review of commit 1686f60b4a641e47f712ed6e06967b1b430748ae found these issues:

1. **Search breaks browser history.** In src/routes/index.tsx, local search text is initialized from q once, then the debounce writes stale text back after browser navigation. Reproduction: search for fiber, choose Open, search for router, then press Back. The URL briefly restores fiber, but the textbox remains router and overwrites the restored query. Make URL navigation and the input stay synchronized, cancel obsolete pending search updates, and retain debouncing for user typing. Back and Forward must restore the input, filters, rows and page together without a subsequent overwrite.

2. **Out-of-range pages misrepresent the dataset.** Opening /?page=99 with the 47 mock cases displays “47 cases”, “No cases yet” and “Page 100 of 3”. After the current query has resolved, normalize an out-of-range page to a valid page and replace the URL without a navigation loop. Handle zero results separately. Do not normalize from placeholder data belonging to a previous query. Guard pagination during transitions so repeated clicks cannot move outside valid bounds; verify both first and last page behavior.

3. **Mobile hides the development disclosure and role selector.** The hidden/md:block container in src/components/app-shell.tsx removes “Dev only — not real auth”, the simulated role selector and the mock/API indicator below the desktop breakpoint. Keep those controls and disclosures accessible on mobile, with a compact layout. Verify at 390px and desktop widths. Switching to Viewer must hide New case at both widths.

4. **Verification and project documentation need cleanup.** The build and TypeScript check pass, but lint reports 62 Prettier errors and eight React Refresh warnings. Apply the existing formatting rules, resolve warnings where practical, and do not disable rules merely to make checks pass. Replace the unrelated “GitHub Connect Hub” README with accurate project setup, environment variables, mock limitations, and commands. Clearly state that authentication and backend integration are deferred.

Add focused automated regression tests for the search-history sequence, out-of-range and zero-result pagination, transition bounds, and mobile role/disclosure behavior. Also cover the existing creation safeguards: blank title rejection, one request while submission is pending, and no automatic mutation retry on an ambiguous network failure. Use a test runner appropriate for the existing stack and add a documented test command. Tests should exercise visible behavior rather than mirror implementation details.

Run build, TypeScript, lint and the new tests. Report exact results, including anything that could not be run. Stop after these fixes and provide a concise summary of changed files and verification. Do not claim real authentication, persistence, or deployment is complete.
