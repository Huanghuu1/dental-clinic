# Troubleshooting

## No formatter detected

This is expected when a project has no formatter configuration. Report the absence and use the build/test command only; do not invent a command or add a plugin silently.

## Maven project with no Spotless

Run `mvn verify` if Maven is available. A Maven parent alone does not imply Spotless, Checkstyle, PMD, or formatter support.

## Vue rules have no files to inspect

The project may serve a bundled static page or use a framework other than SFCs. Say that no `.vue` files were found and do not claim Vue style compliance.

## Tests fail after formatting

Restore only the formatting change if it caused the failure, capture the test output, and separate a toolchain issue from a semantic refactor issue.

## Generated files appear in the report

Check exclusion directories first. Compiled output, dependency trees, coverage, and virtual environments are not source targets.
