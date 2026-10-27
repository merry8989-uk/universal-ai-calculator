#!/usr/bin/env python3
"""Prove the encrypted sync path end to end, from CI.

Builds a payload, encrypts it exactly the way the app does — PBKDF2-HMAC-SHA256
(120k iterations, 256-bit key) then AES-256-GCM with a 12-byte IV, packed as
base64(salt | iv | ciphertext+tag) — sends it to the Worker, fetches it back and
checks it decrypts to the same text.

Usage: sync_roundtrip.py <worker-url> [code]
"""
import base64
import hashlib
import json
import os
import sys
import urllib.request
from urllib.error import HTTPError

from cryptography.hazmat.primitives.ciphers.aead import AESGCM

ITERATIONS = 120_000
SALT_BYTES = 16
IV_BYTES = 12
SLOT_PREFIX = "nexuscalc-sync-v1|"


def slot_id(code: str) -> str:
    return hashlib.sha256(SLOT_PREFIX.encode() + code.encode()).hexdigest()


def encrypt(plain: str, code: str) -> str:
    salt = os.urandom(SALT_BYTES)
    iv = os.urandom(IV_BYTES)
    key = hashlib.pbkdf2_hmac("sha256", code.encode(), salt, ITERATIONS, 32)
    body = AESGCM(key).encrypt(iv, plain.encode(), None)
    return base64.b64encode(salt + iv + body).decode()


def decrypt(packed: str, code: str) -> str:
    raw = base64.b64decode(packed)
    salt, iv, body = raw[:SALT_BYTES], raw[SALT_BYTES:SALT_BYTES + IV_BYTES], raw[SALT_BYTES + IV_BYTES:]
    key = hashlib.pbkdf2_hmac("sha256", code.encode(), salt, ITERATIONS, 32)
    return AESGCM(key).decrypt(iv, body, None).decode()


def post(url: str, payload: dict) -> dict:
    request = urllib.request.Request(
        url,
        data=json.dumps(payload).encode(),
        headers={
            "Content-Type": "application/json",
            # Cloudflare turns away the default urllib agent
            "User-Agent": "nexus-calc-sync-check/1.0",
        },
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.loads(response.read().decode())


def main() -> int:
    url = sys.argv[1] if len(sys.argv) > 1 else "https://nexus-calc-ai.br19shc39.workers.dev"
    code = sys.argv[2] if len(sys.argv) > 2 else "ci-round-trip-code-please-change"

    # something shaped like a real payload, with awkward characters in it
    payload = (
        "NEXUSSYNC1\n1700000000000\n3\n"
        "themeName\nMidnight Gold\n"
        "language\nhi\n"
        "a=b\nline\nbreak\n"
        "NEXUSCUSTOM1\nc1\nLoan EMI\n\u20b9\nMonthly payment\np*r*(1+r)^n/((1+r)^n-1)\n2\n"
        "p\nPrincipal\n100000.0\nr\nRate\n0.01\n"
        "1+1 = 2\n\u0932\u0902\u092c\u093e\u0908 = 5"
    )

    slot = slot_id(code)
    blob = encrypt(payload, code)
    print(f"payload {len(payload)} chars -> blob {len(blob)} chars, slot {slot[:12]}...")

    # The slot is derived from the code, so in principle whoever knows the code
    # knows the slot. A write therefore has to name the version it replaces, and
    # the first write says "I expect nothing there".
    # A leftover from an earlier run may still be here. Reading it first and
    # writing with its version is exactly what the app does, so the check does
    # the same rather than assuming an empty slot.
    existing = post(url, {"sync": "get", "slot": slot})
    known = existing.get("version", 0) if existing.get("found") else 0
    if known:
        print(f"a leftover backup is here at version {known}; replacing it")

    stored = post(url, {"sync": "put", "slot": slot, "blob": blob, "version": known})
    print("put  :", stored)
    if not stored.get("stored"):
        print("FAIL: the Worker did not store the blob")
        return 1
    version = stored.get("version", 0)

    # A second write that does not say what it is replacing must be refused,
    # otherwise a stale device could silently undo a newer backup.
    try:
        post(url, {"sync": "put", "slot": slot, "blob": blob})
        print("FAIL: an unversioned overwrite was accepted")
        return 1
    except HTTPError as e:
        if e.code != 409:
            raise
        print("unversioned overwrite refused, as it should be")

    # And a write naming the wrong version must be refused too.
    try:
        post(url, {"sync": "put", "slot": slot, "blob": blob, "version": version + 99})
        print("FAIL: a stale version was accepted")
        return 1
    except HTTPError as e:
        if e.code != 409:
            raise
        print("stale version refused, as it should be")

    fetched = post(url, {"sync": "get", "slot": slot})
    print("get  :", {"found": fetched.get("found"), "bytes": len(fetched.get("blob", ""))})
    if not fetched.get("found"):
        print("FAIL: the Worker did not return the blob")
        return 1
    if fetched.get("blob") != blob:
        print("FAIL: the blob changed on the way through")
        return 1

    if decrypt(fetched["blob"], code) != payload:
        print("FAIL: it did not decrypt back to the original")
        return 1

    # a wrong code must not open it
    try:
        decrypt(fetched["blob"], code + "-wrong")
        print("FAIL: a wrong code opened the blob")
        return 1
    except Exception:
        print("wrong code rejected, as it should be")

    print("SYNC ROUND TRIP: PASS")

    # Deleting is the one action with no undo, so it has to be asked for twice.
    try:
        post(url, {"sync": "delete", "slot": slot})
        print("FAIL: an unconfirmed delete was accepted")
        return 1
    except HTTPError as e:
        if e.code != 400:
            raise
        print("unconfirmed delete refused, as it should be")

    # clean up after ourselves
    print("delete:", post(url, {"sync": "delete", "slot": slot, "confirm": True}))
    return 0


if __name__ == "__main__":
    sys.exit(main())
