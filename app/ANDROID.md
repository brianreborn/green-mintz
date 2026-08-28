# Android shell — spare unrooted phone

Sideload the debug APK. KernelSU off. You Confirm hops. Coach never presses Cash App Confirm.

## Download

Rolling release (same pattern as japanglify):

**https://github.com/brianreborn/green-mintz/releases/latest/download/app-debug.apk**

If that 404s, open **https://github.com/brianreborn/green-mintz/releases/tag/apk-debug** and grab `app-debug.apk`.

Unknown sources: Settings → security → install unknown apps → Files / Chrome / GitHub.

## Build it yourself (Linux, no Android Studio)

```bash
./scripts/bootstrap-android-sdk.sh
./gradlew :domain:test :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

`./gradlew :domain:test` still works with no SDK.

## What this APK is

- Same `domain/` as the JVM tests (pools, hop policy, share classify, stop, refuseCustody)
- Pools / Venues / Retrieve / Art / STOP
- Share target: `ACTION_SEND` image/* and text, plus `PROCESS_TEXT` → Art inbox
- Lightning checklist first. Invoice is created in Coinbase, never pasted here
- KernelSU off. No Accessibility. No Confirm automation

## What it is not

- Not a Cash App or Coinbase client
- Not a custody wallet (no receive address, no Lightning invoice)
- Not a VGen / Fantia scraper
- Not Auto Invest
