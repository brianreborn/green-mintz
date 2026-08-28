package com.brianreborn.greenmintz.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brianreborn.greenmintz.CoachStore
import com.brianreborn.greenmintz.CoachUiState
import com.brianreborn.greenmintz.Tab
import mintz.domain.ConfirmMode
import mintz.domain.refuseCustody

@Composable
fun MintzScreen() {
    val s by CoachStore.state.collectAsStateWithLifecycle()
    val custody = remember { refuseCustody("receive") }
    Box(
        Modifier
            .fillMaxSize()
            .background(Navy)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            Header(s)
            if (s.stopped) {
                Text(
                    "Stopped. Cancel open Coinbase orders. Revoke the trade key. Nothing keeps buying.",
                    color = Cream,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Stop.copy(alpha = 0.2f))
                        .padding(12.dp),
                )
            }
            SpeakBar(s)
            Text(
                s.lastLine,
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                when (s.tab) {
                    Tab.POOLS -> PoolsView(s)
                    Tab.VENUES -> VenuesView(s)
                    Tab.RETRIEVE -> RetrieveView(s)
                    Tab.ART -> ArtView(s)
                }
                Text(
                    custody.message,
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
                )
            }
            HorizontalDivider(color = Cream.copy(alpha = 0.12f))
            NavBar(s.tab)
        }
        if (s.pending != null) ConfirmSheet(s)
    }
}

@Composable
private fun Header(s: CoachUiState) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(
                "green-mintz",
                color = Cream,
                fontSize = 26.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
            )
            Text(
                if (s.confirmMode == ConfirmMode.DEVELOPMENT) "DEV · confirm each step" else "WATCHING · pool math auto",
                color = Muted,
                fontSize = 12.sp,
            )
        }
        Button(
            onClick = { CoachStore.kill() },
            colors = ButtonDefaults.buttonColors(containerColor = Stop, contentColor = Cream),
            modifier = Modifier.height(48.dp),
        ) { Text("STOP", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun SpeakBar(s: CoachUiState) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = s.speak,
            onValueChange = CoachStore::setSpeak,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("set split 70/30 · zero Blur · stop", color = Muted) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { CoachStore.runUtterance(s.speak) }),
            colors = fieldColors(),
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = { CoachStore.runUtterance(s.speak) },
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
            modifier = Modifier.height(48.dp),
        ) { Text("Say") }
    }
}

@Composable
private fun NavBar(tab: Tab) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        NavBtn("Pools", Icons.Outlined.Layers, tab == Tab.POOLS) { CoachStore.setTab(Tab.POOLS) }
        NavBtn("Venues", Icons.Outlined.Map, tab == Tab.VENUES) { CoachStore.setTab(Tab.VENUES) }
        NavBtn("Retrieve", Icons.Outlined.Download, tab == Tab.RETRIEVE) { CoachStore.setTab(Tab.RETRIEVE) }
        NavBtn("Art", Icons.Outlined.Image, tab == Tab.ART) { CoachStore.setTab(Tab.ART) }
    }
}

@Composable
private fun NavBtn(label: String, icon: ImageVector, on: Boolean, click: () -> Unit) {
    Column(
        Modifier
            .clickable(onClick = click)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = if (on) Teal else Muted, modifier = Modifier.size(22.dp))
        Text(label, color = if (on) Teal else Muted, fontSize = 11.sp)
    }
}

@Composable
private fun ConfirmSheet(s: CoachUiState) {
    val p = s.pending ?: return
    Box(
        Modifier
            .fillMaxSize()
            .background(Navy.copy(alpha = 0.7f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            color = Navy2,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(p.title, color = Cream, fontSize = 20.sp, fontFamily = FontFamily.Serif)
                Text(p.body, color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                Text(
                    "Venue and wallet Confirm stay with you. green-mintz never presses Cash App Confirm.",
                    color = Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { CoachStore.dismissPending() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) { Text("Cancel", color = Cream) }
                    Button(
                        onClick = { CoachStore.confirmPending() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    ) { Text("Confirm") }
                }
            }
        }
    }
}

@Composable
internal fun Warn(text: String) {
    Text(
        text,
        color = Cream,
        fontSize = 14.sp,
        modifier = Modifier
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(WarnBg)
            .padding(12.dp)
            .fillMaxWidth(),
    )
}

@Composable
internal fun Toggle(label: String, on: Boolean, modifier: Modifier = Modifier, click: () -> Unit) {
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (on) Teal else Navy2)
            .clickable(onClick = click)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) { Text(label, color = if (on) Cream else Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
}

@Composable
internal fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Navy2)
            .clickable { onChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Cream, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
internal fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Cream,
    unfocusedTextColor = Cream,
    focusedBorderColor = Teal,
    unfocusedBorderColor = Cream.copy(alpha = 0.18f),
    cursorColor = Teal,
    focusedContainerColor = Navy2,
    unfocusedContainerColor = Navy2,
)
