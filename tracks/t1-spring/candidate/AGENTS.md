# Agent contract

Read `TASK.md` before editing. Implement the actual service in this workspace.

- Spring Boot 3.5.16 / Java 21 with exactly the dependencies in the frozen `pom.xml`. Do not edit `pom.xml`, `TASK.md`, `AGENTS.md`, or `.mvn/`.
- Build offline only (`.mvn/maven.config` already passes `-o` and the local repository). Do not use the network, external services, extra dependencies, binary artifacts, or another model/agent unless the operator's frozen protocol explicitly permits delegation equally for all candidates.
- Do not inspect any parent/sibling workspace, the Maven repository beyond what the build resolves, any reference implementation, judge, hidden tests, calibration output, or other model's solution. The operator must enforce filesystem isolation; this file is not a sandbox.
- Keep the schema in Flyway migrations under `src/main/resources/db/migration`; never let Hibernate create or alter tables.
- Contract tests live in `src/test/java/contract/`, use only JDK + JUnit 5 + Jackson, and treat the server strictly as an HTTP black box at `BENCH_BASE_URL`.
- Use only the documented `__test__/advance` control; never infer or branch on evaluation case IDs, hidden seeds, process names, or mutation labels.
- Keep a truthful implementation summary. Do not fabricate test results, coverage, benchmark numbers, token counts, or production guarantees.
- User-facing explanation and project docs may be Chinese; code identifiers should be clear and consistent.
- Finish by leaving runnable source and tests on disk, not merely describing a patch.
