# universal-clean-code-skill

> Deterministically inspect local Java/Spring, Vue, JavaScript/TypeScript, and Python repositories for Clean Code and framework style violations, then verify with the repository's own tools.

## Purpose

Use this skill for explicit clean-code reviews and narrowly scoped refactors. It detects the actual build and formatter toolchain instead of assuming npm or Gradle, emits a JSON report, and keeps source edits separate from formatting and verification.

## Activation

Invoke `/universal-clean-code-skill` or ask explicitly to review/refactor a local project for Google Style, Spring constructor injection, Controller/Service decoupling, Vue Priority A/B style, function length, nesting, or deterministic formatting.

## Usage

**Run** from the skill directory:

```bash
python3 scripts/run_pipeline.py --project <repository> --output clean-code-report.json
```

Review the report before changing code. The scanner is read-only. Apply changes only after the user requests them, then run the detected formatter and verification commands.

## Gotchas

- The supplied dental project is Maven-only and has no npm/Vue SFC tree or formatter plugin; do not run or claim npm, Prettier, Spotless, or Checkstyle without configuration.
- Exclude generated `target/`, dependency, and virtual-environment directories.
- A clean report means no scanner findings, not proof that behavior is correct; tests remain required after edits.

## Implementation

Full rules, toolchain detection, and scripts are in `SKILL.md` and the accompanying `references/` directory.

## Files

- `SKILL.md` — full skill definition and activation rules
- `scripts/run_pipeline.py` — **Run** the deterministic source scanner and report writer
- `scripts/run_evals.py` — **Run** bundled eval checks
- `scripts/evolve.py` — **Run** maintenance and record corrections
- `references/` — **Read** detailed rules and troubleshooting
- `evals/` — golden inputs and binary criteria
- `install.sh` — cross-platform installer
