# Agent contract

Read `TASK.md` before editing. Implement the actual service in this workspace.

- Use Java source compatible with `javac --release 17`; only JDK classes and the supplied frozen benchkit.
- Do not edit `src/benchkit/`, `TASK.md`, `AGENTS.md`, or the public smoke test.
- Do not use the network, external services, dependencies, binary artifacts, or another model/agent unless the operator's frozen protocol explicitly permits delegation equally for all candidates.
- Do not inspect any parent/sibling workspace, reference implementation, judge, hidden tests, calibration output, or other model's solution. The operator must enforce filesystem isolation; this file is not a sandbox.
- Build in small steps, write meaningful tests, and preserve stock/state/idempotency invariants.
- Test code is self-contained in `tests/candidate_tests.py`, accepts `--base-url`, and treats the server strictly as an HTTP black box.
- Use only the documented `__test__/advance` control; never infer or branch on evaluation case IDs, hidden seeds, process names, or mutation labels.
- Keep a truthful implementation summary. Do not fabricate test results, coverage, benchmark numbers, token counts, or production guarantees.
- User-facing explanation and project docs may be Chinese; code identifiers should be clear and consistent.
- Finish by leaving runnable source and tests on disk, not merely describing a patch.
