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
val Ink = Color(0xFF12202B)
val InkMuted = Color(0xFF63737F)
val InkFaint = Color(0xFF95A3AE)

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