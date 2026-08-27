# Tutorial

1. Install the package with `./install.sh --dry-run`, review the destination, then rerun without `--dry-run`.
2. In a target repository, run `python3 scripts/run_pipeline.py --project . --output clean-code-report.json`.
3. Read `toolchain.formatter_plan` and `toolchain.verification_plan` before executing anything.
4. Ask for a narrowly scoped source refactor if findings are actionable.
5. After edits, format, compile/lint, test, and inspect the final diff.

The scanner never changes source files and requires no credentials.
