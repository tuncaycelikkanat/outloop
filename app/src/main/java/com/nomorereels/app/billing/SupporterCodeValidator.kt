package com.nomorereels.app.billing

import java.util.Locale

/**
 * Harici bağış platformlarında (Kreosus, Buy Me a Coffee, GitHub Sponsors)
 * bağış yapan kullanıcılara gösterilen teşekkür/hoş geldin kodunu %100 çevrimdışı doğrular.
 */
object SupporterCodeValidator {

    private val VALID_CODES = setOf(
        "OUTLOOP-HERO",
        "OUTLOOP-SUPPORTER",
        "OUTLOOP-KREOSUS",
        "OUTLOOP-COFFEE",
        "OUTLOOP-SPONSOR",
        "OUTLOOP-2026",
        "OUTLOOP-STAR",
        "OUTLOOP-PRO",
        "OUTLOOP-BACKER"
    )

    /**
     * Verilen kodun geçerli olup olmadığını büyük/küçük harf ve tire duyarsız olarak denetler.
     */
    fun isValid(rawCode: String): Boolean {
        val trimmed = rawCode.trim().uppercase(Locale.ROOT)
        if (trimmed.isEmpty()) return false

        if (VALID_CODES.contains(trimmed)) return true

        // Tire ve boşluksuz karşılaştırma desteği (örn. "OUTLOOPHERO" veya "outloop hero")
        val stripped = trimmed.replace("-", "").replace(" ", "")
        return VALID_CODES.any { it.replace("-", "") == stripped }
    }
}
