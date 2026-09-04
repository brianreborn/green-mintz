# green-mintz — Grok Bot + web + rapid auto-trade

Status: draft 2026-09-04. Domain for rapid alts + when-necessary NFTs is in tree. Live Magic Eden/Tensor fills are **not** wired yet.

## Goal

Same product, three surfaces. Coinbase is the ledger. Grok is never the wallet.

**Rapid, unattended trades when necessary:**

| Book | Venue | When it fires without a tap |
|---|---|---|
| Alts | Coinbase Advanced `*-USDC` (BTC ETH SOL LINK AVAX SUI DOGE XRP ADA) | Drift outside a 3% band, 5s ticks, 50 USDC cap/order |
| NFTs | Tensor / Magic Eden / OpenSea / Blur (not Coinbase) | Floor dumped vs open → sell now. Floor cheap vs open and sleeve has USDC → one buy. Else hold. |

Say `arm rapid` once. Production + live. `paper` keeps the same math with zero orders. STOP cancels.

## What this is not

Grok still cannot receive Bitcoin. NFT rapid cannot fill until you later paste a **separate mint/trade wallet key** (not the CDP key). Until then the NFT sleeve stays USDC dry powder and only **logs** when a trade would have been necessary.

Discovery-only venues (Botmesh) never fill. No VGen/Fantia scrape. Transfer off.

## Caps (always on for rapid)

- 50 USDC per order
- 3% drift band (10% when slow book)
- Only products Coinbase actually quoted this tick
- Paper default until `arm rapid` confirm
- Heartbeat: if a tick fails, next tick retries; STOP disarms rapid

## Build order now

1. Domain rapid alt book + NFT when-necessary intents (this drop)
2. Android WatchService 5s + paper/live (this drop)
3. NFT venue broker (Magic Eden/Tensor) — blocked on a wallet key you own
4. Web/Grok Bot as thin clients of the same book
