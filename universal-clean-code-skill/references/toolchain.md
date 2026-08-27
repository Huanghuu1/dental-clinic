# Toolchain detection and deterministic verification

The scanner reads repository manifests before proposing commands.

| Evidence | Commands to prefer |
|---|---|
| `pom.xml` | `mvn verify`; Spotless only if the POM declares it |
| `build.gradle` or `.kts` | `gradle check`; use configured format task only |
| `package.json` | declared `lint`/`test`; Prettier only when configured |
| `pyproject.toml` | Ruff/Black/pytest commands only when declared or present |

Never install a formatter just to make a report green. Never run `npm` in a Maven-only tree. Exclude generated output and dependency directories. A formatter's zero exit code proves formatting command success, not semantic correctness; run tests separately.
