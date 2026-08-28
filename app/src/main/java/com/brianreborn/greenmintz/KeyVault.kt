package com.brianreborn.greenmintz

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import mintz.domain.CdpKey
import mintz.domain.parseCdpPaste

object KeyVault {
    private const val PREFS = "mintz_vault"
    private const val K_NAME = "cdp_name"
    private const val K_PEM = "cdp_pem"
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        val alias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        prefs = EncryptedSharedPreferences.create(
            PREFS,
            alias,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun savePaste(raw: String): CdpKey {
        val key = parseCdpPaste(raw) ?: error("Paste the CDP JSON (name + privateKey). Transfer must stay off.")
        save(key)
        return key
    }

    fun save(key: CdpKey) {
        prefs?.edit()?.putString(K_NAME, key.name)?.putString(K_PEM, key.privateKeyPem)?.apply()
            ?: error("vault not ready")
    }

    fun load(): CdpKey? {
        val p = prefs ?: return null
        val name = p.getString(K_NAME, null) ?: return null
        val pem = p.getString(K_PEM, null) ?: return null
        if (name.isBlank() || pem.isBlank()) return null
        return CdpKey(name, pem)
    }

    fun clear() {
        prefs?.edit()?.remove(K_NAME)?.remove(K_PEM)?.apply()
    }

    fun present(): Boolean = load() != null

    fun label(): String {
        val name = load()?.name ?: return "no key on this phone"
        val tail = name.substringAfterLast('/').takeLast(8)
        return "key on this phone · $tail"
    }
}
