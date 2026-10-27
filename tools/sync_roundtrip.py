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
VERIFIER_PREFIX = "nexuscalc-verifier-v1|"


def derive(password: str, salt: bytes) -> bytes:
    """The AES key, exactly as SyncCrypto derives it on the phone."""
    return hashlib.pbkdf2_hmac("sha256", password.encode(), salt, ITERATIONS, 32)


def encrypt(plain: str, password: str) -> str:
    """base64( salt | iv | ciphertext-with-tag )."""
    salt = os.urandom(SALT_BYTES)
    iv = os.urandom(IV_BYTES)
    body = AESGCM(derive(password, salt)).encrypt(iv, plain.encode(), None)
    return base64.b64encode(salt + iv + body).decode()


def decrypt(packed: str, password: str) -> str:
    """Raises when the password is wrong or the blob is damaged."""
    raw = base64.b64decode(packed)
    salt = raw[:SALT_BYTES]
    iv = raw[SALT_BYTES:SALT_BYTES + IV_BYTES]
    body = raw[SALT_BYTES + IV_BYTES:]
    return AESGCM(derive(password, salt)).decrypt(iv, body, None).decode()


def post(url: str, payload: dict) -> dict:
    """Sends one JSON request. Raises HTTPError so the caller can read the code."""
    body = json.dumps(payload).encode()
    request = urllib.request.Request(
        url,
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.loads(response.read().decode())


def verifier_for(account_id: str, password: str) -> str:
    """What the server stores instead of the password.

    PBKDF2 over the password, salted with the id — the same construction the app
    uses in Account.kt, so this checks the real thing rather than a stand-in.
    """
    salt = hashlib.sha256((VERIFIER_PREFIX + account_id).encode()).digest()
    return hashlib.pbkdf2_hmac("sha256", password.encode(), salt, 120_000, 32).hex()


def main() -> int:
    url = sys.argv[1] if len(sys.argv) > 1 else "https://nexus-calc-ai.br19shc39.workers.dev"
    account_id = sys.argv[2] if len(sys.argv) > 2 else "ci-roundtrip-account"
    password = sys.argv[3] if len(sys.argv) > 3 else "ci-round-trip-password"

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

    verifier = verifier_for(account_id, password)
    blob = encrypt(payload, password)
    print(f"payload {len(payload)} chars -> blob {len(blob)} chars, id {account_id}")

    # The account has to exist before anything can be stored under it.
    try:
        print("signup:", post(url, {"account": "signup", "id": account_id, "verifier": verifier}))
    except HTTPError as e:
        if e.code != 409:
            raise
        print("the account already exists, which is fine for a re-run")

    # A wrong password must not get in.
    try:
        post(url, {"account": "login", "id": account_id,
                   "verifier": verifier_for(account_id, password + "-wrong")})
        print("FAIL: a wrong password was accepted")
        return 1
    except HTTPError as e:
        if e.code != 401:
            raise
        print("wrong password refused, as it should be")

    print("login:", post(url, {"account": "login", "id": account_id, "verifier": verifier}))

    # The slot is derived from the code, so in principle whoever knows the code
    # knows the slot. A write therefore has to name the version it replaces, and
    # the first write says "I expect nothing there".
    # A leftover from an earlier run may still be here.
    existing = post(url, {"sync": "get", "id": account_id, "verifier": verifier})
    known = existing.get("version", 0) if existing.get("found") else 0
    if known:
        print(f"a leftover backup is here at version {known}; replacing it")

    stored = post(url, {"sync": "put", "id": account_id, "verifier": verifier, "blob": blob, "version": known})
    print("put  :", stored)
    if not stored.get("stored"):
        print("FAIL: the Worker did not store the blob")
        return 1
    version = stored.get("version", 0)

    # A second write that does not say what it is replacing must be refused,
    # otherwise a stale device could silently undo a newer backup.
    try:
        post(url, {"sync": "put", "id": account_id, "verifier": verifier, "blob": blob})
        print("FAIL: an unversioned overwrite was accepted")
        return 1
    except HTTPError as e:
        if e.code != 409:
            raise
        print("unversioned overwrite refused, as it should be")

    # And a write naming the wrong version must be refused too.
    try:
        post(url, {"sync": "put", "id": account_id, "verifier": verifier, "blob": blob, "version": version + 99})
        print("FAIL: a stale version was accepted")
        return 1
    except HTTPError as e:
        if e.code != 409:
            raise
        print("stale version refused, as it should be")

    fetched = post(url, {"sync": "get", "id": account_id, "verifier": verifier})
    print("get  :", {"found": fetched.get("found"), "bytes": len(fetched.get("blob", ""))})
    if not fetched.get("found"):
        print("FAIL: the Worker did not return the blob")
        return 1
    if fetched.get("blob") != blob:
        print("FAIL: the blob changed on the way through")
        return 1

    if decrypt(fetched["blob"], password) != payload:
        print("FAIL: it did not decrypt back to the original")
        return 1

    # a wrong code must not open it
    try:
        decrypt(fetched["blob"], password + "-wrong")
        print("FAIL: a wrong code opened the blob")
        return 1
    except Exception:
        print("wrong code rejected, as it should be")

    print("SYNC ROUND TRIP: PASS")

    # Deleting is the one action with no undo, so it has to be asked for twice.
    try:
        post(url, {"sync": "delete", "id": account_id, "verifier": verifier})
        print("FAIL: an unconfirmed delete was accepted")
        return 1
    except HTTPError as e:
        if e.code != 400:
            raise
        print("unconfirmed delete refused, as it should be")

    # clean up after ourselves
    print("delete:", post(url, {"sync": "delete", "id": account_id, "verifier": verifier, "confirm": True}))
    return 0


if __name__ == "__main__":
    sys.exit(main())
