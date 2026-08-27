# universal-clean-code-skill

A read-only, toolchain-aware clean-code scanner and review guide for local Java/Spring Boot, Vue, JavaScript/TypeScript, and Python repositories.

## Install

### Claude Code

```text
/plugin marketplace add ./universal-clean-code-skill
/plugin install universal-clean-code-skill@universal-clean-code-skill
```

### Universal path

```bash
cp -R universal-clean-code-skill ~/.agents/skills/universal-clean-code-skill
```

### Other platforms

Run the bundled installer:

```bash
chmod +x install.sh
./install.sh --dry-run
./install.sh --all
```

It detects native paths for Claude Code, Copilot, Cursor, Windsurf, Cline, Codex, Gemini, Kiro, Goose, OpenCode, Roo Code, Kilo Code, Factory, Junie, Trae, Antigravity, and the universal path. On Windows, use Git Bash or copy the package to the platform's native skills directory.

## Use

From the installed skill directory, run:

```bash
python3 scripts/run_pipeline.py --project /path/to/repository --output clean-code-report.json
```

The scanner is read-only. Review the report before requesting code edits. After an explicit refactor, run the detected formatter and verification commands from the report, then inspect the diff and run tests.

## Development checks

```bash
python3 scripts/run_evals.py --validate
python3 -m py_compile scripts/run_pipeline.py
```

## Correction loop

If a real project reveals a rule the skill missed, record it with:

```bash
python3 scripts/evolve.py --correct "the concrete behavior the skill got wrong"
```

## Scope

The skill does not silently install formatters, access network services, modify source code, or claim that a formatter ran when the repository does not configure one.
