package mintz.broker

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.StringReader
import java.security.SecureRandom
import java.security.interfaces.ECPrivateKey
import java.util.Date

internal fun normalizePem(pem: String): String =
    pem.replace("\\n", "\n").replace("\\r", "").trim()

internal fun loadEcPrivateKey(pem: String): ECPrivateKey {
    val parser = PEMParser(StringReader(normalizePem(pem)))
    parser.use { p ->
        val obj = p.readObject() ?: error("empty PEM")
        val converter = JcaPEMKeyConverter()
        val key = when (obj) {
            is PEMKeyPair -> converter.getKeyPair(obj).private
            is PrivateKeyInfo -> converter.getPrivateKey(obj)
            else -> error("unsupported key ${obj.javaClass.simpleName}")
        }
        return key as ECPrivateKey
    }
}

internal fun restJwt(keyName: String, pem: String, method: String, path: String, nowMs: Long = System.currentTimeMillis()): String {
    val key = loadEcPrivateKey(pem)
    val now = Date(nowMs)
    val claims = JWTClaimsSet.Builder()
        .subject(keyName)
        .issuer("cdp")
        .notBeforeTime(now)
        .expirationTime(Date(nowMs + 120_000))
        .claim("uri", "$method api.coinbase.com$path")
        .build()
    val nonce = ByteArray(16).also { SecureRandom().nextBytes(it) }
        .joinToString("") { b -> "%02x".format(b) }
    val header = JWSHeader.Builder(JWSAlgorithm.ES256)
        .keyID(keyName)
        .type(JOSEObjectType.JWT)
        .customParam("nonce", nonce)
        .build()
    val jwt = SignedJWT(header, claims)
    jwt.sign(ECDSASigner(key))
    return jwt.serialize()
}
