# Agent contract

Read `TASK.md`, `REQUIREMENT.md` and `README.md` before editing.

- Implement the requirement in this workspace with a new Flyway migration; never edit released migrations `V1__*`/`V2__*`.
- Keep every v1 endpoint backward compatible as specified. Do not edit `pom.xml`, `.mvn/`, `TASK.md`, `REQUIREMENT.md`, `AGENTS.md`.
- Build and test offline only (`mvn -o ...`). No network, extra dependencies, binary artifacts, or other models/agents unless the operator's frozen protocol permits it equally for all candidates.
- Do not inspect any parent/sibling workspace, hidden tests, reference solution, judge files or other solutions. The operator enforces isolation; this file is not a sandbox.
- Existing tests must keep passing; add your own tests, including a migration test on pre-existing data.
- Write `CHANGES.md` truthfully. Do not claim tests you did not run.
