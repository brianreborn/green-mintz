package com.brianreborn.greenmintz.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brianreborn.greenmintz.ArtIntent
import com.brianreborn.greenmintz.CoachStore
import com.brianreborn.greenmintz.CoachUiState
import com.brianreborn.greenmintz.HopPhase
import mintz.domain.ConfirmMode
import mintz.domain.HopPath
import mintz.domain.ShareKind
import mintz.domain.bookKeyChecklist
import mintz.domain.commissionChecklist
import mintz.domain.lightningChecklist
import mintz.domain.onchainChecklist
import mintz.domain.returnChecklist
import mintz.domain.usdcChecklist

@Composable
internal fun PoolsView(s: CoachUiState) {
    var live by remember(s.split.liquidCrypto, s.pending) {
        mutableFloatStateOf(s.split.liquidCrypto.toFloat())
    }
    val shown = live.toInt()
    Text("Total · sliders armed", color = Cream, fontSize = 24.sp, fontFamily = FontFamily.Serif)
    Warn("Ramp pile too small for on-chain. Use Lightning or leave it.")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .height(48.dp),
    ) {
        Box(
            Modifier
                .weight(shown.coerceAtLeast(1).toFloat())
                .fillMaxSize()
                .background(Teal),
            contentAlignment = Alignment.Center,
        ) { Text("$shown% liquid", color = Cream, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        Box(
            Modifier
                .weight((100 - shown).coerceAtLeast(1).toFloat())
                .fillMaxSize()
                .background(Gold),
            contentAlignment = Alignment.Center,
        ) { Text("${100 - shown}% NFT", color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
    }
    Text("Split (always sums to 100)", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
    Slider(
        value = live,
        onValueChange = { live = it },
        valueRange = 0f..100f,
        enabled = !s.stopped,
        onValueChangeFinished = {
            if (shown != s.split.liquidCrypto) CoachStore.proposeSplit(shown)
        },
        colors = SliderDefaults.colors(thumbColor = Teal, activeTrackColor = Teal),
    )
    Text("Holding BTC on the Cash App ramp. Coinbase and NFT books wait until you hop.", color = Muted, fontSize = 14.sp)
    Text("Cash App pile (USD, typed by you)", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
    OutlinedTextField(
        value = s.pileUsd.toString(),
        onValueChange = { CoachStore.setPileUsd(it.toDoubleOrNull() ?: 0.0) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = fieldColors(),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Navy2)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Confirm mode", color = Cream, fontSize = 14.sp)
            Text("Production still never taps Cash App Confirm.", color = Muted, fontSize = 12.sp)
        }
        OutlinedButton(onClick = {
            CoachStore.setConfirmMode(
                if (s.confirmMode == ConfirmMode.DEVELOPMENT) ConfirmMode.PRODUCTION else ConfirmMode.DEVELOPMENT,
            )
        }) {
            Text(s.confirmMode.name.lowercase(), color = Cream)
        }
    }
}

@Composable
internal fun VenuesView(s: CoachUiState) {
    Text("Venues", color = Cream, fontSize = 24.sp, fontFamily = FontFamily.Serif)
    Text("Weights renormalize. Zero = no new capital.", color = Muted, fontSize = 14.sp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Navy2)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Coinbase *-USDC", color = Cream, fontSize = 14.sp)
            Text("view+trade key only · transfer off", color = Muted, fontSize = 12.sp)
        }
        Text("100", color = Gold, fontSize = 18.sp, fontFamily = FontFamily.Serif)
    }
    s.venues.forEach { v ->
        var live by remember(v.id, v.weight, s.pending) { mutableFloatStateOf(v.weight.toFloat()) }
        val shown = live.toInt()
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Navy2)
                .padding(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(v.label, color = Cream, fontSize = 14.sp)
                    val extra = buildString {
                        append(v.chain)
                        if (v.discoveryOnly) append(" · discovery, not fill")
                        if (shown == 0) append(" · no new capital")
                    }
                    Text(extra, color = Muted, fontSize = 12.sp)
                }
                Text("$shown", color = Gold, fontSize = 18.sp, fontFamily = FontFamily.Serif)
            }
            Slider(
                value = live,
                onValueChange = { live = it },
                valueRange = 0f..100f,
                enabled = !s.stopped,
                onValueChangeFinished = {
                    if (shown != v.weight) CoachStore.proposeVenue(v.id, shown)
                },
                colors = SliderDefaults.colors(thumbColor = Teal, activeTrackColor = Teal),
            )
        }
    }
}

@Composable
internal fun RetrieveView(s: CoachUiState) {
    val path = s.hopPath
    val title = when (path) {
        HopPath.LIGHTNING -> "Lightning hop checklist"
        HopPath.LOCKED -> "Retrieve from Cash App"
        HopPath.USDC -> "USDC fallback checklist"
        HopPath.ON_CHAIN -> "On-chain hop checklist"
    }
    val lines = when (path) {
        HopPath.LIGHTNING -> lightningChecklist()
        HopPath.USDC -> usdcChecklist()
        HopPath.ON_CHAIN -> onchainChecklist()
        HopPath.LOCKED -> listOf(
            "On-chain is locked under Cash App free Standard (~100,000 sats).",
            "Turn Lightning on, or leave the pile on the ramp.",
        )
    }
    Text(title, color = Cream, fontSize = 24.sp, fontFamily = FontFamily.Serif)
    Text(
        if (path == HopPath.LIGHTNING) "preferred small-seed path" else "~${s.sats} sats · ${path.name.lowercase().replace('_', ' ')}",
        color = Teal,
        fontSize = 14.sp,
    )
    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Toggle("Lightning available", s.lightningOk, Modifier.weight(1f)) { CoachStore.setLightningOk(!s.lightningOk) }
        Toggle("USDC fallback", s.usdcOk, Modifier.weight(1f)) { CoachStore.setUsdcOk(!s.usdcOk) }
    }
    if (path == HopPath.LOCKED) Warn("On-chain send locked. Lightning is the small-pile path.")
    if (s.hopPhase == HopPhase.WAITING) {
        Text("Paid in Cash App — done", color = Ok, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
        Text("Landing on Coinbase — in progress", color = Cream, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        Text("Convert to USDC — locked until you say", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        Text(
            "Watch Coinbase. Do not send again.",
            color = Navy,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Cream)
                .padding(12.dp)
                .fillMaxWidth(),
        )
    } else {
        lines.forEachIndexed { i, line ->
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Navy3),
                    contentAlignment = Alignment.Center,
                ) { Text("${i + 1}", color = Cream, fontSize = 12.sp) }
                Text(line, color = Cream, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
    if (path == HopPath.LIGHTNING && s.hopPhase != HopPhase.WAITING) {
        Text(
            "green-mintz does not press Confirm.",
            color = Gold,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Navy3)
                .padding(12.dp)
                .fillMaxWidth(),
        )
    }
    Button(
        onClick = { CoachStore.beginHop() },
        enabled = !s.stopped,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Teal),
    ) { Text("Show Lightning taps") }
    if (s.hopPhase == HopPhase.CHECKLIST) {
        OutlinedButton(
            onClick = { CoachStore.markPaid() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(48.dp),
        ) { Text("I paid in Cash App", color = Cream) }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 28.dp)
            .border(1.dp, Cream.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Text("Return to Cash App", color = Cream, fontSize = 18.sp, fontFamily = FontFamily.Serif)
        returnChecklist().forEachIndexed { i, line ->
            Text("${i + 1}. $line", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
internal fun ArtView(s: CoachUiState) {
    val share = s.share
    val showInbox = share != null && s.artIntent == ArtIntent.NONE && share.kind != ShareKind.SCREENSHOT
    Text(if (share != null) "Shared with green-mintz" else "Art studio", color = Cream, fontSize = 24.sp, fontFamily = FontFamily.Serif)
    Text("Share an image. Mint to YOUR wallet, or offer as a commission. No Grok custody.", color = Muted, fontSize = 14.sp)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .height(112.dp)
            .border(1.dp, Cream.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Navy2),
        contentAlignment = Alignment.Center,
    ) {
        Text("Share into green-mintz — list as NFT", color = Muted, fontSize = 14.sp)
    }
    if (share != null) {
        SharePreview(share.uri, share.name)
        if (showInbox) {
            Button(
                onClick = { CoachStore.setArtIntent(ArtIntent.MINT) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
            ) { Text("This is my art — mint and list") }
            OutlinedButton(
                onClick = { CoachStore.setArtIntent(ArtIntent.COMMISSION) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(48.dp),
            ) { Text("Offer as VGen / Fantia commission", color = Cream) }
            TextButton(
                onClick = { CoachStore.markScreenshot() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("This is a note / Cash App screenshot", color = Muted) }
        }
        if (share.kind == ShareKind.SCREENSHOT && s.artIntent == ArtIntent.NONE) {
            Warn("Cash App screenshot is hop insight only. It will not become an NFT unless you say so with a different file.")
        }
    }
    if (s.artIntent == ArtIntent.COMMISSION) {
        Text("Commission tap-list", color = Cream, fontSize = 18.sp, fontFamily = FontFamily.Serif, modifier = Modifier.padding(top = 16.dp))
        commissionChecklist().forEachIndexed { i, line ->
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Navy3),
                    contentAlignment = Alignment.Center,
                ) { Text("${i + 1}", color = Cream, fontSize = 12.sp) }
                Text(line, color = Cream, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
            }
        }
        CheckRow("Client exclusive commission", s.clientExclusive) { CoachStore.setClientExclusive(it) }
        CheckRow("I have rights to mint this piece", s.rightsFlag) { CoachStore.setRightsFlag(it) }
        if (!s.canMint && s.clientExclusive) {
            Text("Commission path does not mint without a rights flag.", color = Gold, fontSize = 14.sp)
        }
        OutlinedButton(
            onClick = { CoachStore.setArtIntent(ArtIntent.MINT) },
            enabled = s.canMint,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(48.dp),
        ) { Text("Also list as NFT", color = Cream) }
    }
    if (s.artIntent == ArtIntent.MINT) {
        Text("List this as an NFT", color = Cream, fontSize = 18.sp, fontFamily = FontFamily.Serif, modifier = Modifier.padding(top = 16.dp))
        Text("Title", color = Muted, fontSize = 12.sp)
        OutlinedTextField(
            value = s.artTitle,
            onValueChange = CoachStore::setArtTitle,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("your piece", color = Muted) },
            colors = fieldColors(),
        )
        Text("Chain (cheapest first)", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            listOf("solana", "base", "ethereum").forEach { c ->
                val on = s.chain == c
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (on) Teal else Navy2)
                        .clickable { CoachStore.setChain(c) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (c == "ethereum") "ethereum if required" else c,
                        color = if (on) Cream else Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Text("Markets with weight above zero", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
        s.venues.filter { it.weight > 0 && !it.discoveryOnly }.forEach { v ->
            CheckRow(v.label, v.id in s.mintVenues) { CoachStore.toggleMintVenue(v.id) }
        }
        Text(
            "Mint to YOUR wallet. Grok never receives this file as custody.",
            color = Gold,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(WarnBg)
                .padding(12.dp)
                .fillMaxWidth(),
        )
        Button(
            onClick = { CoachStore.requestMint() },
            enabled = !s.stopped && s.canMint,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
        ) { Text("Open wallet to sign") }
    }
    Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Shield, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp))
        Text("  VGen and Fantia are tap-lists. We do not scrape or auto-post.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun SharePreview(uri: Uri?, name: String) {
    val context = LocalContext.current
    val bmp = remember(uri) {
        if (uri == null) null
        else runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = name,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(140.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(88.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Navy3),
            contentAlignment = Alignment.Center,
        ) { Text(name, color = Gold, fontSize = 14.sp) }
    }
}

@Composable
internal fun BookView(s: CoachUiState) {
    var paste by remember { mutableStateOf("") }
    Text("Coinbase book", color = Cream, fontSize = 24.sp, fontFamily = FontFamily.Serif)
    Text("View+trade key only. Transfer off. Key stays on this phone.", color = Muted, fontSize = 14.sp)
    Text(s.keyLabel, color = Gold, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
    if (!s.keyPresent) {
        bookKeyChecklist().forEachIndexed { i, line ->
            Text("${i + 1}. $line", color = Cream, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        }
        OutlinedTextField(
            value = paste,
            onValueChange = { paste = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(120.dp),
            placeholder = { Text("Paste CDP JSON here", color = Muted) },
            colors = fieldColors(),
        )
        Button(
            onClick = { CoachStore.saveKey(paste); paste = "" },
            enabled = paste.isNotBlank() && !s.stopped,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
        ) { Text("Save key on this phone") }
    } else {
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { CoachStore.testKey() }, modifier = Modifier.weight(1f).height(48.dp)) {
                Text("Test view", color = Cream)
            }
            OutlinedButton(onClick = { CoachStore.clearKey() }, modifier = Modifier.weight(1f).height(48.dp)) {
                Text("Remove key", color = Cream)
            }
        }
        Button(
            onClick = { if (s.bookArmed) CoachStore.disarmBook() else CoachStore.armBook() },
            enabled = !s.stopped || s.bookArmed,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (s.bookArmed) Stop else Teal),
        ) { Text(if (s.bookArmed) "Disarm book" else "Arm watching") }
        OutlinedButton(
            onClick = { CoachStore.armConvert() },
            enabled = s.bookArmed && !s.stopped,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(48.dp),
        ) { Text(if (s.convertArmed) "Convert BTC armed" else "Convert hopped BTC → USDC", color = Cream) }
    }
    if (s.balances.isNotEmpty()) {
        Text("Balances", color = Cream, fontSize = 18.sp, fontFamily = FontFamily.Serif, modifier = Modifier.padding(top = 20.dp))
        s.balances.forEach { b ->
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(b.currency, color = Cream, fontSize = 14.sp)
                Text("%.6f".format(b.available).trimEnd('0').trimEnd('.'), color = Gold, fontSize = 14.sp)
            }
        }
    }
    if (s.planLine.isNotBlank()) {
        Text(s.planLine, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
    }
    Text(
        "Development confirms each order. Production auto-places *-USDC. STOP cancels opens. Watch fills on coinbase.com.",
        color = Muted,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 16.dp),
    )
}
