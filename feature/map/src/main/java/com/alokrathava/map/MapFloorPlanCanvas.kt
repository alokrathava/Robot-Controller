package com.alokrathava.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.alokrathava.sdk.model.RobotPosition
import com.alokrathava.theme.MonochromeTheme
import kotlin.math.min

@Composable
fun MapFloorPlanCanvas(
    floorPlanType: FloorPlanType,
    modifier: Modifier = Modifier,
    isThumbnail: Boolean = false,
    robotPosition: RobotPosition? = null
) {
    val colors = MonochromeTheme.colors
    val containerBg = if (colors.isDark) colors.surface else colors.interactiveSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (isThumbnail) 10.dp else 16.dp))
            .background(containerBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val scaleFactor = if (isThumbnail) 0.82f else 0.88f

            val strokeThick = if (isThumbnail) 2.2f else 3.8f
            val strokeThin = if (isThumbnail) 1.0f else 1.8f
            val wallColor = colors.primaryText
            val innerWallColor = colors.secondaryText
            val lightGridColor = colors.defaultBorder

            val centerX = w / 2f
            val centerY = h / 2f
            val unit = min(w, h) * scaleFactor / 10f

            // Background subtle grid lines
            for (i in -4..4) {
                val offsetVal = i * unit * 0.9f
                drawLine(
                    color = lightGridColor,
                    start = Offset(centerX + offsetVal, centerY - 4.2f * unit),
                    end = Offset(centerX + offsetVal, centerY + 4.2f * unit),
                    strokeWidth = strokeThin * 0.5f
                )
                drawLine(
                    color = lightGridColor,
                    start = Offset(centerX - 4.2f * unit, centerY + offsetVal),
                    end = Offset(centerX + 4.2f * unit, centerY + offsetVal),
                    strokeWidth = strokeThin * 0.5f
                )
            }

            // Draw floor plan outline based on floorPlanType
            when (floorPlanType) {
                FloorPlanType.MAIN_FLOOR, FloorPlanType.TEST_MAP -> {
                    // Architectural H-shape multi-wing layout matching mockup
                    val outerPath = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = centerX - 3.8f * unit,
                                top = centerY - 3.8f * unit,
                                right = centerX + 3.8f * unit,
                                bottom = centerY + 3.8f * unit,
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        )
                    }

                    drawPath(
                        path = outerPath,
                        color = wallColor,
                        style = Stroke(width = strokeThick)
                    )

                    // Left wing
                    drawRoundRect(
                        color = wallColor,
                        topLeft = Offset(centerX - 3.2f * unit, centerY - 3.2f * unit),
                        size = Size(1.8f * unit, 6.4f * unit),
                        style = Stroke(width = strokeThick)
                    )

                    // Right wing
                    drawRoundRect(
                        color = wallColor,
                        topLeft = Offset(centerX + 1.4f * unit, centerY - 3.2f * unit),
                        size = Size(1.8f * unit, 6.4f * unit),
                        style = Stroke(width = strokeThick)
                    )

                    // Center connecting block
                    drawRoundRect(
                        color = wallColor,
                        topLeft = Offset(centerX - 1.4f * unit, centerY - 1.2f * unit),
                        size = Size(2.8f * unit, 2.4f * unit),
                        style = Stroke(width = strokeThick)
                    )

                    // Interior partitions
                    drawLine(
                        color = innerWallColor,
                        start = Offset(centerX - 3.2f * unit, centerY),
                        end = Offset(centerX - 1.4f * unit, centerY),
                        strokeWidth = strokeThin
                    )
                    drawLine(
                        color = innerWallColor,
                        start = Offset(centerX + 1.4f * unit, centerY),
                        end = Offset(centerX + 3.2f * unit, centerY),
                        strokeWidth = strokeThin
                    )
                    drawLine(
                        color = innerWallColor,
                        start = Offset(centerX, centerY - 3.8f * unit),
                        end = Offset(centerX, centerY - 1.2f * unit),
                        strokeWidth = strokeThin
                    )
                    drawLine(
                        color = innerWallColor,
                        start = Offset(centerX, centerY + 1.2f * unit),
                        end = Offset(centerX, centerY + 3.8f * unit),
                        strokeWidth = strokeThin
                    )
                }

                FloorPlanType.SECOND_FLOOR -> {
                    // Rectangular corridor layout
                    drawRoundRect(
                        color = wallColor,
                        topLeft = Offset(centerX - 3.8f * unit, centerY - 3.8f * unit),
                        size = Size(7.6f * unit, 7.6f * unit),
                        style = Stroke(width = strokeThick)
                    )
                    drawRoundRect(
                        color = wallColor,
                        topLeft = Offset(centerX - 1.6f * unit, centerY - 1.6f * unit),
                        size = Size(3.2f * unit, 3.2f * unit),
                        style = Stroke(width = strokeThick)
                    )
                    drawLine(
                        color = innerWallColor,
                        start = Offset(centerX - 3.8f * unit, centerY - 1.6f * unit),
                        end = Offset(centerX - 1.6f * unit, centerY - 1.6f * unit),
                        strokeWidth = strokeThin
                    )
                    drawLine(
                        color = innerWallColor,
                        start = Offset(centerX + 1.6f * unit, centerY - 1.6f * unit),
                        end = Offset(centerX + 3.8f * unit, centerY - 1.6f * unit),
                        strokeWidth = strokeThin
                    )
                    drawLine(
                        color = innerWallColor,
                        start = Offset(centerX - 3.8f * unit, centerY + 1.6f * unit),
                        end = Offset(centerX - 1.6f * unit, centerY + 1.6f * unit),
                        strokeWidth = strokeThin
                    )
                    drawLine(
                        color = innerWallColor,
                        start = Offset(centerX + 1.6f * unit, centerY + 1.6f * unit),
                        end = Offset(centerX + 3.8f * unit, centerY + 1.6f * unit),
                        strokeWidth = strokeThin
                    )
                }

                FloorPlanType.OUTDOOR -> {
                    // Perimeter & circular pathway loops
                    drawRoundRect(
                        color = wallColor,
                        topLeft = Offset(centerX - 4.2f * unit, centerY - 3.2f * unit),
                        size = Size(8.4f * unit, 6.4f * unit),
                        cornerRadius = CornerRadius(12f, 12f),
                        style = Stroke(width = strokeThick)
                    )
                    drawCircle(
                        color = wallColor,
                        center = Offset(centerX, centerY),
                        radius = 2.0f * unit,
                        style = Stroke(width = strokeThick)
                    )
                    drawLine(
                        color = innerWallColor,
                        start = Offset(centerX - 4.2f * unit, centerY),
                        end = Offset(centerX - 2.0f * unit, centerY),
                        strokeWidth = strokeThin
                    )
                    drawLine(
                        color = innerWallColor,
                        start = Offset(centerX + 2.0f * unit, centerY),
                        end = Offset(centerX + 4.2f * unit, centerY),
                        strokeWidth = strokeThin
                    )
                }
            }

            if (!isThumbnail && robotPosition != null) {
                val robotX = centerX + (robotPosition.x.toFloat() * unit * 0.5f)
                val robotY = centerY - (robotPosition.y.toFloat() * unit * 0.5f)
                val robotRadius = 12f

                drawCircle(
                    color = Color(0x4422C55E),
                    center = Offset(robotX, robotY),
                    radius = robotRadius * 2.2f
                )
                drawCircle(
                    color = Color(0xFF22C55E),
                    center = Offset(robotX, robotY),
                    radius = robotRadius
                )
                drawCircle(
                    color = Color.White,
                    center = Offset(robotX, robotY),
                    radius = robotRadius * 0.4f
                )
            }
        }
    }
}
