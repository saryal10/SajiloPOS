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
 * Type scale tuned for POS ergonomics: large money figures, calm body copy,
 * wide generous line heights and slightly negative tracking on headings.
 */
object PosType {

    val displayLarge = Manrope(40, 46, FontWeight.ExtraBold, (-0.8))
    val displayMedium = Manrope(34, 40, FontWeight.ExtraBold, (-0.6))
    val displaySmall = Manrope(28, 34, FontWeight.Bold, (-0.4))

    val headlineLarge = Manrope(26, 32, FontWeight.Bold, (-0.4))
    val headlineMedium = Manrope(22, 28, FontWeight.Bold, (-0.3))
    val headlineSmall = Manrope(19, 25, FontWeight.Bold, (-0.2))

    val titleLarge = Manrope(17, 23, FontWeight.Bold, (-0.15))
    val titleMedium = Manrope(15, 21, FontWeight.SemiBold)
    val titleSmall = Inter(14, 19, FontWeight.SemiBold, 0.05)

    val bodyLarge = Inter(15, 23, FontWeight.Normal, 0.1)
    val bodyMedium = Inter(14, 21, FontWeight.Normal, 0.1)
    val bodySmall = Inter(12, 17, FontWeight.Normal, 0.15)

    val labelLarge = Inter(14, 18, FontWeight.SemiBold, 0.15)
    val labelMedium = Inter(12, 15, FontWeight.SemiBold, 0.25)
    val labelSmall = Inter(10, 14, FontWeight.SemiBold, 0.6)

    val overline = Inter(10, 14, FontWeight.Bold, 1.1)

    // Money: big, tight, tabular-feeling figures.
    val moneyLarge = Manrope(28, 32, FontWeight.ExtraBold, (-0.6))
    val moneyMedium = Manrope(21, 26, FontWeight.Bold, (-0.4))
    val moneySmall = Manrope(16, 21, FontWeight.Bold, (-0.2))
    val moneyTiny = Manrope(13, 17, FontWeight.SemiBold, 0.0)

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