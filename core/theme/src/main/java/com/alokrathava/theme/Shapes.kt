package com.alokrathava.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class MonochromeShapes(
    val smallControls: CornerBasedShape = RoundedCornerShape(6.dp),
    val inputs: CornerBasedShape = RoundedCornerShape(8.dp),
    val buttons: CornerBasedShape = RoundedCornerShape(8.dp),
    val cards: CornerBasedShape = RoundedCornerShape(12.dp),
    val largeCards: CornerBasedShape = RoundedCornerShape(16.dp),
    val dialogs: CornerBasedShape = RoundedCornerShape(16.dp),
    val pills: CornerBasedShape = CircleShape,
    val default: CornerBasedShape = RoundedCornerShape(8.dp)
)

val DefaultMonochromeShapes = MonochromeShapes()

val LocalMonochromeShapes = staticCompositionLocalOf { DefaultMonochromeShapes }

// Material3 Shapes mapping
val Material3Shapes = Shapes(
    extraSmall = DefaultMonochromeShapes.smallControls,
    small = DefaultMonochromeShapes.buttons,
    medium = DefaultMonochromeShapes.cards,
    large = DefaultMonochromeShapes.largeCards,
    extraLarge = DefaultMonochromeShapes.dialogs
)
