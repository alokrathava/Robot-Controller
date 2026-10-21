package com.alokrathava.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.alokrathava.sdk.model.LiveRobotPosition
import com.alokrathava.sdk.model.Pose2D
import com.alokrathava.sdk.model.PositionAvailability
import com.alokrathava.sdk.model.RobotOccupancyGrid
import com.alokrathava.theme.MonochromeButton
import com.alokrathava.theme.MonochromeButtonSize
import com.alokrathava.theme.MonochromeButtonVariant
import com.alokrathava.theme.MonochromeStatusPill
import com.alokrathava.theme.MonochromeTheme
import com.alokrathava.theme.StatusLevel
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.launch

/** Renders received robot positions and occupancy cells through one map transform. */
@Composable
fun MapFloorPlanCanvas(
    occupancyGrid: RobotOccupancyGrid?,
    modifier: Modifier = Modifier,
    isThumbnail: Boolean = false,
    livePosition: LiveRobotPosition = LiveRobotPosition(),
    targetPose: Pose2D? = null,
    onMapTap: ((worldX: Double, worldY: Double) -> Unit)? = null
) {
    val colors = MonochromeTheme.colors
    val spacing = MonochromeTheme.spacing
    val shapes = MonochromeTheme.shapes
    val grid = occupancyGrid?.takeIf(::isRenderableGrid)
    val background = if (colors.isDark) colors.surface else colors.interactiveSurface
    if (grid == null) {
        Box(modifier.clip(if (isThumbnail) shapes.inputs else shapes.largeCards).background(background), contentAlignment = Alignment.Center) {
            Text("No Occupancy Grid Data Available", style = MonochromeTheme.typography.bodySmall, color = colors.mutedText)
            if (!isThumbnail) PositionInfo(livePosition, Modifier.align(Alignment.BottomStart).padding(spacing.space3))
        }
        return
    }

    // Keep manual pan and zoom through SLAM grid growth, resetting only for a different map.
    val viewportKey = listOf(grid.mapId, livePosition.activeMap)
    val geometryKey = listOf(grid.mapId, grid.originX, grid.originY, grid.originYaw, grid.resolutionMetersPerCell)
    var zoom by remember(viewportKey) { mutableFloatStateOf(1f) }
    var panX by remember(viewportKey) { mutableFloatStateOf(0f) }
    var panY by remember(viewportKey) { mutableFloatStateOf(0f) }
    var follow by rememberSaveable(grid.mapId) { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    // Telemetry's active-map label may differ from the streamed grid ID during SLAM.
    val gridPose = livePosition.pose?.takeIf { livePosition.availability != PositionAvailability.UNAVAILABLE }
        ?.let { worldToGrid(grid, it.xMeters, it.yMeters) }
        ?.takeIf { it.x in 0f..grid.widthCells.toFloat() && it.y in 0f..grid.heightCells.toFloat() }
    val canFollow = gridPose != null && livePosition.availability == PositionAvailability.FRESH
    val scale = if (canvasSize.width > 0 && canvasSize.height > 0)
        min(canvasSize.width.toFloat() / grid.widthCells, canvasSize.height.toFloat() / grid.heightCells) * 0.85f * zoom else 0f
    val followX = gridPose?.let { -(it.x - grid.widthCells / 2f) * scale } ?: panX
    val followY = gridPose?.let { (it.y - grid.heightCells / 2f) * scale } ?: panY
    val animatedFollowX by animateFloatAsState(if (follow && canFollow) followX else panX, tween(220), label = "Follow X")
    val animatedFollowY by animateFloatAsState(if (follow && canFollow) followY else panY, tween(220), label = "Follow Y")
    val viewPanX = if (follow && canFollow) animatedFollowX else panX
    val viewPanY = if (follow && canFollow) animatedFollowY else panY
    val latestViewPanX by rememberUpdatedState(viewPanX)
    val latestViewPanY by rememberUpdatedState(viewPanY)
    val latestTap by rememberUpdatedState(onMapTap)

    // A stale or unavailable pose suspends following at the last visible viewport.
    LaunchedEffect(canFollow) {
        if (!canFollow && follow) {
            panX = animatedFollowX
            panY = animatedFollowY
            follow = false
        }
    }

    // A new motion epoch snaps; consecutive real samples animate in grid coordinates.
    val animationKey = listOf(geometryKey, livePosition.motionEpoch, gridPose != null)
    val animatedX = remember(animationKey) { Animatable(gridPose?.x ?: 0f) }
    val animatedY = remember(animationKey) { Animatable(gridPose?.y ?: 0f) }
    val animatedYaw = remember(animationKey) { Animatable(((livePosition.pose?.yawRadians ?: 0.0) - grid.originYaw).toFloat()) }
    LaunchedEffect(gridPose, livePosition.pose?.yawRadians, livePosition.availability, animationKey) {
        if (gridPose != null) {
            if (livePosition.availability == PositionAvailability.FRESH) {
                launch { animatedX.animateTo(gridPose.x, tween(220)) }
                launch { animatedY.animateTo(gridPose.y, tween(220)) }
                val target = (livePosition.pose!!.yawRadians - grid.originYaw).toFloat()
                val delta = ((target - animatedYaw.value + PI) % (2 * PI) + 2 * PI) % (2 * PI) - PI
                launch { animatedYaw.animateTo(animatedYaw.value + delta.toFloat(), tween(220)) }
            } else {
                animatedX.snapTo(gridPose.x)
                animatedY.snapTo(gridPose.y)
                animatedYaw.snapTo((livePosition.pose!!.yawRadians - grid.originYaw).toFloat())
            }
        }
    }

    Box(modifier.clip(if (isThumbnail) shapes.inputs else shapes.largeCards).background(background)) {
        Canvas(
            Modifier.fillMaxSize().onSizeChanged { canvasSize = it }
                .pointerInput(isThumbnail, grid.mapId) {
                    if (!isThumbnail) detectTransformGestures { centroid, pan, gestureZoom, _ ->
                        // Touch suspends follow and keeps the pinch centroid fixed while zooming.
                        if (follow) {
                            panX = latestViewPanX
                            panY = latestViewPanY
                            follow = false
                        }
                        val nextZoom = (zoom * gestureZoom).coerceIn(0.5f, 5f)
                        val factor = nextZoom / zoom
                        panX += (centroid.x - size.width / 2f - panX) * (1f - factor) + pan.x
                        panY += (centroid.y - size.height / 2f - panY) * (1f - factor) + pan.y
                        zoom = nextZoom
                    }
                }
                .pointerInput(isThumbnail, grid.mapId, grid.originX, grid.originY, grid.originYaw, grid.resolutionMetersPerCell, grid.widthCells, grid.heightCells) {
                    if (!isThumbnail) detectTapGestures { tap ->
                        // Taps invert the same viewport and origin-yaw transform as the marker.
                        val cellScale = min(size.width.toFloat() / grid.widthCells, size.height.toFloat() / grid.heightCells) * 0.85f * zoom
                        if (cellScale > 0f && cellScale.isFinite()) {
                            val left = size.width / 2f + latestViewPanX - grid.widthCells * cellScale / 2f
                            val bottom = size.height / 2f + latestViewPanY + grid.heightCells * cellScale / 2f
                            val col = (tap.x - left) / cellScale
                            val row = (bottom - tap.y) / cellScale
                            if (col in 0f..grid.widthCells.toFloat() && row in 0f..grid.heightCells.toFloat()) {
                                val world = gridToWorld(grid, col.toDouble(), row.toDouble())
                                latestTap?.invoke(world.first, world.second)
                            }
                        }
                    }
                }
        ) {
            val cellScale = min(size.width / grid.widthCells, size.height / grid.heightCells) * 0.85f * zoom
            val left = size.width / 2f + viewPanX - grid.widthCells * cellScale / 2f
            val bottom = size.height / 2f + viewPanY + grid.heightCells * cellScale / 2f

            // Occupancy bytes are grid-local, so the grid itself stays axis aligned.
            val step = if (isThumbnail || grid.widthCells > 200) 2 else 1
            for (row in 0 until grid.heightCells step step) for (col in 0 until grid.widthCells step step) {
                val value = grid.data[row * grid.widthCells + col].toInt()
                val color = when {
                    value > 50 -> colors.primaryText
                    value < 0 -> colors.defaultBorder
                    else -> null
                }
                if (color != null) drawRect(
                    color, Offset(left + col * cellScale, bottom - (row + step) * cellScale),
                    Size(cellScale * step, cellScale * step)
                )
            }

            // A segment contains only received poses from one continuous stretch.
            if (!isThumbnail) livePosition.trailSegments.forEach { segment ->
                val points = segment.mapNotNull { worldToGrid(grid, it.xMeters, it.yMeters) }
                    .filter { it.x in 0f..grid.widthCells.toFloat() && it.y in 0f..grid.heightCells.toFloat() }
                if (points.size > 1) {
                    val path = Path().apply {
                        moveTo(left + points.first().x * cellScale, bottom - points.first().y * cellScale)
                        points.drop(1).forEach { lineTo(left + it.x * cellScale, bottom - it.y * cellScale) }
                    }
                    drawPath(path, colors.secondaryText.copy(alpha = 0.55f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                }
            }

            // The nose points along the robot yaw relative to the grid origin yaw.
            if (gridPose != null) {
                val center = Offset(left + animatedX.value * cellScale, bottom - animatedY.value * cellScale)
                val radius = if (isThumbnail) 8.dp.toPx() else 12.dp.toPx()
                val alpha = if (livePosition.availability == PositionAvailability.FRESH) 1f else 0.35f
                val nose = Offset(cos(animatedYaw.value), -sin(animatedYaw.value))
                val side = Offset(-nose.y, nose.x)
                drawCircle(colors.primaryText.copy(alpha = alpha * 0.12f), radius * 2f, center)
                drawCircle(colors.surface.copy(alpha = alpha), radius * 1.1f, center)
                drawCircle(colors.primaryText.copy(alpha = alpha), radius * 1.1f, center, style = Stroke(width = 2.dp.toPx()))
                val marker = Path().apply {
                    moveTo(center.x + nose.x * radius * 1.35f, center.y + nose.y * radius * 1.35f)
                    lineTo(center.x - nose.x * radius * 0.7f + side.x * radius * 0.65f, center.y - nose.y * radius * 0.7f + side.y * radius * 0.65f)
                    lineTo(center.x - nose.x * radius * 0.7f - side.x * radius * 0.65f, center.y - nose.y * radius * 0.7f - side.y * radius * 0.65f)
                    close()
                }
                drawPath(marker, colors.primaryText.copy(alpha = alpha))
            }

            // The destination stays visually separate from the travelled path.
            targetPose?.let { target -> worldToGrid(grid, target.xMeters, target.yMeters)?.let { point ->
                if (point.x in 0f..grid.widthCells.toFloat() && point.y in 0f..grid.heightCells.toFloat()) {
                    val center = Offset(left + point.x * cellScale, bottom - point.y * cellScale)
                    drawCircle(colors.focusBorder, 12.dp.toPx(), center, style = Stroke(width = 3.dp.toPx()))
                    drawCircle(colors.primaryText, 5.dp.toPx(), center)
                }
            } }
        }
        if (!isThumbnail) {
            // Follow is available only with a fresh pose on this grid.
            MonochromeButton(
                onClick = {
                    if (follow) {
                        panX = viewPanX
                        panY = viewPanY
                    }
                    follow = !follow
                },
                text = if (follow) "Following" else "Follow Robot",
                enabled = canFollow,
                variant = MonochromeButtonVariant.Secondary,
                size = MonochromeButtonSize.Large,
                modifier = Modifier.align(Alignment.TopEnd).padding(spacing.space3)
            )
            PositionInfo(livePosition, Modifier.align(Alignment.BottomStart).padding(spacing.space3))
        }
    }
}

/** Applies the inverse map-origin rotation and resolution to a world coordinate. */
private fun worldToGrid(grid: RobotOccupancyGrid, x: Double, y: Double): Offset? {
    if (!x.isFinite() || !y.isFinite()) return null
    val dx = x - grid.originX
    val dy = y - grid.originY
    val c = cos(grid.originYaw)
    val s = sin(grid.originYaw)
    val col = (c * dx + s * dy) / grid.resolutionMetersPerCell
    val row = (-s * dx + c * dy) / grid.resolutionMetersPerCell
    return if (col.isFinite() && row.isFinite() && col in -Float.MAX_VALUE..Float.MAX_VALUE && row in -Float.MAX_VALUE..Float.MAX_VALUE)
        Offset(col.toFloat(), row.toFloat()) else null
}

/** Applies the forward map-origin rotation to a tapped grid cell. */
private fun gridToWorld(grid: RobotOccupancyGrid, col: Double, row: Double): Pair<Double, Double> {
    val localX = col * grid.resolutionMetersPerCell
    val localY = row * grid.resolutionMetersPerCell
    val c = cos(grid.originYaw)
    val s = sin(grid.originYaw)
    return Pair(grid.originX + c * localX - s * localY, grid.originY + s * localX + c * localY)
}

/** Rejects incomplete grids before indexing cells or dividing by resolution. */
private fun isRenderableGrid(grid: RobotOccupancyGrid): Boolean =
    grid.widthCells > 0 && grid.heightCells > 0 &&
        grid.data.size.toLong() == grid.widthCells.toLong() * grid.heightCells &&
        grid.resolutionMetersPerCell.isFinite() && grid.resolutionMetersPerCell > 0.0 &&
        grid.originX.isFinite() && grid.originY.isFinite() && grid.originYaw.isFinite()

/** Labels measured position details and never presents stale speed as live. */
@Composable
private fun PositionInfo(position: LiveRobotPosition, modifier: Modifier = Modifier) {
    val colors = MonochromeTheme.colors
    val spacing = MonochromeTheme.spacing
    val shapes = MonochromeTheme.shapes
    val typography = MonochromeTheme.typography
    val pose = position.pose?.takeIf { position.availability != PositionAvailability.UNAVAILABLE }
    val label = when (position.availability) {
        PositionAvailability.FRESH -> "Live position"
        PositionAvailability.STALE -> "Stale · last known"
        PositionAvailability.UNAVAILABLE -> "Position unavailable"
    }
    Column(modifier.background(colors.surface.copy(alpha = 0.94f), shapes.cards)
        .border(1.dp, colors.defaultBorder, shapes.cards).padding(spacing.space3)) {
        MonochromeStatusPill(label, level = if (position.availability == PositionAvailability.FRESH) StatusLevel.Active else StatusLevel.Inactive)
        Text("X ${pose?.xMeters?.let { String.format(Locale.US, "%.2f m", it) } ?: "—"}   Y ${pose?.yMeters?.let { String.format(Locale.US, "%.2f m", it) } ?: "—"}",
            style = typography.bodySmall, color = colors.primaryText)
        Text("Heading ${pose?.yawRadians?.let { String.format(Locale.US, "%.1f°", Math.toDegrees(it)) } ?: "—"}   Speed ${if (position.availability == PositionAvailability.FRESH) position.linearVelocityMps?.let { String.format(Locale.US, "%.2f m/s", it) } ?: "—" else "—"}",
            style = typography.bodySmall, color = colors.secondaryText)
        Text("Localization ${position.localizationState}", style = typography.caption, color = colors.secondaryText)
    }
}
