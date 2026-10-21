Run the adversarial review on the current branch against `main`, then drive every finding to a resolution.

1. Run `git diff main...HEAD --stat` and pass the changed files, the milestone from `specs/build-plan.md`, and the branch name to the `adversarial-reviewer` subagent. Tell it which specs the milestone lists. Do not tell it what you think of the change.
2. When the findings come back, handle each one in order of severity:
   - **Fix it** in a commit that names the finding, and add or extend a test that would have caught it.
   - **Decline it** only with a reason that cites the spec; if the spec is the thing that is wrong, fix the spec and the code together (CLAUDE.md rule).
   - Never silently drop a finding.
3. Append the findings and their resolutions to the PR description under a `## Review` heading: one line per finding, `fixed in <sha>` or `declined: <reason>`.
4. Re-run the reviewer on the fixes if any finding was a blocker.
5. Only when every finding has a resolution and the milestone gate still passes, report that the branch is ready to merge. The merge itself is the user's call.
