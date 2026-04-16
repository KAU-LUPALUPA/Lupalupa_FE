package com.example.lupapj.ui.scene

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.lupapj.R
import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.scene.DefaultWallPivot
import com.example.lupapj.data.model.scene.IsoRoomProjectionSpec
import com.example.lupapj.data.model.scene.SceneObjectDefinition
import com.example.lupapj.data.model.scene.WallAnchor
import com.example.lupapj.data.model.scene.WallFace
import com.example.lupapj.ui.theme.LupaPJTheme
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val FLOOR_HEIGHT_RATIO = 216f / 196f
private const val WALL_HORIZONTAL_OVERLAP_FRACTION = 0.010f
private const val WALL_VERTICAL_OVERLAP_FRACTION = 0.014f
private const val FLOOR_TILE_OVERLAP_TILES = 0.04f

data class RoomBackgroundSpec(
    val roomColumns: Int = 3,
    val wallMiddleRows: Int = 1,
    val floorRows: Int = 1
)

private data class SurfaceQuadPx(
    val topStart: ScreenPointPx,
    val topEnd: ScreenPointPx,
    val bottomEnd: ScreenPointPx,
    val bottomStart: ScreenPointPx
)

private data class WallSurfaceStyle(
    val renderColumns: Int,
    val overlayBrush: Brush
)

@Composable
fun RoomBackground(
    projectionSpec: IsoRoomProjectionSpec,
    modifier: Modifier = Modifier,
    spec: RoomBackgroundSpec = RoomBackgroundSpec(),
    wallDecor: List<SceneObjectDefinition> = emptyList(),
    sideWallFace: WallFace = WallFace.RIGHT,
    highlightFloor: Boolean = false
) {
    val resources = LocalContext.current.resources
    val density = LocalDensity.current
    val wallTopStrip = remember(resources) {
        BitmapFactory.decodeResource(resources, R.drawable.wall_top_strip).asImageBitmap()
    }
    val wallMiddleTile = remember(resources) {
        BitmapFactory.decodeResource(resources, R.drawable.wall_middle_tile).asImageBitmap()
    }
    val wallBottomStrip = remember(resources) {
        BitmapFactory.decodeResource(resources, R.drawable.wall_bottom_strip).asImageBitmap()
    }
    val floorTile = remember(resources) {
        BitmapFactory.decodeResource(resources, R.drawable.floor_tile_repeatable).asImageBitmap()
    }
    val windowObject = remember(resources) {
        BitmapFactory.decodeResource(resources, R.drawable.window_object).asImageBitmap()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5ECDD))
            .drawWithCache {
                val metrics = resolveIsoRoomMetrics(
                    viewportWidthPx = size.width,
                    viewportHeightPx = size.height,
                    projectionSpec = projectionSpec
                )
                val backWall = resolveIsoWallPanelPx(WallFace.BACK, metrics)
                val sideWall = resolveIsoWallPanelPx(sideWallFace, metrics)
                val floor = resolveIsoFloorDiamondPx(metrics)

                val backWallPath = backWall.toPath()
                val sideWallPath = sideWall.toPath()
                val floorPath = floor.toPath()

                onDrawBehind {
                    drawTiledWallPanel(
                        panel = backWall,
                        spec = spec,
                        style = resolveWallSurfaceStyle(
                            wallFace = WallFace.BACK,
                            panel = backWall,
                            roomColumns = spec.roomColumns
                        ),
                        topStrip = wallTopStrip,
                        middleTile = wallMiddleTile,
                        bottomStrip = wallBottomStrip,
                        wallDecor = wallDecor,
                        wallFace = WallFace.BACK,
                        wallImage = windowObject,
                        viewportWidthPx = size.width,
                        density = density
                    )

                    drawTiledWallPanel(
                        panel = sideWall,
                        spec = spec,
                        style = resolveWallSurfaceStyle(
                            wallFace = sideWallFace,
                            panel = sideWall,
                            roomColumns = spec.roomColumns
                        ),
                        topStrip = wallTopStrip,
                        middleTile = wallMiddleTile,
                        bottomStrip = wallBottomStrip,
                        wallDecor = wallDecor,
                        wallFace = sideWallFace,
                        wallImage = windowObject,
                        viewportWidthPx = size.width,
                        density = density
                    )

                    drawTiledFloor(
                        path = floorPath,
                        metrics = metrics,
                        floorTile = floorTile,
                        highlightFloor = highlightFloor
                    )
                }
            }
    )
}

private fun DrawScope.drawTiledWallPanel(
    panel: IsoWallPanelPx,
    spec: RoomBackgroundSpec,
    style: WallSurfaceStyle,
    topStrip: ImageBitmap,
    middleTile: ImageBitmap,
    bottomStrip: ImageBitmap,
    wallDecor: List<SceneObjectDefinition>,
    wallFace: WallFace,
    wallImage: ImageBitmap,
    viewportWidthPx: Float,
    density: androidx.compose.ui.unit.Density
) {
    val path = panel.toPath()
    val bounds = path.getBounds()
    val renderColumns = style.renderColumns.coerceAtLeast(1)
    val wallMiddleRows = spec.wallMiddleRows.coerceAtLeast(1)
    val totalHeightUnits = 24f + (wallMiddleRows * 222f) + 74f
    val columnWidth = 1f / renderColumns.toFloat()
    var currentTopUnits = 0f

    repeat(renderColumns) { columnIndex ->
        val u0 = columnIndex * columnWidth
        val u1 = (columnIndex + 1) * columnWidth
        val topBandBottom = (currentTopUnits + 24f) / totalHeightUnits
        drawProjectedImageQuad(
            image = topStrip,
            quad = resolveWallLocalQuad(
                panel = panel,
                u0 = expandStartFraction(
                    fraction = u0,
                    overlap = WALL_HORIZONTAL_OVERLAP_FRACTION,
                    isFirst = columnIndex == 0
                ),
                u1 = expandEndFraction(
                    fraction = u1,
                    overlap = WALL_HORIZONTAL_OVERLAP_FRACTION,
                    isLast = columnIndex == renderColumns - 1
                ),
                v0 = currentTopUnits / totalHeightUnits,
                v1 = expandEndFraction(
                    fraction = topBandBottom,
                    overlap = WALL_VERTICAL_OVERLAP_FRACTION,
                    isLast = wallMiddleRows == 0
                )
            )
        )
    }
    currentTopUnits += 24f

    repeat(wallMiddleRows) {
        val v0 = currentTopUnits / totalHeightUnits
        val v1 = (currentTopUnits + 222f) / totalHeightUnits
        repeat(renderColumns) { columnIndex ->
            val u0 = columnIndex * columnWidth
            val u1 = (columnIndex + 1) * columnWidth
            drawProjectedImageQuad(
                image = middleTile,
                quad = resolveWallLocalQuad(
                    panel = panel,
                    u0 = expandStartFraction(
                        fraction = u0,
                        overlap = WALL_HORIZONTAL_OVERLAP_FRACTION,
                        isFirst = columnIndex == 0
                    ),
                    u1 = expandEndFraction(
                        fraction = u1,
                        overlap = WALL_HORIZONTAL_OVERLAP_FRACTION,
                        isLast = columnIndex == renderColumns - 1
                    ),
                    v0 = expandStartFraction(
                        fraction = v0,
                        overlap = WALL_VERTICAL_OVERLAP_FRACTION,
                        isFirst = it == 0
                    ),
                    v1 = expandEndFraction(
                        fraction = v1,
                        overlap = WALL_VERTICAL_OVERLAP_FRACTION,
                        isLast = it == wallMiddleRows - 1
                    )
                )
            )
        }
        currentTopUnits += 222f
    }

    val bottomBandTop = currentTopUnits / totalHeightUnits
    repeat(renderColumns) { columnIndex ->
        val u0 = columnIndex * columnWidth
        val u1 = (columnIndex + 1) * columnWidth
        drawProjectedImageQuad(
            image = bottomStrip,
            quad = resolveWallLocalQuad(
                panel = panel,
                u0 = expandStartFraction(
                    fraction = u0,
                    overlap = WALL_HORIZONTAL_OVERLAP_FRACTION,
                    isFirst = columnIndex == 0
                ),
                u1 = expandEndFraction(
                    fraction = u1,
                    overlap = WALL_HORIZONTAL_OVERLAP_FRACTION,
                    isLast = columnIndex == renderColumns - 1
                ),
                v0 = expandStartFraction(
                    fraction = bottomBandTop,
                    overlap = WALL_VERTICAL_OVERLAP_FRACTION,
                    isFirst = wallMiddleRows == 0
                ),
                v1 = 1f
            )
        )
    }

    clipPath(path) {
        drawRect(
            brush = style.overlayBrush,
            topLeft = Offset(bounds.left, bounds.top),
            size = bounds.size
        )
    }

    wallDecor.forEach { decor ->
        val anchor = decor.anchor as? WallAnchor ?: return@forEach
        if (!isDecorOnWallFace(anchor.face, wallFace)) return@forEach
        val decorImage = when (decor.type) {
            RoomObjectType.WINDOW -> wallImage
            else -> null
        } ?: return@forEach

        resolveWallDecorQuad(
            decor = decor,
            panel = panel,
            viewportWidthPx = viewportWidthPx,
            density = density
        )?.let { quad ->
            drawProjectedImageQuad(
                image = decorImage,
                quad = quad
            )
        }
    }

    drawPath(
        path = path,
        color = Color(0x18000000),
        style = Stroke(width = 1.5f)
    )
    drawLine(
        color = Color(0x12FFFFFF),
        start = Offset(panel.topStart.xPx, panel.topStart.yPx),
        end = Offset(panel.topEnd.xPx, panel.topEnd.yPx),
        strokeWidth = 1.2f
    )
    drawLine(
        color = Color(0x18000000),
        start = Offset(panel.bottomStart.xPx, panel.bottomStart.yPx),
        end = Offset(panel.bottomEnd.xPx, panel.bottomEnd.yPx),
        strokeWidth = 1.8f
    )
}

private fun DrawScope.drawTiledFloor(
    path: Path,
    metrics: IsoRoomMetricsPx,
    floorTile: ImageBitmap,
    highlightFloor: Boolean
) {
    val bounds = path.getBounds()
    val columnCount = ceil(metrics.roomWidthTiles).toInt().coerceAtLeast(1)
    val rowCount = ceil(metrics.roomDepthTiles).toInt().coerceAtLeast(1)

    repeat(rowCount) { rowIndex ->
        repeat(columnCount) { columnIndex ->
            drawProjectedImageQuad(
                image = floorTile,
                quad = resolveFloorTileQuad(
                    metrics = metrics,
                    x0Tiles = columnIndex.toFloat(),
                    y0Tiles = rowIndex.toFloat()
                )
            )
        }
    }

    clipPath(path) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x04FFFFFF),
                    Color(0x18000000)
                ),
                startY = bounds.top,
                endY = bounds.bottom
            ),
            topLeft = Offset(bounds.left, bounds.top),
            size = bounds.size
        )

        if (highlightFloor) {
            drawRect(
                color = Color(0x18F0C96B),
                topLeft = Offset(bounds.left, bounds.top),
                size = bounds.size
            )
        }
    }

    drawPath(
        path = path,
        color = Color(0x22000000),
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawProjectedImageQuad(
    image: ImageBitmap,
    quad: SurfaceQuadPx
) {
    val androidBitmap = image.asAndroidBitmap()
    val source = floatArrayOf(
        0f, 0f,
        androidBitmap.width.toFloat(), 0f,
        androidBitmap.width.toFloat(), androidBitmap.height.toFloat(),
        0f, androidBitmap.height.toFloat()
    )
    val destination = floatArrayOf(
        quad.topStart.xPx, quad.topStart.yPx,
        quad.topEnd.xPx, quad.topEnd.yPx,
        quad.bottomEnd.xPx, quad.bottomEnd.yPx,
        quad.bottomStart.xPx, quad.bottomStart.yPx
    )
    val matrix = Matrix().apply {
        setPolyToPoly(source, 0, destination, 0, 4)
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val clipPath = quad.toPath().asAndroidPath()

    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas
        val checkpoint = nativeCanvas.save()
        nativeCanvas.clipPath(clipPath)
        nativeCanvas.concat(matrix)
        nativeCanvas.drawBitmap(androidBitmap, 0f, 0f, paint)
        nativeCanvas.restoreToCount(checkpoint)
    }
}

private fun resolveWallLocalQuad(
    panel: IsoWallPanelPx,
    u0: Float,
    u1: Float,
    v0: Float,
    v1: Float
): SurfaceQuadPx {
    return SurfaceQuadPx(
        topStart = resolveWallPanelPoint(panel, u0, v0),
        topEnd = resolveWallPanelPoint(panel, u1, v0),
        bottomEnd = resolveWallPanelPoint(panel, u1, v1),
        bottomStart = resolveWallPanelPoint(panel, u0, v1)
    )
}

private fun resolveFloorTileQuad(
    metrics: IsoRoomMetricsPx,
    x0Tiles: Float,
    y0Tiles: Float
): SurfaceQuadPx {
    val maxXTiles = metrics.roomWidthTiles
    val maxYTiles = metrics.roomDepthTiles
    val startX = (x0Tiles - FLOOR_TILE_OVERLAP_TILES).coerceIn(0f, maxXTiles)
    val endX = (x0Tiles + 1f + FLOOR_TILE_OVERLAP_TILES).coerceIn(0f, maxXTiles)
    val startY = (y0Tiles - FLOOR_TILE_OVERLAP_TILES).coerceIn(0f, maxYTiles)
    val endY = (y0Tiles + 1f + FLOOR_TILE_OVERLAP_TILES).coerceIn(0f, maxYTiles)

    return SurfaceQuadPx(
        topStart = isoToScreen(startX, startY, metrics),
        topEnd = isoToScreen(endX, startY, metrics),
        bottomEnd = isoToScreen(endX, endY, metrics),
        bottomStart = isoToScreen(startX, endY, metrics)
    )
}

private fun resolveWallPanelPoint(
    panel: IsoWallPanelPx,
    u: Float,
    v: Float
): ScreenPointPx {
    val clampedU = u.coerceIn(0f, 1f)
    val clampedV = v.coerceIn(0f, 1f)
    val leftEdge = lerpScreenPoint(panel.topStart, panel.bottomStart, clampedV)
    val rightEdge = lerpScreenPoint(panel.topEnd, panel.bottomEnd, clampedV)
    return lerpScreenPoint(leftEdge, rightEdge, clampedU)
}

private fun resolveWallDecorQuad(
    decor: SceneObjectDefinition,
    panel: IsoWallPanelPx,
    viewportWidthPx: Float,
    density: androidx.compose.ui.unit.Density
): SurfaceQuadPx? {
    val anchor = decor.anchor as? WallAnchor ?: return null
    val sprite = decor.sprite
    val spriteSizePx = resolveSpriteSizePx(
        viewportWidthPx = viewportWidthPx,
        sprite = sprite,
        minWidthPx = with(density) { sprite.minWidthDp.dp.toPx() },
        maxWidthPx = with(density) { sprite.maxWidthDp.dp.toPx() }
    )
    val pivot = sprite.pivot ?: DefaultWallPivot
    val panelWidthPx = distanceBetween(panel.bottomStart, panel.bottomEnd).coerceAtLeast(1f)
    val panelHeightPx = distanceBetween(panel.bottomStart, panel.topStart).coerceAtLeast(1f)
    val widthFraction = (spriteSizePx.widthPx / panelWidthPx).coerceIn(0.02f, 0.95f)
    val heightFraction = (spriteSizePx.heightPx / panelHeightPx).coerceIn(0.02f, 0.95f)
    val left = (anchor.u - (widthFraction * pivot.x)).coerceIn(0f, 1f - widthFraction)
    val top = (anchor.v - (heightFraction * pivot.y)).coerceIn(0f, 1f - heightFraction)

    return resolveWallLocalQuad(
        panel = panel,
        u0 = left,
        u1 = left + widthFraction,
        v0 = top,
        v1 = top + heightFraction
    )
}

private fun isDecorOnWallFace(
    anchorFace: WallFace,
    renderFace: WallFace
): Boolean {
    return when (renderFace) {
        WallFace.BACK -> anchorFace == WallFace.BACK
        WallFace.LEFT,
        WallFace.RIGHT -> anchorFace != WallFace.BACK
    }
}

private fun resolveWallSurfaceStyle(
    wallFace: WallFace,
    panel: IsoWallPanelPx,
    roomColumns: Int
): WallSurfaceStyle {
    val baseColumns = roomColumns.coerceAtLeast(1)
    val diagonalLength = distanceBetween(panel.topStart, panel.bottomEnd)
    val additionalColumns = if (diagonalLength > 220f && wallFace != WallFace.BACK) 1 else 0

    return when (wallFace) {
        WallFace.BACK -> WallSurfaceStyle(
            renderColumns = baseColumns,
            overlayBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x0DFFF8EC),
                    Color(0x06000000)
                ),
                startY = panel.topStart.yPx,
                endY = panel.bottomEnd.yPx
            )
        )

        WallFace.LEFT,
        WallFace.RIGHT -> WallSurfaceStyle(
            renderColumns = baseColumns + additionalColumns,
            overlayBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0x06FFFFFF),
                    Color(0x08000000),
                    Color(0x18000000)
                ),
                start = Offset(panel.topStart.xPx, panel.topStart.yPx),
                end = Offset(panel.bottomEnd.xPx, panel.bottomEnd.yPx)
            )
        )
    }
}

private fun expandStartFraction(
    fraction: Float,
    overlap: Float,
    isFirst: Boolean
): Float {
    return if (isFirst) {
        fraction.coerceIn(0f, 1f)
    } else {
        (fraction - overlap).coerceIn(0f, 1f)
    }
}

private fun expandEndFraction(
    fraction: Float,
    overlap: Float,
    isLast: Boolean
): Float {
    return if (isLast) {
        fraction.coerceIn(0f, 1f)
    } else {
        (fraction + overlap).coerceIn(0f, 1f)
    }
}

private fun SurfaceQuadPx.toPath(): Path {
    return Path().apply {
        moveTo(topStart.xPx, topStart.yPx)
        lineTo(topEnd.xPx, topEnd.yPx)
        lineTo(bottomEnd.xPx, bottomEnd.yPx)
        lineTo(bottomStart.xPx, bottomStart.yPx)
        close()
    }
}

private fun IsoFloorDiamondPx.toPath(): Path {
    return Path().apply {
        moveTo(top.xPx, top.yPx)
        lineTo(right.xPx, right.yPx)
        lineTo(bottom.xPx, bottom.yPx)
        lineTo(left.xPx, left.yPx)
        close()
    }
}

private fun IsoWallPanelPx.toPath(): Path {
    return Path().apply {
        moveTo(topStart.xPx, topStart.yPx)
        lineTo(topEnd.xPx, topEnd.yPx)
        lineTo(bottomEnd.xPx, bottomEnd.yPx)
        lineTo(bottomStart.xPx, bottomStart.yPx)
        close()
    }
}

private fun lerpScreenPoint(
    start: ScreenPointPx,
    end: ScreenPointPx,
    fraction: Float
): ScreenPointPx {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    return ScreenPointPx(
        xPx = start.xPx + ((end.xPx - start.xPx) * clampedFraction),
        yPx = start.yPx + ((end.yPx - start.yPx) * clampedFraction)
    )
}

private fun distanceBetween(
    start: ScreenPointPx,
    end: ScreenPointPx
): Float {
    return hypot(end.xPx - start.xPx, end.yPx - start.yPx)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RoomBackgroundPreview() {
    LupaPJTheme {
        RoomBackground(
            projectionSpec = createReferenceIsoRoomProjectionSpec()
        )
    }
}
