#!/usr/bin/env python3
"""Handoff smoke tests. No KYC. No Cash App. No real money.

Coinbase Advanced Trade sandbox returns static mocked JSON
without authentication. Public spot is a live print only.
"""
from __future__ import annotations

import json
import sys
import urllib.request

SANDBOX = "https://api-sandbox.coinbase.com/api/v3/brokerage"
PUBLIC = "https://api.coinbase.com/v2/prices/BTC-USD/spot"


def get(url: str) -> dict:
    req = urllib.request.Request(url, headers={"User-Agent": "green-mintz-sandbox-smoke/1.5"})
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read().decode())


def main() -> int:
    fails = []
    try:
        spot = get(PUBLIC)
        amt = float(spot["data"]["amount"])
        print(f"public BTC-USD {amt}")
        if amt <= 0:
            fails.append("public spot not a price")
    except Exception as exc:
        fails.append(f"public spot: {exc}")

    try:
        accounts = get(f"{SANDBOX}/accounts")
        print("sandbox accounts keys:", list(accounts)[:6])
        if not isinstance(accounts, dict):
            fails.append("sandbox accounts not an object")
    except Exception as exc:
        fails.append(f"sandbox accounts: {exc}")

    if fails:
        print("FAIL", *fails, sep="\n  ")
        return 1
    print("PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
