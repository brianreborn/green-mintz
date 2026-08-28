# green-mintz design artifacts

Phone-first mock-ups from the requirements pass (28 August 2026).
Palette: navy `#0B1220`, cream `#F4F1EA`, teal `#1F6F66`, gold `#C4A574`, stop `#9F1239`.

Raster originals still say **GrokBot**. Implement as **green-mintz**.

## Current hop policy (overrides some retrieve mock-ups)

1. Lightning first: Cash App BTC balance pays a Coinbase Lightning invoice the user creates.
2. USDC send on Solana/Base if Lightning is missing on that Coinbase account.
3. On-chain locked under Cash App free Standard floor (~100,000 sats).

Mock-ups 04–07 were drawn when the default hop was on-chain. Keep them for locked / waiting / return flows. Do not implement “Coinbase does not take Lightning” from the old ready-checklist raster.

## Screens

| File | Screen | Implement |
|---|---|---|
| `01-architecture-pools.svg` | System board | Relabel to green-mintz |
| `02-home-pools.svg` | Home split 50/50, STOP, small-pile banner | Yes |
| `03-venues.svg` | Per-venue weights | Coinbase is the fungible book |
| `04-retrieve-locked.svg` | On-chain locked | Offer Lightning as the unlocked path |
| `05-retrieve-ready-checklist.svg` | Lightning checklist | User copies invoice in Coinbase, not here |
| `06-retrieve-waiting.svg` | Hop in flight | Do not send again |
| `07-return-to-cash-app.svg` | Return hop | Bitcoin network only |
| `08-android-share-sheet.svg` | ACTION_SEND | Label green-mintz |
| `09-share-inbox.svg` | Art vs commission vs screenshot | Yes |
| `10-mint-and-list.svg` | Mint to user wallet | No Grok custody |

Raster JPGs with the same names live in the project folder `artifacts/design/` (binary; this GitHub tool path is SVG).

## Do not copy from old rasters

- Product name GrokBot on chrome
- Coinbase does not take Lightning
- Auto Invest / Custom Orders
- Grok receive addresses
- Accessibility Confirm
