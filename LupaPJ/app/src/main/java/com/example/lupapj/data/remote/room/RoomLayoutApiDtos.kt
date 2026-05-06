package com.example.lupapj.data.remote.room

data class ValidateRoomLayoutRequestDto(
    val localLayoutRevision: Int?,
    val localLayoutHash: String?,
    val localUpdatedAt: String?
)

data class RoomLayoutValidationResponseDto(
    val syncStatus: String,
    val serverLayoutRevision: Int?,
    val serverLayoutHash: String?,
    val serverUpdatedAt: String?,
    val roomLayout: RoomLayoutDto? = null
)

data class RoomLayoutResponseDto(
    val roomLayout: RoomLayoutDto
)

data class SaveRoomLayoutRequestDto(
    val baseLayoutRevision: Int?,
    val wallAssetKey: String?,
    val floorAssetKey: String?,
    val placedItems: List<PlacedRoomItemDto>
)

data class RoomLayoutDto(
    val roomId: String,
    val ownerUserId: String,
    val sceneId: String = "main_room",
    val layoutRevision: Int,
    val layoutHash: String,
    val wallAssetKey: String?,
    val floorAssetKey: String?,
    val placedItems: List<PlacedRoomItemDto>,
    val updatedAt: String
)

data class PlacedRoomItemDto(
    val placementId: String,
    val inventoryItemId: String? = null,
    val shopItemId: String? = null,
    val assetKey: String,
    val type: String,
    val anchorType: String,
    val tilePlacement: TilePlacementDto? = null,
    val wallPlacement: WallPlacementDto? = null,
    val scale: Float = 1f,
    val rotation: Int = 0,
    val depthBias: Float = 0f
)

data class TilePlacementDto(
    val tile: TileCoordDto,
    val footprint: TileFootprintDto,
    val anchorMode: String
)

data class TileCoordDto(
    val x: Int,
    val y: Int
)

data class TileFootprintDto(
    val widthTiles: Int,
    val depthTiles: Int
)

data class WallPlacementDto(
    val face: String,
    val u: Float,
    val v: Float
)
