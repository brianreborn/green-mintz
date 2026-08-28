# green-mintz — test layers

Japanglify split: domain on a local JVM covers almost all logic. The spare phone covers the Android shell.

## 1. Domain (bots, no Android)
- [x] Interim: `python3 artifacts/decent_exit.py --once`
- [x] Interim: `python3 artifacts/sandbox_smoke.py`
- [x] When `domain/` exists: `./gradlew :domain:test` with JDK 17+ only
- [x] Pool split always sums to 100
- [x] Zero-weight venue gets no new capital
- [x] Hop policy: Lightning first, USDC second, on-chain locked under Standard floor
- [x] Decent-exit returns wait / sell_now / sell_at_expiry
- [x] Share classifier: image → Art studio (mint and/or commission), Cash App screenshot → insight only
- [x] Commission path does not mint without a rights flag
- [x] Stop ends new work
- [x] No receive address ever emitted
- [x] Schema product is `green-mintz` and `cashAppRole` is `edge_transfer_only`

## 2. Bots must not
- [x] Open an Android VM to sign up for Coinbase or Cash App
- [x] Complete KYC as Grok
- [x] Send the Cash App seed anywhere
- [x] Drive Cash App, Coinbase, VGen, or Fantia UI

## 3. Spare unrooted phone (you sideload)
KernelSU off. No-root path only. Debug APK:

**https://github.com/brianreborn/green-mintz/releases/latest/download/app-debug.apk**

See `app/ANDROID.md`. You Confirm hops.

- [ ] Install APK from the GitHub release
- [ ] Sliders and Stop work
- [ ] Share an image lands on Art
- [ ] Retrieve shows Lightning first
- [ ] Lightning hop Confirm shows cents, or cancel
- [ ] You did **not** send BTC to Grok
