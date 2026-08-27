# Evaluation contract

The skill's deterministic contract is that it emits valid JSON, remains read-only, reports toolchain keys, and never scans generated directories. The fixtures are input-only examples for representative language contexts.

```json
{
  "skill": "universal-clean-code-skill",
  "run": "python3 scripts/run_pipeline.py --project {input} --output {output}",
  "criteria": [
    {"id": "valid-json", "text": "Output is valid JSON", "type": "command", "cmd": "python3 -c \"import json; json.load(open({output}))\""},
    {"id": "report-shape", "text": "Report contains toolchain, files_scanned, findings, and read_only", "type": "command", "cmd": "python3 -c \"import json; d=json.load(open({output})); assert all(k in d for k in ['toolchain','files_scanned','findings','read_only']); assert d['read_only'] is True\""},
    {"id": "safe-scope", "text": "Report identifies a read-only scan and excludes generated trees", "type": "command", "cmd": "python3 -c \"import json; d=json.load(open({output})); assert d['read_only'] is True; assert all('target' not in x['path'] for x in d.get('files', []))\""}
  ],
  "golden": [
    {"id": "java-spring", "input": "golden/java-spring", "expected": null, "expected_status": "pending-first-green", "split": "val"},
    {"id": "vue-sfc", "input": "golden/vue-sfc", "expected": null, "expected_status": "pending-first-green", "split": "val"},
    {"id": "python-project", "input": "golden/python-project", "expected": null, "expected_status": "pending-first-green", "split": "test"}
  ]
}
```
