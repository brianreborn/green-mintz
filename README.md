# green-mintz

Mint, commission, buy, sell, and trade art as NFTs.
Cash App is only the hop. Coinbase Advanced Trade `*-USDC` is the funding book.

## Sideload the APK

Spare unrooted phone. KernelSU off. You Confirm hops.

**https://github.com/brianreborn/green-mintz/releases/latest/download/app-debug.apk**

Alpha 1.1: Book tab runs view+trade on YOUR Coinbase while the app is running. Transfer off. Key stays on the phone.

See `app/ANDROID.md`.

## Layout (japanglify split)

- `domain/` — pure JVM. Pools, hop policy, decent-exit, book plan, share classify, stop. `./gradlew :domain:test`
- `broker/` — Coinbase REST. No withdraw/transfer paths. `./gradlew :broker:test`
- `app/` — Android shell. Share target, Retrieve, Art, Book. `./scripts/bootstrap-android-sdk.sh` then `./gradlew :app:assembleDebug`

Private: https://github.com/brianreborn/green-mintz
