package com.example.lupapj.ui.screens.main

import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.data.model.scene.FloorTilePlacement
import com.example.lupapj.data.model.scene.IsoRoomProjectionSpec
import com.example.lupapj.data.model.scene.SceneAnchor
import com.example.lupapj.data.model.scene.SceneObjectDefinition
import com.example.lupapj.data.model.scene.TileAnchorMode
import com.example.lupapj.data.model.scene.TileCoord
import com.example.lupapj.data.model.scene.TileFootprint
import com.example.lupapj.data.model.scene.WallAnchor
import com.example.lupapj.data.model.scene.toFloorAnchor
import kotlin.math.roundToInt

object RearrangeController {

    private const val MOVE_STEP_TILE = 1

    fun toggle(room: RoomUiState): RoomUiState {
        return room.copy(
            rearrangeMode = !room.rearrangeMode,
            selectedRearrangeObjectType = null
        )
    }

    fun selectObject(
        room: RoomUiState,
        objectType: RoomObjectType
    ): RoomUiState {
        return room.copy(
            selectedRearrangeObjectType = objectType
        )
    }

    fun moveUp(room: RoomUiState): RoomUiState {
        return move(room, 0, -MOVE_STEP_TILE)
    }

    fun moveDown(room: RoomUiState): RoomUiState {
        return move(room, 0, MOVE_STEP_TILE)
    }

    fun moveLeft(room: RoomUiState): RoomUiState {
        return move(room, -MOVE_STEP_TILE, 0)
    }

    fun moveRight(room: RoomUiState): RoomUiState {
        return move(room, MOVE_STEP_TILE, 0)
    }

    fun confirm(room: RoomUiState): RoomUiState {
        return room.copy(
            rearrangeMode = false,
            selectedRearrangeObjectType = null
        )
    }

    private fun move(
        room: RoomUiState,
        dxTile: Int,
        dyTile: Int
    ): RoomUiState {
        val selectedType = room.selectedRearrangeObjectType ?: return room
        val projectionSpec = room.sceneDefinition.projectionSpec

        val updatedObjects = room.sceneDefinition.objects.map { obj ->
            moveObjectIfSelected(
                obj = obj,
                selectedType = selectedType,
                dxTile = dxTile,
                dyTile = dyTile,
                projectionSpec = projectionSpec
            )
        }

        return room.copy(
            sceneDefinition = room.sceneDefinition.copy(
                objects = updatedObjects
            )
        )
    }

    private fun moveObjectIfSelected(
        obj: SceneObjectDefinition,
        selectedType: RoomObjectType,
        dxTile: Int,
        dyTile: Int,
        projectionSpec: IsoRoomProjectionSpec
    ): SceneObjectDefinition {
        if (obj.type != selectedType) return obj

        val currentPlacement = obj.tilePlacement
            ?: obj.anchor.toTilePlacementOrNull(projectionSpec)
            ?: return obj

        val roomWidthTiles = projectionSpec.roomWidthTiles.toInt()
        val roomDepthTiles = projectionSpec.roomDepthTiles.toInt()

        val maxX = roomWidthTiles - currentPlacement.footprint.widthTiles
        val maxY = roomDepthTiles - currentPlacement.footprint.depthTiles

        val newTile = TileCoord(
            x = (currentPlacement.tile.x + dxTile).coerceIn(0, maxX),
            y = (currentPlacement.tile.y + dyTile).coerceIn(0, maxY)
        )

        val newPlacement = currentPlacement.copy(
            tile = newTile
        )

        return obj.copy(
            anchor = newPlacement.toFloorAnchor(projectionSpec),
            tilePlacement = newPlacement
        )
    }

    private fun SceneAnchor.toTilePlacementOrNull(
        projectionSpec: IsoRoomProjectionSpec
    ): FloorTilePlacement? {
        return when (this) {
            is FloorAnchor -> {
                FloorTilePlacement(
                    tile = TileCoord(
                        x = (u * projectionSpec.roomWidthTiles).roundToInt()
                            .coerceIn(0, projectionSpec.roomWidthTiles.toInt() - 1),
                        y = (v * projectionSpec.roomDepthTiles).roundToInt()
                            .coerceIn(0, projectionSpec.roomDepthTiles.toInt() - 1)
                    ),
                    footprint = TileFootprint(1, 1),
                    anchorMode = TileAnchorMode.CENTER
                )
            }

            is WallAnchor -> null
        }
    }
}