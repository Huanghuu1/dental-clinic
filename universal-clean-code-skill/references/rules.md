# Clean-code rule matrix

## Common baseline

- Prefer guard clauses over nested `if/else`; target no more than two control-flow levels.
- Keep functions to 25 physical lines or fewer where practical. Extract cohesive helpers rather than splitting arbitrary lines.
- Separate logical blocks with one blank line. Break fluent chains one method call per line after the receiver.
- Preserve behavior, public contracts, identity fields, authorization boundaries, and user changes.

## Java/Spring

- Controller: HTTP mapping, binding, validation, authorization boundary, response status.
- Service: domain decisions, transactions, state transitions, cross-repository orchestration.
- Repository: persistence query abstraction; no business workflow.
- Dependencies: one constructor, `private final` fields; no field injection.
- Concurrency: preserve optimistic locking and tests around invariants such as quota or unique appointment slots.

## Vue Priority A/B

- SFC block order: script, template, style.
- Multi-attribute elements: one attribute per line.
- Stable keys, explicit props/events, and computed values for derived state.
- Do not mix a broad formatting-only diff with an unrelated behavioral rewrite.

## Severity

`error` means likely correctness or architectural regression; `warning` means a concrete maintainability violation; `info` means a consistency improvement. Scanner output is advisory and must be reviewed against project conventions.
