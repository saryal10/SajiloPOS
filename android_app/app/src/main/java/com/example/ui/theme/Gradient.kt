package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Reusable gradients. Kept out of composables so they can be created once and
 * passed to any modifier (background, border, progress arcs, icon chips).
 */
object PosGradients {

    /** Signature brand gradient for primary actions: teal to emerald. */
    val brand: Brush
        get() = Brush.linearGradient(listOf(VividTeal, VividEmerald))

    /** Hero wash used on the Insights header. */
    val hero: Brush
        get() = Brush.linearGradient(listOf(VividIndigo, VividViolet, VividPink))

    /** Warm gradient for accent CTAs. */
    val warm: Brush
        get() = Brush.linearGradient(listOf(VividAmber, VividCoral))

    /** Cool gradient for informational surfaces. */
    val cool: Brush
        get() = Brush.linearGradient(listOf(VividSky, VividIndigo))

    /** Spectrum arc for the ring gauge. */
    val spectrum: Brush
        get() = Brush.sweepGradient(
            listOf(VividEmerald, VividSky, VividIndigo, VividViolet, VividPink, VividRose, VividAmber, VividEmerald)
        )

    /** Chart area fill: coloured at the top, fading to nothing. */
    fun area(color: Color): Brush =
        Brush.verticalGradient(listOf(color.copy(alpha = 0.34f), color.copy(alpha = 0.02f)))

    /** Background wash so the page itself carries colour. */
    fun page(): Brush = Brush.linearGradient(
        listOf(PaperMist, PaperDepth.copy(alpha = 0.6f), Color(0xFFFDF4F0))
    )
}
