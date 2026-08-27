# Decision record

## 1.0.0 — 2026-08-25

- Chose a simple skill because the workflows share one read-only repository scan and one report format.
- Used Python standard library only so the skill is portable and does not require network access or package installation.
- Made `run_pipeline.py` the sole deterministic entry point: toolchain detection and source scanning are wired in code.
- Kept refactoring instructions separate from the scanner so the default action cannot overwrite user code.
- Encoded current-project gotchas: Maven-only backend, no configured formatter plugins, bundled static Vue rather than SFCs, and generated `target/` output.
