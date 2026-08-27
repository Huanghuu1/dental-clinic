---
name: universal-clean-code-skill
description: >-
  Review and refactor local Java/Spring Boot, Vue, JavaScript/TypeScript, and Python projects for Google Style, Clean Code, Spring official and Alibaba Java guidelines, and Vue Priority A/B style. Use when users ask to clean up code, enforce constructor injection, decouple controllers and services, reduce function length or nesting, normalize Vue SFC structure, detect Maven/npm/Prettier/Spotless/Ruff toolchains, or run deterministic formatters and linters after changes. Produces a dry-run findings report by default and can guide narrowly scoped fixes without overwriting unrelated work.
license: MIT
metadata:
  author: Huanghuu1
  version: 1.0.0
  created: 2026-08-25
  last_reviewed: 2026-08-25
  review_interval_days: 90
  dependencies:
    - name: Python standard library
      type: runtime
      version: '>=3.10'
  schema_expectations:
    report: JSON object with toolchain, files_scanned, findings, formatter_plan, and verification_plan
activation: /universal-clean-code-skill
provenance:
  maintainer: Huanghuu1
  version: 1.0.0
  created: 2026-08-25
---
# /universal-clean-code-skill — Universal Clean Code

## When to use

Invoke explicitly with `/universal-clean-code-skill` when reviewing or refactoring a local repository. It covers Java/Spring Boot, Vue SFCs, JavaScript/TypeScript, and Python. Do **not** activate for general questions about coding style without a repository or explicit invocation.

## Prerequisites

- Python 3.10+ for the deterministic scanner.
- Read access to the repository; write access only when the user explicitly requests refactoring.
- Maven, npm/pnpm/yarn, or Python tooling is optional and detected from repository files.
- No network or credentials are required by the scanner.

## Core workflow

1. **Inventory first.** Run the single happy-path command below from the target repository. It detects Maven, Gradle, npm-family, Prettier, Spotless, Checkstyle, PMD, Ruff, Black, and pytest configuration without assuming a stack.
2. **Review the report.** Group findings by file and severity. Never rewrite generated files, `target/`, `node_modules/`, virtual environments, or unrelated user changes.
3. **Refactor in small batches.** Keep functions at or below 25 physical lines where practical, use guard clauses, keep nesting at two levels or less, and preserve one blank line between logical blocks. For chained calls, put one method call per line after the receiver.
4. **Apply framework rules.** Spring controllers should translate HTTP concerns only; services own business logic. Use constructor injection and `final` dependencies. Vue SFCs use `<script>`/`<template>`/`<style>` order, and multi-attribute elements put one attribute per line.
5. **Format deterministically.** Run only tools discovered by the scanner, in the order recorded in `formatter_plan`. Do not claim formatting passed unless the command exits zero.
6. **Verify.** Run the discovered build/lint/test commands. Report failures verbatim and distinguish a clean dry-run from an applied refactor.

**Run** the deterministic scanner and report writer:

```bash
python3 scripts/run_pipeline.py --project <repository> --output clean-code-report.json
```

On Windows, use `python` if `python3` is unavailable. The command is read-only and produces an inspectable JSON report.

## Rules by language

### Java and Spring

- Keep controllers thin: request mapping, validation/binding, authorization boundary, response mapping. Move decisions, persistence, transactions, and domain orchestration to services.
- Prefer one public service operation per use case; inject dependencies through a single constructor. Do not use field injection or instantiate services in controllers.
- Add `@Transactional` at the service boundary for multi-repository mutations. Keep repositories free of business orchestration.
- Prefer immutable DTOs and explicit response types. Avoid leaking entities from public APIs when a DTO is practical.
- Use guard clauses for invalid or absent state. Avoid more than two nested control-flow levels.

### Vue, JavaScript, and TypeScript

- SFC order is `<script setup>` (or `<script>`), then `<template>`, then `<style>`.
- Put one attribute per line when an element has multiple attributes; keep event handlers and computed state named for intent.
- Prefer computed values over duplicated template expressions, stable `:key` values, and explicit prop/event contracts.
- Do not mix formatting changes with behavior changes unless the user requested both.

### Python

- Prefer small typed functions, guard clauses, explicit exceptions, and standard formatter/linter configuration already present in the repository.
- Use Ruff/Black/isort only when configured or when the user explicitly asks to introduce them; do not add a dependency silently.

## Validation and safety

- The scanner reports findings; it does not modify source files.
- Before any write, inspect the target and preserve existing behavior and uncommitted changes.
- A formatter is not a semantic refactor. Run tests after formatting and again after behavior changes.
- If no supported toolchain is detected, report that fact and provide manual verification steps rather than inventing a passing command.

## Gotchas

- The supplied project is a Maven Spring Boot backend (`springboot-dental-backend/pom.xml`) and currently contains no Vue SFC or npm manifest in the scanned tree; Vue rules remain available for other repositories but should not be reported as applied here.
- Maven is available in the current Windows environment, but formatter plugins are not declared in the supplied `pom.xml`; do not claim Spotless or Checkstyle ran unless a future project configuration adds them.
- `target/` contains compiled artifacts and must be excluded from source findings.

## Keywords for detection

**Stacks:** Spring Boot, Java, Maven, Gradle, Vue, Vue.js, SFC, JavaScript, TypeScript, Python, pyproject.toml.

**Rules:** Google Style, Clean Code, Alibaba Java, constructor injection, field injection, controller service decoupling, guard clause, nesting, 25 lines, Prettier, Spotless, Checkstyle, PMD, Ruff, Black.

**Actions:** clean, refactor, review, format, lint, standardize, simplify, decouple, verify, fix style.

## Examples

1. `/universal-clean-code-skill` against a Spring repository: scan controllers and services, flag field injection and business logic in controllers, then run the configured Maven verification.
2. `/universal-clean-code-skill` against a Vue repository: detect SFCs, flag tag ordering and multi-attribute formatting, then run the configured Prettier/ESLint command.
3. `/universal-clean-code-skill` against a Python repository: detect `pyproject.toml`, use configured Ruff/Black, and report functions exceeding the local limit.
4. “Review this project but do not edit it”: run the scanner only and return the JSON report.
5. “Apply the safe fixes and verify”: scan, show the planned files, apply only requested changes, format with discovered tools, then run tests.

## References

| When to read | File |
|---|---|
| Read for the full rule matrix and severity policy | `references/rules.md` |
| Read before choosing formatter/linter commands | `references/toolchain.md` |
| Read when applying Spring or Vue refactors | `references/refactoring-playbook.md` |
| Read when diagnosing a failed verification command | `references/troubleshooting.md` |
