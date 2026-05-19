package com.example.lupapj.data.remote.room

import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.initialRoomUiState
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.data.model.scene.FloorTilePlacement
import com.example.lupapj.data.model.scene.HouseSceneState
import com.example.lupapj.data.model.scene.RoomSceneDefinition
import com.example.lupapj.data.model.scene.SceneObjectDefinition
import com.example.lupapj.data.model.scene.TileAnchorMode
import com.example.lupapj.data.model.scene.TileCoord
import com.example.lupapj.data.model.scene.TileFootprint
import com.example.lupapj.data.model.scene.WallAnchor
import com.example.lupapj.data.model.scene.toFloorAnchor
import com.example.lupapj.data.remote.pet.PetDto
import com.example.lupapj.data.remote.pet.toHouseSceneState
import kotlin.math.roundToInt

fun RoomLayoutDto.toDomainRoomState(
    sceneResolver: (String) -> RoomSceneDefinition,
    pet: PetDto? = null,
    fallbackHouseSceneState: HouseSceneState? = null
): RoomUiState {
    val baseScene = sceneResolver(sceneId)
    val sceneDefinition = baseScene.copy(
        wallAssetKey = wallAssetKey ?: baseScene.wallAssetKey,
        floorAssetKey = floorAssetKey ?: baseScene.floorAssetKey,
        objects = placedItems.toSceneObjects(baseScene)
    )
    val houseSceneState = pet?.toHouseSceneState(sceneDefinition.id)
        ?: fallbackHouseSceneState?.copy(
            currentSceneId = sceneDefinition.id,
            currentSceneRuntime = fallbackHouseSceneState.currentSceneRuntime.copy(
                sceneId = sceneDefinition.id
            )
        )

    return initialRoomUiState(
        sceneDefinition = sceneDefinition,
        houseSceneState = houseSceneState ?: com.example.lupapj.data.model.scene.initialHouseSceneState(
            sceneDefinition.id
        )
    ).copy(
        layoutRevision = layoutRevision,
        layoutHash = layoutHash,
        layoutUpdatedAt = updatedAt
    )
}

fun RoomUiState.toSaveRoomLayoutRequestDto(): SaveRoomLayoutRequestDto {
    return SaveRoomLayoutRequestDto(
        baseLayoutRevision = layoutRevision,
        wallAssetKey = sceneDefinition.wallAssetKey,
        floorAssetKey = sceneDefinition.floorAssetKey,
        placedItems = sceneDefinition.objects.map { it.toPlacedRoomItemDto(sceneDefinition) }
    )
}

private fun List<PlacedRoomItemDto>.toSceneObjects(
    baseScene: RoomSceneDefinition
): List<SceneObjectDefinition> {
    return mapNotNull { placedItem ->
        val type = placedItem.type.toRoomObjectTypeOrNull()
            ?: placedItem.shopItemId.toRoomObjectTypeOrNull()
            ?: placedItem.assetKey.toRoomObjectTypeOrNull()
            ?: return@mapNotNull null
        val template = baseScene.objects.firstOrNull { it.type == type }
            ?: return@mapNotNull null
        val tilePlacement = placedItem.tilePlacement?.toFloorTilePlacement()
        val anchor = tilePlacement?.toFloorAnchor(baseScene.projectionSpec)
            ?: placedItem.wallPlacement?.toWallAnchor()
            ?: template.anchor

        template.copy(
            id = placedItem.placementId,
            anchor = anchor,
            tilePlacement = tilePlacement,
            depthBias = placedItem.depthBias
        )
    }
}

private fun SceneObjectDefinition.toPlacedRoomItemDto(
    sceneDefinition: RoomSceneDefinition
): PlacedRoomItemDto {
    val floorTilePlacement = tilePlacement
        ?: (anchor as? FloorAnchor)?.toTilePlacement(type, sceneDefinition)

    return PlacedRoomItemDto(
        placementId = id,
        inventoryItemId = null,
        shopItemId = type.name,
        assetKey = sprite.assetKey ?: defaultAssetKeyFor(type),
        type = type.name,
        anchorType = when (anchor) {
            is FloorAnchor -> "FLOOR"
            is WallAnchor -> "WALL"
        },
        tilePlacement = floorTilePlacement?.toDto(),
        wallPlacement = (anchor as? WallAnchor)?.toDto(),
        scale = 1f,
        rotation = 0,
        depthBias = depthBias
    )
}

private fun TilePlacementDto.toFloorTilePlacement(): FloorTilePlacement {
    return FloorTilePlacement(
        tile = TileCoord(
            x = tile.x.coerceAtLeast(0),
            y = tile.y.coerceAtLeast(0)
        ),
        footprint = TileFootprint(
            widthTiles = footprint.widthTiles.coerceAtLeast(1),
            depthTiles = footprint.depthTiles.coerceAtLeast(1)
        ),
        anchorMode = enumValueOrDefault(anchorMode, TileAnchorMode.CENTER)
    )
}

private fun FloorTilePlacement.toDto(): TilePlacementDto {
    return TilePlacementDto(
        tile = TileCoordDto(
            x = tile.x,
            y = tile.y
        ),
        footprint = TileFootprintDto(
            widthTiles = footprint.widthTiles,
            depthTiles = footprint.depthTiles
        ),
        anchorMode = anchorMode.name
    )
}

private fun WallPlacementDto.toWallAnchor(): WallAnchor? {
    val face = runCatching {
        enumValueOf<com.example.lupapj.data.model.scene.WallFace>(face)
    }.getOrNull() ?: return null
    return WallAnchor(
        face = face,
        u = u.coerceIn(0f, 1f),
        v = v.coerceIn(0f, 1f)
    )
}

private fun WallAnchor.toDto(): WallPlacementDto {
    return WallPlacementDto(
        face = face.name,
        u = u.coerceIn(0f, 1f),
        v = v.coerceIn(0f, 1f)
    )
}

private fun FloorAnchor.toTilePlacement(
    type: RoomObjectType,
    sceneDefinition: RoomSceneDefinition
): FloorTilePlacement {
    val footprint = defaultFootprintFor(type)
    val projectionSpec = sceneDefinition.projectionSpec
    val maxX = (projectionSpec.roomWidthTiles.toInt() - footprint.widthTiles).coerceAtLeast(0)
    val maxY = (projectionSpec.roomDepthTiles.toInt() - footprint.depthTiles).coerceAtLeast(0)
    return FloorTilePlacement(
        tile = TileCoord(
            x = (u * projectionSpec.roomWidthTiles).roundToInt().coerceIn(0, maxX),
            y = (v * projectionSpec.roomDepthTiles).roundToInt().coerceIn(0, maxY)
        ),
        footprint = footprint,
        anchorMode = TileAnchorMode.CENTER
    )
}

private fun defaultFootprintFor(type: RoomObjectType): TileFootprint {
    return when (type) {
        RoomObjectType.BED -> TileFootprint(widthTiles = 2, depthTiles = 2)
        RoomObjectType.TOY_BOX,
        RoomObjectType.FOOD_BAG,
        RoomObjectType.WINDOW -> TileFootprint(widthTiles = 1, depthTiles = 1)
    }
}

private fun defaultAssetKeyFor(type: RoomObjectType): String {
    return when (type) {
        RoomObjectType.BED -> "room/objects/bed_basic"
        RoomObjectType.TOY_BOX -> "room/objects/toy_box_basic"
        RoomObjectType.FOOD_BAG -> "room/objects/food_bag_basic"
        RoomObjectType.WINDOW -> "room/decor/window_main"
    }
}

private fun String?.toRoomObjectTypeOrNull(): RoomObjectType? {
    val normalized = this
        ?.uppercase()
        ?.replace(Regex("[^A-Z0-9]+"), "_")
        ?: return null

    return when {
        "BED" in normalized -> RoomObjectType.BED
        "TOY" in normalized -> RoomObjectType.TOY_BOX
        "FOOD" in normalized || "FEED" in normalized -> RoomObjectType.FOOD_BAG
        "WINDOW" in normalized -> RoomObjectType.WINDOW
        else -> null
    }
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(
    value: String,
    default: T
): T {
    return runCatching { enumValueOf<T>(value) }.getOrDefault(default)
}
