#!/usr/bin/env python3
"""Print the id of the KV namespace whose title ends with the given suffix.

Reads `wrangler kv namespace list` output on stdin. Wrangler sometimes prints a
warning line before the JSON, so the first '[' starts the payload. Prints an
empty line when there is no match, which the caller treats as "create one".
"""
import json
import sys

suffix = sys.argv[1] if len(sys.argv) > 1 else "NEXUS_SYNC"
raw = sys.stdin.read()
start = raw.find("[")
namespaces = []
if start >= 0:
    try:
        namespaces = json.loads(raw[start:])
    except Exception:
        namespaces = []

if not isinstance(namespaces, list):
    namespaces = []

for entry in namespaces:
    if not isinstance(entry, dict):
        continue
    title = str(entry.get("title", ""))
    if title.endswith(suffix):
        print(str(entry.get("id", "")))
        break
