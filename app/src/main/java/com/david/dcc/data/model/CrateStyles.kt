package com.david.dcc.data.model

enum class TechnoStyle(val id: String, val displayName: String, val colorHex: Long) {
    TECHNO_HOUSE("technohouse", "Techno House", 0xFF1E88E5L),
    HARD_TECHNO("hardtechno", "Hard Techno", 0xFFD32F2FL),
    HARD_BOUNCE("hardbounce", "Hard Bounce", 0xFFFF7043L),
    HARD_GROOVE("hardgroove", "Hard Groove", 0xFF43A047L),
    PSYTRANCE("psytrance", "Psytrance", 0xFF8E24AAL),
    ACID_TECHNO("acidtechno", "Acid Techno", 0xFFFFC107L),
    INDUSTRIAL("industrial", "Industrial Techno", 0xFF546E7AL),
    MELODIC("melodic", "Melodic Techno", 0xFF5E35B1L);

    companion object {
        fun fromId(id: String?): TechnoStyle? =
            values().firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}