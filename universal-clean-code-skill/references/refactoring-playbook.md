# Refactoring playbook

## Spring boundary extraction

1. Identify controller statements that calculate domain state, mutate multiple entities, reserve/release resources, or start transactions.
2. Move that cohesive operation to a service method with a domain name.
3. Inject the service through the controller constructor and keep the dependency final.
4. Keep authorization and request-to-command mapping at the edge; enforce domain validation again at the service boundary.
5. Preserve response codes and add/retain focused tests, especially concurrent state transitions.

## Guard-clause extraction

Replace a deep success-shaped pyramid with early returns or exceptions for invalid input, missing entities, unauthorized ownership, and unsupported enum values. Extract a helper only when it has one nameable responsibility.

## Vue cleanup

Normalize SFC block order first. Then format multi-attribute tags and extract repeated template expressions to computed state. Keep event handlers thin and move reusable state transitions into composables only when the repository already uses them.

## Safe sequencing

Review -> make one coherent change -> deterministic format -> compile/lint -> tests. If any verification fails, stop and report the exact command and output rather than applying unrelated cleanup.
