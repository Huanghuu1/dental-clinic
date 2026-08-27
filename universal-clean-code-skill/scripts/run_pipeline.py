#!/usr/bin/env python3
"""Deterministic, read-only clean-code scanner for local repositories.

It detects source languages and configured formatters, then emits a JSON report.
It deliberately does not edit source files or invoke network services.
"""
from __future__ import annotations

import argparse
import json
import re
import shutil
import sys
from pathlib import Path
from typing import Any

EXCLUDED = {".git", "target", "node_modules", ".venv", "venv", "dist", "build", "__pycache__"}
SOURCE_SUFFIXES = {".java", ".py", ".js", ".jsx", ".ts", ".tsx", ".vue"}
FUNCTION_RE = re.compile(r"\b(?:public|private|protected|static|async|def|function)\b[^\n{;]*\([^\n]*\)")
INJECTION_RE = re.compile(r"@Autowired|@Inject|\b@Autowired\s+private\b")
NESTING_RE = re.compile(r"\b(if|for|while|switch|try)\s*\(")


def iter_sources(project: Path) -> list[Path]:
    """Return supported source files while excluding generated trees."""
    return sorted(
        path for path in project.rglob("*")
        if path.is_file()
        and path.suffix.lower() in SOURCE_SUFFIXES
        and not EXCLUDED.intersection(path.relative_to(project).parts)
    )


def available(command: str) -> bool:
    """Return whether a command is available without executing it."""
    return shutil.which(command) is not None


def interpreter_name() -> str:
    """Choose a portable Python command for generated eval commands."""
    return sys.executable or "python3"


def detect_toolchain(project: Path) -> dict[str, Any]:
    """Inspect manifests and return available build and formatting commands."""
    names = {path.name for path in project.iterdir()}
    tools: list[str] = []
    verify: list[str] = []
    formatters: list[str] = []
    if "pom.xml" in names:
        tools.append("maven")
        verify.append("mvn verify")
        text = (project / "pom.xml").read_text(encoding="utf-8", errors="ignore")
        if "spotless" in text.lower():
            formatters.append("mvn spotless:apply")
        if "checkstyle" in text.lower():
            verify.append("mvn checkstyle:check")
        if "pmd" in text.lower():
            verify.append("mvn pmd:check")
    if any((project / name).exists() for name in ("build.gradle", "build.gradle.kts")):
        tools.append("gradle")
        verify.append("gradle check")
    package = project / "package.json"
    if package.exists():
        tools.append("npm")
        manifest = json.loads(package.read_text(encoding="utf-8"))
        scripts = manifest.get("scripts", {})
        verify.extend(f"npm run {key}" for key in ("lint", "test") if key in scripts)
        if "prettier" in json.dumps(manifest).lower() or (project / ".prettierrc").exists():
            formatters.append("npx prettier --write .")
    pyproject = project / "pyproject.toml"
    if pyproject.exists():
        tools.append("python")
        text = pyproject.read_text(encoding="utf-8", errors="ignore").lower()
        if "ruff" in text:
            formatters.append("ruff format .")
            verify.append("ruff check .")
        if "black" in text:
            formatters.append("black .")
        if "pytest" in text or (project / "tests").exists():
            verify.append("pytest")
    return {"tools": tools, "formatter_plan": formatters, "verification_plan": verify}


def line_findings(path: Path, text: str) -> list[dict[str, Any]]:
    """Find concrete, explainable style issues in one source file."""
    findings: list[dict[str, Any]] = []
    lines = text.splitlines()
    for index, line in enumerate(lines, 1):
        if INJECTION_RE.search(line):
            findings.append({"severity": "warning", "rule": "constructor-injection", "line": index, "message": "Prefer constructor injection with final dependencies."})
        if len(line) > 120:
            findings.append({"severity": "info", "rule": "line-length", "line": index, "message": "Break the line at a logical boundary."})
    for match in FUNCTION_RE.finditer(text):
        start = text[:match.start()].count("\n") + 1
        end = min(len(lines), start + 25)
        if end - start >= 25:
            findings.append({"severity": "warning", "rule": "function-length", "line": start, "message": "Keep functions at or below 25 physical lines where practical."})
    if path.suffix == ".vue":
        tags = [tag for tag in ("script", "template", "style") if re.search(rf"<{tag}(?:\s|>)", text)]
        if tags != sorted(tags, key=("script", "template", "style").index):
            findings.append({"severity": "warning", "rule": "vue-sfc-order", "line": 1, "message": "Use SFC order: script, template, style."})
    if path.suffix == ".java" and "Controller" in path.name and "@Transactional" in text:
        findings.append({"severity": "warning", "rule": "controller-service-boundary", "line": 1, "message": "Keep transaction and business orchestration in a service."})
    return findings


def scan(project: Path) -> dict[str, Any]:
    """Scan project sources and build a stable report."""
    toolchain = detect_toolchain(project)
    files: list[dict[str, Any]] = []
    for path in iter_sources(project):
        text = path.read_text(encoding="utf-8", errors="ignore")
        files.append({"path": str(path.relative_to(project)), "findings": line_findings(path, text)})
    return {"project": str(project), "toolchain": toolchain, "files_scanned": len(files), "files": files, "findings": sum((item["findings"] for item in files), []), "read_only": True}


def main() -> int:
    """Parse arguments, scan the project, and write JSON output."""
    parser = argparse.ArgumentParser(description="Scan a project for clean-code issues")
    parser.add_argument("--project", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    project = args.project.resolve()
    if not project.is_dir():
        parser.error(f"project directory does not exist: {project}")
    report = scan(project)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Scanned {report['files_scanned']} source files; findings: {len(report['findings'])}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
