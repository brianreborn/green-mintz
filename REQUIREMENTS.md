# green-mintz — Grok Bot + web + auto-trade

Status: draft 2026-09-04. Does not change the live APK until you accept this.

## Goal

Same product, three surfaces:

| Surface | Job |
|---|---|
| Grok Bot (this chat) | Coach, STOP, arm/disarm, read-only status |
| Web PWA | iPhone / desktop / anything that is not the spare Android |
| Android APK | Share-target, Lightning hop checklist, on-device vault, WatchService |

Coinbase remains the **source of truth**. Devices do not sync a second ledger. They all read the same View+Trade account.

## What “completely automatic” means (and does not)

**Does:** after you arm **production** and a hop has landed on **your** Coinbase, the book places `*-USDC` market IOC orders with no per-order tap. STOP still cancels open orders. Transfer stays off.

**Does not:** send Bitcoin to Grok, xAI, or this chat. There is still no receive address and no Lightning invoice here. A bot that “has the transfer” is **your Coinbase**, not a Grok wallet.

Minting automatic is a **separate arm**. Listing still needs a signer you control (Phantom / Coinbase Wallet / a mint key you pasted into the same vault). Grok never holds the art file as custody.

## Runtimes

1. **Android (exists)** — EncryptedSharedPreferences vault, foreground WatchService, share inbox, VGen/Fantia tap-open.
2. **Grok Bot (new)** — utterance in, domain out. Tools: balances, plan, arm, STOP. No CDP JSON in the thread after the first paste; paste goes to the vault on the device that will trade.
3. **Web PWA (exists as preview shell, broker missing)** — same tabs. Browser cannot call `api.coinbase.com` from grok.com (CORS). Needs a **user-owned relay**: Android WatchService already is one; or a tiny worker you run; or GitHub Actions on a schedule. Grok’s servers must not store the PEM.
4. **Durable tick (needed for auto when the phone sleeps / chat is idle)** — GitHub Actions cron or Grok Automations that call the **relay**, not Coinbase directly with a key in the prompt.

Default confirm mode stays **development** until you say `arm production`.

## Must-have before bot auto-trade is real

- Production arm is an explicit utterance and a second confirm the first time only
- Hard caps: max USDC per order, max USDC per day, halt if drawdown from session high exceeds a band you set
- Paper book: same `planBook`, zero `createOrder`
- Heartbeat: last successful Coinbase tick age; if stale, treat as disarmed
- Audit log on-device (and optional private GitHub issue/release note): time, product, side, size, order id
- STOP from chat, web, or Android all cancel open orders
- Key never logged, never in HANDOFF, never in a Grok prompt after vault save
- Transfer / withdraw / convert / addresses still allow-listed out

## Should-have (web + other platforms)

- PWA install on iOS Safari and desktop (same UI as the preview)
- Hop checklists that work without Android Share (copy/paste Lightning amount you Confirm in Cash App)
- Pick-image on web for art; VGen/Fantia still `window.open` tap-lists
- Preset splits (50/50, 70/30, 100 liquid, 100 NFT dry powder)
- Session banner: DEV vs PRODUCTION vs STOPPED vs HEARTBEAT STALE
- Notify path: Android notification (exists), plus optional X/Discord webhook you own

## Minting (bot cannot sign for you unless you give it a key)

Keep tap-list as the default. Optional later:

- **Mint key vault** (separate from CDP, same encryption). Only used if you arm mint.
- Metadata JSON + image stay on-device; mint to **your** wallet on Solana first, Base second
- Client-exclusive commission still requires the rights flag
- No scrape, no auto-post to VGen/Fantia

## Will not

- Grok/xAI custody address or invoice
- Accessibility / auto-press Cash App Confirm
- Register Coinbase or Cash App as Grok
- Withdraw or transfer off Coinbase
- KYC as Grok
- Drive VGen/Fantia UI

## Suggested build order

1. Paper book + caps + heartbeat on Android (proves auto math without new risk)
2. Web PWA wired to **read-only** Coinbase via a relay you control (balances/plan only)
3. Grok Bot status/STOP/arm as a thin client of that same relay
4. Production auto on the relay with caps
5. Optional mint-key arm

## Open questions for you

1. Is the durable tick the spare Android (already works), or do you want GitHub Actions / a VPS even when the phone is off?
2. Paper book first, or skip to live production auto?
3. Mint stays tap-list for alpha, or do you want a mint key vault in the same release?
