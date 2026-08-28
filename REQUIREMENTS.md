# green-mintz SRS (repo export)

Contract of record: `interface-schemas.json` v1.5.0.
Product: **green-mintz**. Grok is the interface. Cash App is only the hop.
Word original: `GrokBot-Trading-System-Requirements.docx` in the project folder.

## Intent

Mint, commission, buy, sell, and trade art as NFTs. Liquid `*-USDC` on Coinbase Advanced Trade funds that book. User steers pools. System picks tickets. User watches. Stop is always one control away.

## Rails

- In preferred: Cash App Lightning (pay from BTC balance) to a Coinbase Lightning invoice the user creates. Grok never supplies an invoice or address.
- In fallback: sell to dollars on Cash App, send USDC on Solana/Base to the user's Coinbase USDC address.
- In locked: on-chain BTC under Cash App free Standard floor (~100,000 sats).
- Trade fungible: Coinbase Advanced Trade, view+trade key, transfer off.
- Trade NFT: Blur, Tensor, Chadbot, Magic Eden, OpenSea/Reservoir, AgentVault, Bonsai, Botmesh (discovery).
- Commissions: VGen and Fantia as tap-lists. Do not scrape or auto-post. Do not mint a client exclusive without a rights flag.
- Out: return hop to the user's Cash App receive. Dollars via native Cash App Sell + Cash Out.

## Pools

Default 50 liquid crypto / 50 NFT. Venue weights renormalize. Zero weight = no new capital. Rebalance when drift > 10%.

## UX

Android 1080x2400. Screens: Home pools, Venues, Retrieve, Art, Share inbox. STOP always visible. No addresses in chat. Dev confirms every step. Production auto-accepts pool math only.

## Supervision

User watches. Stop = cancel Coinbase orders + revoke key. Never press Cash App Confirm. Never Accessibility on Cash App.

## Testing

Japanglify split: `./gradlew :domain:test` on a JDK. Spare unrooted phone for the Android shell. No Android VM KYC as Grok.

## KernelSU

Optional, off. Local mine/mint resource caps only. No su into other apps.

## Out of scope

Bank, ACH, debit, Auto Invest as engine, Custom Orders as engine, Grok custody, leverage, Reddit spam, mint snipers.
