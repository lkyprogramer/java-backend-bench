# Agent contract

Read `TASK.md`, `ISSUES.md` and `README.md` before editing.

- Fix the reported issues at their root cause in this workspace; keep the public HTTP API unchanged.
- Do not edit `pom.xml`, `.mvn/`, `TASK.md`, `ISSUES.md`, `AGENTS.md`, or released migrations `V1__*`/`V2__*`.
- Build and test offline only (`mvn -o ...`). Do not use the network, extra dependencies, binary artifacts, or another model/agent unless the operator's frozen protocol permits it equally for all candidates.
- Do not inspect any parent/sibling workspace, hidden tests, reference fixes, judge files or other solutions. The operator enforces isolation; this file is not a sandbox.
- Keep changes focused; do not reformat or rewrite unrelated code. Existing tests must keep passing; you may add tests.
- Write `FIXES.md` truthfully: root cause, change, verification per issue. Do not claim tests you did not run.
