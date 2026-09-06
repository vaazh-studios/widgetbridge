package com.vocabloot.widgetbridge.internal

/** SHA-256 of [bytes] as 64 lowercase hex characters.  */
internal fun sha256Hex(bytes: ByteArray): String = sha256(bytes).joinToString(separator = "") { b ->
    val v = b.toInt() and 0xFF
    HEX[v ushr 4].toString() + HEX[v and 0x0F]
}

private const val HEX = "0123456789abcdef"

/** Common Kotlin has no digest in the stdlib: MessageDigest on Android, CommonCrypto on iOS. */
internal expect fun sha256(bytes: ByteArray): ByteArray
