package com.example.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.example.R

// Bundled variable fonts (fully offline, no runtime download).
// Inter -> interface text, long-form copy, numerals.
// Manrope -> headings, money and display numbers (geometric, confident).
val InterFamily = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium),
    Font(R.font.inter_variable, FontWeight.SemiBold),
    Font(R.font.inter_variable, FontWeight.Bold)
)

val ManropeFamily = FontFamily(
    Font(R.font.manrope_variable, FontWeight.Medium),
    Font(R.font.manrope_variable, FontWeight.SemiBold),
    Font(R.font.manrope_variable, FontWeight.Bold),
    Font(R.font.manrope_variable, FontWeight.ExtraBold)
)

private val TrimmedLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private fun Manrope(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Double = 0.0
) = TextStyle(
    fontFamily = ManropeFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    lineHeightStyle = TrimmedLineHeight
)

private fun Inter(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Double = 0.0
) = TextStyle(
    fontFamily = InterFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    lineHeightStyle = TrimmedLineHeight
)

/**
 * Type scale tuned for high-contrast, Apple-grade legibility: oversized
 * ExtraBold display and money figures with tight tracking, calm body copy.
 */
object PosType {

    val displayLarge = Manrope(46, 52, FontWeight.ExtraBold, (-1.0))
    val displayMedium = Manrope(40, 46, FontWeight.ExtraBold, (-0.8))
    val displaySmall = Manrope(34, 40, FontWeight.ExtraBold, (-0.6))

    val headlineLarge = Manrope(30, 36, FontWeight.ExtraBold, (-0.5))
    val headlineMedium = Manrope(26, 32, FontWeight.ExtraBold, (-0.4))
    val headlineSmall = Manrope(22, 28, FontWeight.Bold, (-0.3))

    val titleLarge = Manrope(19, 25, FontWeight.Bold, (-0.2))
    val titleMedium = Manrope(16, 22, FontWeight.Bold, (-0.1))
    val titleSmall = Inter(15, 20, FontWeight.Bold, 0.0)

    val bodyLarge = Inter(16, 24, FontWeight.Normal, 0.1)
    val bodyMedium = Inter(15, 22, FontWeight.Normal, 0.1)
    val bodySmall = Inter(13, 18, FontWeight.Normal, 0.15)

    val labelLarge = Inter(15, 19, FontWeight.Bold, 0.1)
    val labelMedium = Inter(13, 17, FontWeight.Bold, 0.2)
    val labelSmall = Inter(11, 15, FontWeight.Bold, 0.5)

    val overline = Inter(10, 14, FontWeight.Bold, 1.1)

    // Money: big, tight, tabular-feeling figures.
    val moneyLarge = Manrope(34, 38, FontWeight.ExtraBold, (-0.8))
    val moneyMedium = Manrope(24, 30, FontWeight.Bold, (-0.5))
    val moneySmall = Manrope(18, 23, FontWeight.Bold, (-0.2))
    val moneyTiny = Manrope(14, 18, FontWeight.SemiBold, 0.0)

    // Thermal printer / invoice style — monospace, 1:1 rhythm.
    val receipt = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    )
    val receiptStrong = receipt.copy(fontWeight = FontWeight.Bold)
}