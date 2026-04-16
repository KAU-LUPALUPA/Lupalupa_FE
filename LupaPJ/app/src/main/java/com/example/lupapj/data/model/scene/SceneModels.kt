package com.example.lupapj.data.model.scene

import com.example.lupapj.data.model.RoomObjectType

@JvmInline
value class RoomSceneId(val value: String)

data class ScreenPointFraction(
    val xFraction: Float,
    val yFraction: Float
)

typealias SceneFractionPoint = ScreenPointFraction

/**
 * FaceProjectionSpec stores the screen-space quad for one cube face after projecting the
 * fixed interior viewing direction onto 2D.
 *
 * - p00 = local face (0, 0)
 * - p10 = local face (1, 0)
 * - p11 = local face (1, 1)
 * - p01 = local face (0, 1)
 */
data class FaceProjectionSpec(
    val p00: ScreenPointFraction,
    val p10: ScreenPointFraction,
    val p11: ScreenPointFraction,
    val p01: ScreenPointFraction
)

/**
 * Fixed-view projection for the three visible cube faces.
 *
 * Geometry mapping:
 * - floorFace: (u, v) -> world (u, v, 0)
 * - x0WallFace: WallFace.BACK -> world (0, u, 1 - v)
 * - y0WallFace: WallFace.RIGHT -> world (u, 0, 1 - v)
 *
 * WallFace.LEFT is retained only as a compatibility alias for the side wall while the
 * previous scene naming migrates to fixed-view face semantics.
 */
data class FixedViewProjectionSpec(
    val floorFace: FaceProjectionSpec,
    val x0WallFace: FaceProjectionSpec,
    val y0WallFace: FaceProjectionSpec
)

/**
 * Kairosoft-style isometric room projection spec.
 *
 * The room uses a stable tile room instead of a projected cube:
 * - roomWidthTiles / roomDepthTiles: normalized FloorAnchor space mapped onto tile counts
 * - floorWidthFraction: target floor diamond width inside the viewport
 * - floorTopYFraction: screen Y for the back/top corner of the floor diamond
 * - tileHeightRatio: tileHeight = tileWidth * tileHeightRatio
 * - wallHeightTiles: low cutaway wall height measured in tile-height units
 */
data class IsoRoomProjectionSpec(
    val roomWidthTiles: Float,
    val roomDepthTiles: Float,
    val floorWidthFraction: Float,
    val floorTopYFraction: Float,
    val tileHeightRatio: Float,
    val wallHeightTiles: Float
)

/**
 * Anchor values are stored in room-local normalized coordinates rather than viewport pixels.
 *
 * - FloorAnchor.u: horizontal placement from left(0f) to right(1f)
 * - FloorAnchor.v: depth placement from back(0f) to front(1f)
 * - WallAnchor.u: horizontal placement from left(0f) to right(1f)
 * - WallAnchor.v: vertical placement from top(0f) to bottom(1f)
 */
sealed interface SceneAnchor {
    val u: Float
    val v: Float
}

data class WallAnchor(
    val face: WallFace = WallFace.BACK,
    override val u: Float,
    override val v: Float
) : SceneAnchor

data class FloorAnchor(
    override val u: Float,
    override val v: Float
) : SceneAnchor

data class TileCoord(
    val x: Int,
    val y: Int
)

data class TileFootprint(
    val widthTiles: Int,
    val depthTiles: Int
)

enum class TileAnchorMode {
    CENTER,
    FRONT_CENTER
}

data class FloorTilePlacement(
    val tile: TileCoord,
    val footprint: TileFootprint = TileFootprint(1, 1),
    val anchorMode: TileAnchorMode = TileAnchorMode.FRONT_CENTER
)

data class ScenePivot(
    val x: Float,
    val y: Float
)

val DefaultFloorPivot = ScenePivot(x = 0.5f, y = 1.0f)
val DefaultWallPivot = ScenePivot(x = 0.5f, y = 0.5f)

fun defaultPivotFor(anchor: SceneAnchor): ScenePivot {
    return when (anchor) {
        is FloorAnchor -> DefaultFloorPivot
        is WallAnchor -> DefaultWallPivot
    }
}

/**
 * widthRatio is used as the normalized base width: baseWidthPx = viewportWidth * widthRatio.
 * The base width is clamped between minWidthDp and maxWidthDp after converting them to px.
 * heightRatio always means height = width * heightRatio.
 */
data class SceneSpriteSpec(
    val assetKey: String? = null,
    val fallbackLabel: String,
    val widthRatio: Float,
    val heightRatio: Float,
    val minWidthDp: Float,
    val maxWidthDp: Float,
    // Optional art-direction hint for iso rooms. Floor furniture can size itself against its
    // tile footprint, and actors can size against a single tile, without changing placement.
    val isoTileFillRatio: Float? = null,
    val pivot: ScenePivot? = null
)

data class SceneObjectDefinition(
    val id: String,
    val type: RoomObjectType,
    val anchor: SceneAnchor,
    // Static furniture can opt into tile placement while runtime anchors remain backward compatible.
    // This lets us migrate room layout data first without forcing every dynamic state to become tile-based.
    val tilePlacement: FloorTilePlacement? = null,
    val sprite: SceneSpriteSpec,
    val clickable: Boolean = true,
    val depthBias: Float = 0f
)

enum class WallFace {
    // Back cutaway wall running from the rear corner toward the right floor edge.
    BACK,
    // Compatibility alias kept while older scene naming still references "left".
    LEFT,
    // Side cutaway wall running from the rear corner toward the left floor edge.
    RIGHT
}

data class FloorPlaneSpec(
    val backLeftXFraction: Float,
    val backRightXFraction: Float,
    val frontLeftXFraction: Float,
    val frontRightXFraction: Float,
    val backYFraction: Float,
    val frontYFraction: Float,
    val minScale: Float = 0.82f,
    val maxScale: Float = 1.18f,
    val backLeftYFraction: Float = backYFraction,
    val backRightYFraction: Float = backYFraction,
    val frontLeftYFraction: Float = frontYFraction,
    val frontRightYFraction: Float = frontYFraction
) {
    val backLeftCorner: SceneFractionPoint
        get() = SceneFractionPoint(backLeftXFraction, backLeftYFraction)

    val backRightCorner: SceneFractionPoint
        get() = SceneFractionPoint(backRightXFraction, backRightYFraction)

    val frontLeftCorner: SceneFractionPoint
        get() = SceneFractionPoint(frontLeftXFraction, frontLeftYFraction)

    val frontRightCorner: SceneFractionPoint
        get() = SceneFractionPoint(frontRightXFraction, frontRightYFraction)
}

data class WallPlaneSpec(
    val leftXFraction: Float = 0f,
    val rightXFraction: Float = 1f,
    val topYFraction: Float = 0f,
    val bottomYFraction: Float = 0.58f,
    val topLeftXFraction: Float = leftXFraction,
    val topLeftYFraction: Float = topYFraction,
    val topRightXFraction: Float = rightXFraction,
    val topRightYFraction: Float = topYFraction,
    val bottomLeftXFraction: Float = leftXFraction,
    val bottomLeftYFraction: Float = bottomYFraction,
    val bottomRightXFraction: Float = rightXFraction,
    val bottomRightYFraction: Float = bottomYFraction
) {
    val topLeftCorner: SceneFractionPoint
        get() = SceneFractionPoint(topLeftXFraction, topLeftYFraction)

    val topRightCorner: SceneFractionPoint
        get() = SceneFractionPoint(topRightXFraction, topRightYFraction)

    val bottomLeftCorner: SceneFractionPoint
        get() = SceneFractionPoint(bottomLeftXFraction, bottomLeftYFraction)

    val bottomRightCorner: SceneFractionPoint
        get() = SceneFractionPoint(bottomRightXFraction, bottomRightYFraction)
}

data class SceneCollisionRect(
    val minU: Float,
    val minV: Float,
    val maxU: Float,
    val maxV: Float
)

data class SceneNavigationSpec(
    val walkableAreas: List<SceneCollisionRect> = emptyList(),
    val blockedZones: List<SceneCollisionRect> = emptyList()
)

data class RoomPortal(
    val id: String,
    val anchor: FloorAnchor,
    val targetSceneId: RoomSceneId,
    val targetSpawn: FloorAnchor
)

data class RoomSceneDefinition(
    val id: RoomSceneId,
    val wallAssetKey: String? = null,
    val floorAssetKey: String? = null,
    val projectionSpec: IsoRoomProjectionSpec,
    // Legacy quad specs retained temporarily so old preview/debug adapters still compile.
    val wallPlane: WallPlaneSpec,
    val sideWallFace: WallFace? = null,
    val sideWallPlane: WallPlaneSpec? = null,
    val floorPlane: FloorPlaneSpec,
    val fixedDecor: List<SceneObjectDefinition>,
    val objects: List<SceneObjectDefinition>,
    val frontOccluders: List<SceneObjectDefinition> = emptyList(),
    val portals: List<RoomPortal> = emptyList(),
    // TODO: consume navigationSpec from pet movement/collision logic when scene walking is added.
    val navigationSpec: SceneNavigationSpec? = null
)

/**
 * Tile placement is introduced first for static furniture because it unlocks occupancy and validation
 * without forcing pet runtime state or tap placement flows to change in the same step.
 *
 * - CENTER: anchor at the middle of the occupied footprint
 * - FRONT_CENTER: anchor at the front edge center, which is usually a better render foot point
 *   for large furniture in an isometric room
 */
fun FloorTilePlacement.toFloorAnchor(spec: IsoRoomProjectionSpec): FloorAnchor {
    val anchorX = tile.x + (footprint.widthTiles / 2f)
    val anchorY = when (anchorMode) {
        TileAnchorMode.CENTER -> tile.y + (footprint.depthTiles / 2f)
        TileAnchorMode.FRONT_CENTER -> tile.y + footprint.depthTiles.toFloat()
    }
    return FloorAnchor(
        u = anchorX / spec.roomWidthTiles,
        v = anchorY / spec.roomDepthTiles
    )
}

fun FloorTilePlacement.occupiedTiles(): List<TileCoord> {
    val occupied = mutableListOf<TileCoord>()
    for (xOffset in 0 until footprint.widthTiles) {
        for (yOffset in 0 until footprint.depthTiles) {
            occupied += TileCoord(
                x = tile.x + xOffset,
                y = tile.y + yOffset
            )
        }
    }
    return occupied
}
