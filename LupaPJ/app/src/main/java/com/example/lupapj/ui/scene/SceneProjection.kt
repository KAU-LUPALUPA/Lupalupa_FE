package com.example.lupapj.ui.scene

import com.example.lupapj.data.model.scene.FaceProjectionSpec
import com.example.lupapj.data.model.scene.FixedViewProjectionSpec
import com.example.lupapj.data.model.scene.FloorTilePlacement
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.data.model.scene.IsoRoomProjectionSpec
import com.example.lupapj.data.model.scene.ScreenPointFraction
import com.example.lupapj.data.model.scene.ScenePivot
import com.example.lupapj.data.model.scene.SceneSpriteSpec
import com.example.lupapj.data.model.scene.WallAnchor
import com.example.lupapj.data.model.scene.WallFace

private const val ROOM_FLOOR_BOTTOM_LIMIT_FRACTION = 0.82f
private const val MIN_SPRITE_SCALE = 0.98f
private const val MAX_SPRITE_SCALE = 1.04f
private const val DEFAULT_FOOTPRINT_FILL_RATIO = 0.78f
private const val DEFAULT_PET_TILE_FILL_RATIO = 0.86f

data class SpriteSizePx(
    val widthPx: Float,
    val heightPx: Float
)

data class ScreenPointPx(
    val xPx: Float,
    val yPx: Float
)

data class ProjectedNode(
    val xPx: Float,
    val yPx: Float,
    val widthPx: Float,
    val heightPx: Float,
    val sortDepth: Float,
    val footXpx: Float,
    val footYpx: Float,
    val viewDepth: Float,
    val perspectiveScale: Float
)

data class IsoRoomMetricsPx(
    val roomWidthTiles: Float,
    val roomDepthTiles: Float,
    val tileWidthPx: Float,
    val tileHeightPx: Float,
    val originXPx: Float,
    val originYPx: Float,
    val wallHeightPx: Float
)

data class IsoFloorDiamondPx(
    val top: ScreenPointPx,
    val right: ScreenPointPx,
    val bottom: ScreenPointPx,
    val left: ScreenPointPx
)

data class IsoWallPanelPx(
    val topStart: ScreenPointPx,
    val topEnd: ScreenPointPx,
    val bottomEnd: ScreenPointPx,
    val bottomStart: ScreenPointPx
)

fun createReferenceIsoRoomProjectionSpec(): IsoRoomProjectionSpec {
    return IsoRoomProjectionSpec(
        roomWidthTiles = 6f,
        roomDepthTiles = 6f,
        floorWidthFraction = 0.88f,
        floorTopYFraction = 0.48f,
        tileHeightRatio = 0.50f,
        wallHeightTiles = 4.0f
    )
}

/**
 * Legacy compatibility only.
 * Older preview/debug code still expects face quads, so we derive them from the
 * iso room once using a normalized viewport. The renderer itself now treats the
 * iso tile projection as the real source of truth.
 */
fun createCompatibilityFaceProjectionSpec(
    projectionSpec: IsoRoomProjectionSpec
): FixedViewProjectionSpec {
    val metrics = resolveIsoRoomMetrics(
        viewportWidthPx = 1f,
        viewportHeightPx = 1f,
        projectionSpec = projectionSpec
    )
    val floor = resolveIsoFloorDiamondPx(metrics)
    val backWall = resolveIsoWallPanelPx(WallFace.BACK, metrics)
    val sideWall = resolveIsoWallPanelPx(WallFace.RIGHT, metrics)

    return FixedViewProjectionSpec(
        floorFace = FaceProjectionSpec(
            p00 = floor.top.toFraction(),
            p10 = floor.right.toFraction(),
            p11 = floor.bottom.toFraction(),
            p01 = floor.left.toFraction()
        ),
        x0WallFace = FaceProjectionSpec(
            p00 = backWall.topStart.toFraction(),
            p10 = backWall.topEnd.toFraction(),
            p11 = backWall.bottomEnd.toFraction(),
            p01 = backWall.bottomStart.toFraction()
        ),
        y0WallFace = FaceProjectionSpec(
            p00 = sideWall.topStart.toFraction(),
            p10 = sideWall.topEnd.toFraction(),
            p11 = sideWall.bottomEnd.toFraction(),
            p01 = sideWall.bottomStart.toFraction()
        )
    )
}

/**
 * widthRatio remains the authored sprite width baseline. Tile projection is almost orthographic,
 * so sprite size is driven mainly by authored size instead of camera perspective.
 */
fun resolveSpriteSizePx(
    viewportWidthPx: Float,
    sprite: SceneSpriteSpec,
    minWidthPx: Float,
    maxWidthPx: Float
): SpriteSizePx {
    val baseWidthPx = (viewportWidthPx * sprite.widthRatio).coerceIn(minWidthPx, maxWidthPx)
    return SpriteSizePx(
        widthPx = baseWidthPx,
        heightPx = baseWidthPx * sprite.heightRatio
    )
}

/**
 * In an iso tile room, static furniture should read against the footprint it occupies rather than
 * against the raw viewport width. That keeps large blocks from overwhelming the room.
 */
fun resolveFloorSpriteSizePx(
    viewportWidthPx: Float,
    sprite: SceneSpriteSpec,
    minWidthPx: Float,
    maxWidthPx: Float,
    metrics: IsoRoomMetricsPx,
    tilePlacement: FloorTilePlacement?
): SpriteSizePx {
    val baseWidthPx = if (tilePlacement != null) {
        val footprintWidthPx = tilePlacement.resolveFootprintDiamondWidthPx(metrics)
        footprintWidthPx * (sprite.isoTileFillRatio ?: DEFAULT_FOOTPRINT_FILL_RATIO)
    } else {
        viewportWidthPx * sprite.widthRatio
    }.coerceIn(minWidthPx, maxWidthPx)

    return SpriteSizePx(
        widthPx = baseWidthPx,
        heightPx = baseWidthPx * sprite.heightRatio
    )
}

fun resolvePetSpriteSizePx(
    sprite: SceneSpriteSpec,
    minWidthPx: Float,
    maxWidthPx: Float,
    metrics: IsoRoomMetricsPx
): SpriteSizePx {
    val baseWidthPx = (
        metrics.tileWidthPx * (sprite.isoTileFillRatio ?: DEFAULT_PET_TILE_FILL_RATIO)
        ).coerceIn(minWidthPx, maxWidthPx)

    return SpriteSizePx(
        widthPx = baseWidthPx,
        heightPx = baseWidthPx * sprite.heightRatio
    )
}

fun resolveIsoRoomMetrics(
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    projectionSpec: IsoRoomProjectionSpec
): IsoRoomMetricsPx {
    val combinedTiles = projectionSpec.roomWidthTiles + projectionSpec.roomDepthTiles
    val targetFloorWidthPx = viewportWidthPx * projectionSpec.floorWidthFraction
    val widthConstrainedTileWidth = targetFloorWidthPx * 2f / combinedTiles

    val originYPx = viewportHeightPx * projectionSpec.floorTopYFraction
    val floorBottomLimitPx = viewportHeightPx * ROOM_FLOOR_BOTTOM_LIMIT_FRACTION
    val availableFloorHeightPx = (floorBottomLimitPx - originYPx).coerceAtLeast(viewportHeightPx * 0.16f)
    val heightConstrainedTileWidth =
        availableFloorHeightPx * 2f / (combinedTiles * projectionSpec.tileHeightRatio)

    val tileWidthPx = minOf(widthConstrainedTileWidth, heightConstrainedTileWidth)
    val tileHeightPx = tileWidthPx * projectionSpec.tileHeightRatio

    return IsoRoomMetricsPx(
        roomWidthTiles = projectionSpec.roomWidthTiles,
        roomDepthTiles = projectionSpec.roomDepthTiles,
        tileWidthPx = tileWidthPx,
        tileHeightPx = tileHeightPx,
        originXPx = viewportWidthPx * 0.5f,
        originYPx = originYPx,
        wallHeightPx = tileHeightPx * projectionSpec.wallHeightTiles
    )
}

fun isoToScreen(
    xTiles: Float,
    yTiles: Float,
    metrics: IsoRoomMetricsPx
): ScreenPointPx {
    return ScreenPointPx(
        xPx = metrics.originXPx + ((xTiles - yTiles) * metrics.tileWidthPx * 0.5f),
        yPx = metrics.originYPx + ((xTiles + yTiles) * metrics.tileHeightPx * 0.5f)
    )
}

fun resolveIsoFloorDiamondPx(
    metrics: IsoRoomMetricsPx
): IsoFloorDiamondPx {
    return IsoFloorDiamondPx(
        top = isoToScreen(0f, 0f, metrics),
        right = isoToScreen(metrics.roomWidthTiles, 0f, metrics),
        bottom = isoToScreen(metrics.roomWidthTiles, metrics.roomDepthTiles, metrics),
        left = isoToScreen(0f, metrics.roomDepthTiles, metrics)
    )
}

fun resolveIsoWallPanelPx(
    face: WallFace,
    metrics: IsoRoomMetricsPx
): IsoWallPanelPx {
    val floor = resolveIsoFloorDiamondPx(metrics)
    val topUp = floor.top.shiftUp(metrics.wallHeightPx)

    return when (face) {
        WallFace.BACK -> IsoWallPanelPx(
            topStart = topUp,
            topEnd = floor.right.shiftUp(metrics.wallHeightPx),
            bottomEnd = floor.right,
            bottomStart = floor.top
        )

        WallFace.RIGHT,
        WallFace.LEFT -> IsoWallPanelPx(
            topStart = topUp,
            topEnd = floor.left.shiftUp(metrics.wallHeightPx),
            bottomEnd = floor.left,
            bottomStart = floor.top
        )
    }
}

fun projectFloorAnchor(
    anchor: FloorAnchor,
    projectionSpec: IsoRoomProjectionSpec,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    spriteSizePx: SpriteSizePx,
    pivot: ScenePivot,
    depthBias: Float = 0f
): ProjectedNode {
    val metrics = resolveIsoRoomMetrics(
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        projectionSpec = projectionSpec
    )
    val projectedPoint = isoToScreen(
        xTiles = anchor.u * metrics.roomWidthTiles,
        yTiles = anchor.v * metrics.roomDepthTiles,
        metrics = metrics
    )
    val depthT = ((anchor.u + anchor.v) * 0.5f).coerceIn(0f, 1f)
    val scale = gentleScaleForDepth(depthT)

    return createProjectedNode(
        projectedPoint = projectedPoint,
        spriteSizePx = spriteSizePx,
        pivot = pivot,
        sortDepth = anchor.u + anchor.v + depthBias,
        perspectiveScale = scale
    )
}

fun projectWallAnchor(
    anchor: WallAnchor,
    projectionSpec: IsoRoomProjectionSpec,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    spriteSizePx: SpriteSizePx,
    pivot: ScenePivot,
    depthBias: Float = 0f
): ProjectedNode {
    val metrics = resolveIsoRoomMetrics(
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        projectionSpec = projectionSpec
    )
    val floor = resolveIsoFloorDiamondPx(metrics)
    val bottomPoint = when (anchor.face) {
        WallFace.BACK -> lerpScreenPoint(floor.top, floor.right, anchor.u)
        WallFace.RIGHT,
        WallFace.LEFT -> lerpScreenPoint(floor.top, floor.left, anchor.u)
    }
    val projectedPoint = ScreenPointPx(
        xPx = bottomPoint.xPx,
        yPx = bottomPoint.yPx - ((1f - anchor.v.coerceIn(0f, 1f)) * metrics.wallHeightPx)
    )

    return createProjectedNode(
        projectedPoint = projectedPoint,
        spriteSizePx = spriteSizePx,
        pivot = pivot,
        sortDepth = anchor.u + depthBias,
        perspectiveScale = 1f
    )
}

fun resolveFloorAnchorFromViewport(
    tapXPx: Float,
    tapYPx: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    projectionSpec: IsoRoomProjectionSpec
): FloorAnchor? {
    val metrics = resolveIsoRoomMetrics(
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        projectionSpec = projectionSpec
    )
    val halfTileWidth = metrics.tileWidthPx * 0.5f
    val halfTileHeight = metrics.tileHeightPx * 0.5f
    val a = (tapXPx - metrics.originXPx) / halfTileWidth
    val b = (tapYPx - metrics.originYPx) / halfTileHeight
    val xTiles = (a + b) * 0.5f
    val yTiles = (b - a) * 0.5f
    val u = xTiles / metrics.roomWidthTiles
    val v = yTiles / metrics.roomDepthTiles

    if (u !in 0f..1f || v !in 0f..1f) return null

    return FloorAnchor(
        u = u,
        v = v
    )
}

private fun createProjectedNode(
    projectedPoint: ScreenPointPx,
    spriteSizePx: SpriteSizePx,
    pivot: ScenePivot,
    sortDepth: Float,
    perspectiveScale: Float
): ProjectedNode {
    val widthPx = spriteSizePx.widthPx * perspectiveScale
    val heightPx = spriteSizePx.heightPx * perspectiveScale

    return ProjectedNode(
        xPx = projectedPoint.xPx - (widthPx * pivot.x),
        yPx = projectedPoint.yPx - (heightPx * pivot.y),
        widthPx = widthPx,
        heightPx = heightPx,
        sortDepth = sortDepth,
        footXpx = projectedPoint.xPx,
        footYpx = projectedPoint.yPx,
        viewDepth = sortDepth,
        perspectiveScale = perspectiveScale
    )
}

private fun gentleScaleForDepth(depthT: Float): Float {
    return lerp(MIN_SPRITE_SCALE, MAX_SPRITE_SCALE, depthT)
}

fun FloorTilePlacement.resolveFootprintDiamondWidthPx(metrics: IsoRoomMetricsPx): Float {
    return (footprint.widthTiles + footprint.depthTiles) * metrics.tileWidthPx * 0.5f
}

private fun lerpScreenPoint(
    start: ScreenPointPx,
    end: ScreenPointPx,
    fraction: Float
): ScreenPointPx {
    return ScreenPointPx(
        xPx = lerp(start.xPx, end.xPx, fraction),
        yPx = lerp(start.yPx, end.yPx, fraction)
    )
}

private fun ScreenPointPx.shiftUp(offsetPx: Float): ScreenPointPx {
    return ScreenPointPx(
        xPx = xPx,
        yPx = yPx - offsetPx
    )
}

private fun ScreenPointPx.toFraction(): ScreenPointFraction {
    return ScreenPointFraction(
        xFraction = xPx,
        yFraction = yPx
    )
}

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + ((end - start) * fraction)
}
