"""Prints the id of a D1 database from `wrangler d1 list --json`.

Kept as a file rather than inline in the workflow: Python written inside a YAML
block arrives indented, and indented top-level code is a syntax error.
"""
import json
import sys

try:
    databases = json.load(sys.stdin)
except Exception:
    sys.exit(0)

if not isinstance(databases, list):
    databases = databases.get("result") or databases.get("databases") or []

for db in databases:
    if not isinstance(db, dict):
        continue
    if db.get("name") == "nexus-events":
        print(db.get("uuid") or db.get("id") or "")
        break
