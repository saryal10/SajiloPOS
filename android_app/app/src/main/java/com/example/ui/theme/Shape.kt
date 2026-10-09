package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val PosShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Consistent spacing ladder — the backbone of the airy layout. */
object PosSpace {
    val hairline = 2.dp
    val xxs = 4.dp
    val xs = 10.dp
    val sm = 14.dp
    val md = 20.dp
    val lg = 24.dp
    val xl = 28.dp
    val xxl = 40.dp
    val huge = 56.dp
}

/** Screen gutter used by every top-level destination. */
val ScreenGutter = 24.dp

/** Card corner + border width used by PosCard. */
val CardRadius = 20.dp
val CardBorderWidth = 1.dp