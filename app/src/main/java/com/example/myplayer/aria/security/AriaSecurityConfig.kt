package com.example.myplayer.aria.security

object AriaSecurityConfig {
    /**
     * Map of package names allowed to bind and consume the ARIA Music SDK API,
     * mapped to their authorized certificate SHA-256 hashes (colon-separated uppercase).
     * Includes placeholders for debug builds and production release builds of the ARIA ecosystem.
     */
    val TRUSTED_PACKAGES = mapOf(
        "com.flexfly.ultron" to listOf(
            "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99", // Prod certificate
            "BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA"  // Debug certificate
        ),
        "com.example.myplayer" to listOf(
            "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99"  // Own prod signing cert
        )
    )
}
