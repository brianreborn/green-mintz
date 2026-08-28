# Android shell — spare unrooted phone

Sideload the debug APK. KernelSU off. You Confirm hops. Coach never presses Cash App Confirm.

## Download

**https://github.com/brianreborn/green-mintz/releases/latest/download/app-debug.apk**

If that 404s, open **https://github.com/brianreborn/green-mintz/releases/tag/apk-debug**.

Unknown sources: Settings → security → install unknown apps → Files / Chrome / GitHub.

If an older debug APK is installed, uninstall it first (signing key may differ).

## Alpha 1.1 — live Coinbase book

1. On **your** Coinbase: CDP API key with **View + Trade**. Leave **Transfer off**.
2. Download the JSON once. Open green-mintz → **Book** → paste → Save key on this phone.
3. **Test view** should list balances.
4. Hop BTC Lightning yourself (Retrieve). Then **Arm watching**.
5. Development: confirm each *-USDC order in the sheet. Watch fills on coinbase.com.
6. Production: pool math + book orders auto. Cash App Confirm still never.
7. **STOP** cancels open Coinbase orders.

The secret never leaves the phone. Transfer/withdraw paths are not compiled in.

## Build it yourself (Linux)

```bash
./scripts/bootstrap-android-sdk.sh
./gradlew :domain:test :broker:test :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```
