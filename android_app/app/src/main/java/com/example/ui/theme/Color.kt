package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// -----------------------------------------------------------------------------
// SajiloPOS design tokens — "Himalayan Paper" palette.
// Light, airy and high-legibility for fast counter use, with a single confident
// brand green and a warm marigold accent.
// -----------------------------------------------------------------------------

// Brand — Himalayan pine green
val BrandGreen = Color(0xFF0E7C63)
val BrandGreenDeep = Color(0xFF0A5D4C)
val BrandGreenSoft = Color(0xFF3E9C85)
val BrandGreenTint = Color(0xFFE1F1EC)
val BrandGreenMist = Color(0xFFF1F8F6)

// Accent — marigold / amber (cash, highlights, warnings)
val AccentAmber = Color(0xFFE9A13B)
val AccentAmberDeep = Color(0xFFB87614)
val AccentAmberTint = Color(0xFFFDF2E1)
val AccentAmberWash = Color(0xFFFEFAF3)

// Neutrals — cool paper
val Paper = Color(0xFFF7F9FA)
val PaperAlt = Color(0xFFEFF3F5)
val CardWhite = Color(0xFFFFFFFF)
val LineSubtle = Color(0xFFE7ECEF)
val LineStrong = Color(0xFFD7E0E5)
val Ink = Color(0xFF0B0C0E)
val InkMuted = Color(0xFF636366)
val InkFaint = Color(0xFFAEAEB2)

// Dark theme neutrals
val NightPaper = Color(0xFF0D1519)
val NightSurface = Color(0xFF141F24)
val NightSurfaceAlt = Color(0xFF1C2A31)
val NightLine = Color(0xFF27383F)
val NightInk = Color(0xFFEAF1F4)
val NightInkMuted = Color(0xFFA3B3BC)

// Status
val SuccessGreen = Color(0xFF11845A)
val SuccessTint = Color(0xFFE4F5ED)
val WarningOrange = Color(0xFFD07C1F)
val WarningTint = Color(0xFFFDF1E1)
val DangerRed = Color(0xFFC33A32)
val DangerTint = Color(0xFFFBEBE9)
val InfoBlue = Color(0xFF2F6BC4)
val InfoTint = Color(0xFFEAF1FC)
val VioletPurple = Color(0xFF6D4AC4)
val VioletTint = Color(0xFFF0ECFB)

// Digital wallet brand colours (Nepal)
val ESewaGreen = Color(0xFF5BB13F)
val FonepayRed = Color(0xFFCC2E2E)
val KhaltiPurple = Color(0xFF5B36AE)

// Stock / analytics semantic aliases
val StockNormalGreen = SuccessGreen
val StockLowOrange = WarningOrange
val StockCriticalRed = DangerRed

// Scanner (camera viewfinder is always dark, regardless of app theme)
val ScannerSurface = Color(0xFF0B1418)
val ScannerSurfaceAlt = Color(0xFF04090C)
val ScannerMuted = Color(0xFF93A3AC)

// Thermal receipt paper
val ThermalPaperBg = Color(0xFFFCFDFE)
val ThermalReceiptInk = Color(0xFF1B2730)
val ThermalReceiptFaint = Color(0xFF8A99A3)
// -----------------------------------------------------------------------------
// Vivid spectrum — used for category identity, quick actions and chart series so
// the app reads as colourful while staying legible in daylight.
// -----------------------------------------------------------------------------
val VividIndigo = Color(0xFF6366F1)
val VividViolet = Color(0xFF8B5CF6)
val VividPink = Color(0xFFEC4899)
val VividRose = Color(0xFFF43F5E)
val VividCoral = Color(0xFFF97316)
val VividAmber = Color(0xFFE9A13B)
val VividLime = Color(0xFF65A30D)
val VividEmerald = Color(0xFF10B981)
val VividSky = Color(0xFF0EA5E9)
val VividCyan = Color(0xFF06B6D4)
val VividTeal = Color(0xFF0E7C63)

// Matching pastel tints for icon chips and row backgrounds
val TintIndigo = Color(0xFFEDEAFE)
val TintViolet = Color(0xFFF2E9FE)
val TintPink = Color(0xFFFDE8F3)
val TintRose = Color(0xFFFDE8EC)
val TintCoral = Color(0xFFFEEBE0)
val TintAmber = Color(0xFFFDF2E1)
val TintLime = Color(0xFFEFF6E0)
val TintEmerald = Color(0xFFDEF5EA)
val TintSky = Color(0xFFE2F1FD)
val TintCyan = Color(0xFFDFF5F8)
val TintTeal = Color(0xFFE1F1EC)

// Neutral Apple-style page background: near-white gray, hairline borders,
// near-black ink. Colour now comes from accents, not the canvas.
val PaperMist = Color(0xFFF4F4F6)
val PaperDepth = Color(0xFFECECF0)
val LineMist = Color(0xFFE8E8ED)
val LineStrongMist = Color(0xFFD9D9DF)

/** A category accent pair (vivid + matching tint). */
data class AccentPair(val strong: Color, val tint: Color)

private val CATEGORY_ACCENTS: List<AccentPair> = listOf(
    AccentPair(VividIndigo, TintIndigo),
    AccentPair(VividEmerald, TintEmerald),
    AccentPair(VividCoral, TintCoral),
    AccentPair(VividSky, TintSky),
    AccentPair(VividPink, TintPink),
    AccentPair(VividAmber, TintAmber),
    AccentPair(VividViolet, TintViolet),
    AccentPair(VividCyan, TintCyan),
    AccentPair(VividLime, TintLime),
    AccentPair(VividRose, TintRose)
)

/**
 * Explicit category colours so "Dairy" is always the same hue on every screen —
 * a merchant learns the colours instead of re-reading them. Unknown categories
 * fall back to a stable hash.
 */
private val CATEGORY_COLOURS: Map<String, AccentPair> = mapOf(
    "snacks" to AccentPair(VividAmber, TintAmber),
    "dairy" to AccentPair(VividSky, TintSky),
    "beverages" to AccentPair(VividCyan, TintCyan),
    "grains" to AccentPair(VividAmber, TintAmber),
    "groceries" to AccentPair(VividEmerald, TintEmerald),
    "spices" to AccentPair(VividCoral, TintCoral),
    "household" to AccentPair(VividIndigo, TintIndigo),
    "personal care" to AccentPair(VividPink, TintPink),
    "produce" to AccentPair(VividLime, TintLime),
    "bakery" to AccentPair(VividRose, TintRose),
    "frozen" to AccentPair(VividSky, TintSky)
)

fun accentFor(key: String): AccentPair {
    if (key.isBlank()) return CATEGORY_ACCENTS.first()
    CATEGORY_COLOURS[key.trim().lowercase()]?.let { return it }
    var hash = 7
    key.lowercase().forEach { hash = (hash * 31 + it.code) and 0x7FFFFFFF }
    return CATEGORY_ACCENTS[hash % CATEGORY_ACCENTS.size]
}

/** Full vivid spectrum, for chart series and decorative gradients. */
val PosSpectrum: List<Color> = listOf(
    VividIndigo, VividViolet, VividPink, VividCoral, VividAmber, VividLime,
    VividEmerald, VividSky, VividCyan, VividRose
)
