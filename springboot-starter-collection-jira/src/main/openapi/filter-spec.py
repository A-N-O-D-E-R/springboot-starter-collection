"""Derives jira-platform-v3.json, the subset of Atlassian's Jira Cloud platform REST API v3 spec
that the client is generated from.

Source: https://developer.atlassian.com/cloud/jira/platform/swagger-v3.v3.json
Usage:  python3 filter-spec.py swagger-v3.v3.json > jira-platform-v3.json

Keeps the listed operations and every component schema they reference, transitively.
"""
import json
import sys

OPERATIONS = {
    "getIssue",
    "searchAndReconsileIssuesUsingJqlPost",
    "addComment",
    "getTransitions",
    "doTransition",
    "getCurrentUser",
}

spec = json.load(open(sys.argv[1]))
paths = {}
for path, item in spec["paths"].items():
    kept = {m: op for m, op in item.items() if isinstance(op, dict) and op.get("operationId") in OPERATIONS}
    if kept:
        paths[path] = kept
missing = OPERATIONS - {op["operationId"] for item in paths.values() for op in item.values()}
if missing:
    sys.exit(f"operations not found: {sorted(missing)}")

schemas = spec["components"]["schemas"]
needed, pending = set(), []


def collect(node):
    if isinstance(node, dict):
        ref = node.get("$ref")
        if isinstance(ref, str) and ref.startswith("#/components/schemas/"):
            name = ref.rsplit("/", 1)[1]
            if name not in needed:
                needed.add(name)
                pending.append(name)
        for value in node.values():
            collect(value)
    elif isinstance(node, list):
        for value in node:
            collect(value)


collect(paths)
while pending:
    collect(schemas[pending.pop()])

subset = {
    "openapi": spec["openapi"],
    "info": spec["info"],
    "servers": spec["servers"],
    "paths": paths,
    "components": {
        "schemas": {name: schemas[name] for name in sorted(needed)},
        "securitySchemes": {"basicAuth": spec["components"]["securitySchemes"]["basicAuth"]},
    },
}
json.dump(subset, sys.stdout, indent=1, sort_keys=True)
