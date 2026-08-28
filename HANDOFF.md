# green-mintz — handoff

Product: **green-mintz** (was GrokBot / grokbot-cashapp-coach)
Project files: `/home/workdir/artifacts`

The point is minting, commissioning, buying, selling, and trading art as NFTs.
Cash App is an **edge hop only**. Liquid funding book is Coinbase Advanced Trade (`*-USDC`).

Layout like japanglify:
- `domain/` — pure JVM. Pools, hop policy, decent-exit, share classify, commission vs mint split, stop. No Android.
- `app/` — Android shell. Share target, Retrieve, Art, sliders.

## What a background agent may finish
- Keep `interface-schemas.json` aligned with this file
- Keep `decent_exit.py` as a public-print timing helper
- Run `python3 sandbox_smoke.py` and `python3 decent_exit.py --once`
- When `domain/` exists: `./gradlew :domain:test` (JDK only, no Android SDK)
- Keep TEST.md accurate

## What a background agent must not do
- Treat Auto Invest or Custom Orders as the engine
- Register Coinbase or Cash App as Grok
- Scrape or auto-post VGen / Fantia
- Mint a client-exclusive commission without a rights flag
- Stand up an Android VM for KYC
- Add a production API secret, a custody address, or Accessibility against Cash App
- Press Confirm anywhere

Device under test: old working phone the user provides. Unrooted. KernelSU off. UI + Lightning hop only.

Default confirm mode stays **development**.

Repo: https://github.com/brianreborn/green-mintz  
Old path `brianreborn/grokbot-cashapp-coach` is leftover. Do not implement from it.
