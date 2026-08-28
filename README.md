# green-mintz

Mint, commission, buy, sell, and trade art as NFTs.
Cash App is only the hop. Coinbase Advanced Trade `*-USDC` is the funding book.

## Sideload the APK

Spare unrooted phone. KernelSU off. You Confirm hops.

**https://github.com/brianreborn/green-mintz/releases/latest/download/app-debug.apk**

If that 404s, the rolling tag is [apk-debug](https://github.com/brianreborn/green-mintz/releases/tag/apk-debug).

See `app/ANDROID.md`.

## Layout (japanglify split)

- `domain/` — pure JVM. Pools, hop policy, decent-exit, share classify, stop. `./gradlew :domain:test`
- `app/` — Android shell. Share target, Retrieve, Art, sliders. `./scripts/bootstrap-android-sdk.sh` then `./gradlew :app:assembleDebug`

## Start here

- `HANDOFF.md` — what a coder may and must not do
- `interface-schemas.json` v1.5.0 — machine contract
- `REQUIREMENTS.md` — SRS export
- `design/DESIGN.md` — mock-ups
- `TEST.md` — domain JVM tests vs spare phone

Private: https://github.com/brianreborn/green-mintz
