package com.example.lupapj.data.mock

import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.scene.DefaultFloorPivot
import com.example.lupapj.data.model.scene.DefaultWallPivot
import com.example.lupapj.data.model.scene.FaceProjectionSpec
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.data.model.scene.FloorPlaneSpec
import com.example.lupapj.data.model.scene.IsoRoomProjectionSpec
import com.example.lupapj.data.model.scene.RoomPortal
import com.example.lupapj.data.model.scene.RoomSceneDefinition
import com.example.lupapj.data.model.scene.RoomSceneId
import com.example.lupapj.data.model.scene.SceneCollisionRect
import com.example.lupapj.data.model.scene.SceneNavigationSpec
import com.example.lupapj.data.model.scene.SceneObjectDefinition
import com.example.lupapj.data.model.scene.ScenePivot
import com.example.lupapj.data.model.scene.SceneSpriteSpec
import com.example.lupapj.data.model.scene.FloorTilePlacement
import com.example.lupapj.data.model.scene.TileAnchorMode
import com.example.lupapj.data.model.scene.TileCoord
import com.example.lupapj.data.model.scene.TileFootprint
import com.example.lupapj.data.model.scene.WallAnchor
import com.example.lupapj.data.model.scene.WallFace
import com.example.lupapj.data.model.scene.WallPlaneSpec
import com.example.lupapj.data.model.scene.toFloorAnchor
import com.example.lupapj.ui.scene.createCompatibilityFaceProjectionSpec
import com.example.lupapj.ui.scene.createReferenceIsoRoomProjectionSpec

object DemoScenes {
    private val mainRoomId = RoomSceneId("main_room")
    private val sideRoomId = RoomSceneId("side_room")

    private val isoProjection: IsoRoomProjectionSpec = createReferenceIsoRoomProjectionSpec()
    private val compatibilityProjection = createCompatibilityFaceProjectionSpec(isoProjection)
    private val bedPlacement = FloorTilePlacement(
        tile = TileCoord(0, 0),
        footprint = TileFootprint(2, 2),
        anchorMode = TileAnchorMode.CENTER
    )
    private val toyBoxPlacement = FloorTilePlacement(
        tile = TileCoord(4, 3),
        footprint = TileFootprint(1, 1),
        anchorMode = TileAnchorMode.CENTER
    )
    private val foodBagPlacement = FloorTilePlacement(
        tile = TileCoord(1, 3),
        footprint = TileFootprint(1, 1),
        anchorMode = TileAnchorMode.CENTER
    )

    val mainRoom = RoomSceneDefinition(
        id = mainRoomId,
        wallAssetKey = "room/walls/main_wall",
        floorAssetKey = "room/floors/main_floor",
        projectionSpec = isoProjection,
        wallPlane = wallPlaneFromFace(compatibilityProjection.x0WallFace),
        sideWallFace = WallFace.RIGHT,
        sideWallPlane = wallPlaneFromFace(compatibilityProjection.y0WallFace),
        floorPlane = floorPlaneFromFace(compatibilityProjection.floorFace),
        fixedDecor = listOf(
            SceneObjectDefinition(
                id = "window",
                type = RoomObjectType.WINDOW,
                anchor = WallAnchor(face = WallFace.BACK, u = 0.58f, v = 0.36f),
                sprite = SceneSpriteSpec(
                    assetKey = "room/decor/window_main",
                    fallbackLabel = "창문",
                    widthRatio = 0.20f,
                    heightRatio = 1.12f,
                    minWidthDp = 72f,
                    maxWidthDp = 124f,
                    isoTileFillRatio = 0.90f,
                    pivot = DefaultWallPivot
                ),
                clickable = false
            )
        ),
        objects = listOf(
            SceneObjectDefinition(
                id = "bed",
                type = RoomObjectType.BED,
                anchor = bedPlacement.toFloorAnchor(isoProjection),
                tilePlacement = bedPlacement,
                sprite = SceneSpriteSpec(
                    assetKey = "room/objects/bed_basic",
                    fallbackLabel = "침대",
                    widthRatio = 0.24f,
                    heightRatio = 0.56f,
                    minWidthDp = 88f,
                    maxWidthDp = 148f,
                    isoTileFillRatio = 0.94f,
                    pivot = ScenePivot(x = 0.5f, y = 1.04f)
                )
            ),
            SceneObjectDefinition(
                id = "toy_box",
                type = RoomObjectType.TOY_BOX,
                anchor = toyBoxPlacement.toFloorAnchor(isoProjection),
                tilePlacement = toyBoxPlacement,
                sprite = SceneSpriteSpec(
                    assetKey = "room/objects/toy_box_basic",
                    fallbackLabel = "장난감박스",
                    widthRatio = 0.16f,
                    heightRatio = 0.82f,
                    minWidthDp = 56f,
                    maxWidthDp = 88f,
                    isoTileFillRatio = 0.76f,
                    pivot = DefaultFloorPivot
                )
            ),
            SceneObjectDefinition(
                id = "food_bag",
                type = RoomObjectType.FOOD_BAG,
                anchor = foodBagPlacement.toFloorAnchor(isoProjection),
                tilePlacement = foodBagPlacement,
                sprite = SceneSpriteSpec(
                    assetKey = "room/objects/food_bag_basic",
                    fallbackLabel = "사료 봉투",
                    widthRatio = 0.10f,
                    heightRatio = 1.30f,
                    minWidthDp = 38f,
                    maxWidthDp = 60f,
                    isoTileFillRatio = 0.72f,
                    pivot = DefaultFloorPivot
                )
            )
        ),
        frontOccluders = emptyList(),
        portals = listOf(
            RoomPortal(
                id = "to_side_room",
                anchor = FloorAnchor(u = 0.92f, v = 0.58f),
                targetSceneId = sideRoomId,
                targetSpawn = FloorAnchor(u = 0.10f, v = 0.58f)
            )
        ),
        navigationSpec = SceneNavigationSpec(
            walkableAreas = listOf(
                SceneCollisionRect(
                    minU = 0.08f,
                    minV = 0.20f,
                    maxU = 0.94f,
                    maxV = 0.92f
                )
            )
        )
    )

    val sideRoom = RoomSceneDefinition(
        id = sideRoomId,
        wallAssetKey = "room/walls/side_wall",
        floorAssetKey = "room/floors/side_floor",
        projectionSpec = isoProjection,
        wallPlane = wallPlaneFromFace(compatibilityProjection.x0WallFace),
        sideWallFace = WallFace.RIGHT,
        sideWallPlane = wallPlaneFromFace(compatibilityProjection.y0WallFace),
        floorPlane = floorPlaneFromFace(compatibilityProjection.floorFace),
        fixedDecor = listOf(
            SceneObjectDefinition(
                id = "side_window",
                type = RoomObjectType.WINDOW,
                anchor = WallAnchor(face = WallFace.BACK, u = 0.30f, v = 0.30f),
                sprite = SceneSpriteSpec(
                    assetKey = "room/decor/window_side",
                    fallbackLabel = "창문",
                    widthRatio = 0.20f,
                    heightRatio = 1.08f,
                    minWidthDp = 84f,
                    maxWidthDp = 128f,
                    pivot = DefaultWallPivot
                ),
                clickable = false
            )
        ),
        objects = emptyList(),
        portals = listOf(
            RoomPortal(
                id = "back_to_main_room",
                anchor = FloorAnchor(u = 0.06f, v = 0.58f),
                targetSceneId = mainRoomId,
                targetSpawn = FloorAnchor(u = 0.88f, v = 0.58f)
            )
        )
    )

    private val scenesById = listOf(mainRoom, sideRoom).associateBy { it.id }

    fun sceneFor(id: RoomSceneId): RoomSceneDefinition {
        return requireNotNull(scenesById[id]) { "Unknown scene id: ${id.value}" }
    }

    private fun wallPlaneFromFace(face: FaceProjectionSpec): WallPlaneSpec {
        return WallPlaneSpec(
            topLeftXFraction = face.p00.xFraction,
            topLeftYFraction = face.p00.yFraction,
            topRightXFraction = face.p10.xFraction,
            topRightYFraction = face.p10.yFraction,
            bottomLeftXFraction = face.p01.xFraction,
            bottomLeftYFraction = face.p01.yFraction,
            bottomRightXFraction = face.p11.xFraction,
            bottomRightYFraction = face.p11.yFraction
        )
    }

    private fun floorPlaneFromFace(face: FaceProjectionSpec): FloorPlaneSpec {
        return FloorPlaneSpec(
            backLeftXFraction = face.p00.xFraction,
            backRightXFraction = face.p10.xFraction,
            frontLeftXFraction = face.p01.xFraction,
            frontRightXFraction = face.p11.xFraction,
            backYFraction = face.p00.yFraction,
            frontYFraction = face.p11.yFraction,
            backLeftYFraction = face.p00.yFraction,
            backRightYFraction = face.p10.yFraction,
            frontLeftYFraction = face.p01.yFraction,
            frontRightYFraction = face.p11.yFraction
        )
    }
}
