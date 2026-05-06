package com.example.lupapj.ui.screens.main

import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.data.model.scene.SceneObjectDefinition
import com.example.lupapj.data.model.scene.WallAnchor

object RearrangeController {

    private const val MOVE_STEP = 0.05f

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
        return move(room, 0f, -MOVE_STEP)
    }

    fun moveDown(room: RoomUiState): RoomUiState {
        return move(room, 0f, MOVE_STEP)
    }

    fun moveLeft(room: RoomUiState): RoomUiState {
        return move(room, -MOVE_STEP, 0f)
    }

    fun moveRight(room: RoomUiState): RoomUiState {
        return move(room, MOVE_STEP, 0f)
    }

    fun confirm(room: RoomUiState): RoomUiState {
        return room.copy(
            rearrangeMode = false,
            selectedRearrangeObjectType = null
        )
    }

    private fun move(
        room: RoomUiState,
        dx: Float,
        dy: Float
    ): RoomUiState {
        val selectedType = room.selectedRearrangeObjectType ?: return room

        val updatedObjects = room.sceneDefinition.objects.map { obj ->
            moveObjectIfSelected(obj, selectedType, dx, dy)
        }

        val updatedFixedDecor = room.sceneDefinition.fixedDecor.map { obj ->
            moveObjectIfSelected(obj, selectedType, dx, dy)
        }

        return room.copy(
            sceneDefinition = room.sceneDefinition.copy(
                objects = updatedObjects,
                fixedDecor = updatedFixedDecor
            )
        )
    }

    private fun moveObjectIfSelected(
        obj: SceneObjectDefinition,
        selectedType: RoomObjectType,
        dx: Float,
        dy: Float
    ): SceneObjectDefinition {
        if (obj.type != selectedType) return obj

        val updatedAnchor = when (val anchor = obj.anchor) {
            is FloorAnchor -> {
                anchor.copy(
                    u = (anchor.u + dx).coerceIn(0f, 1f),
                    v = (anchor.v + dy).coerceIn(0f, 1f)
                )
            }

            is WallAnchor -> {
                anchor.copy(
                    u = (anchor.u + dx).coerceIn(0f, 1f),
                    v = (anchor.v + dy).coerceIn(0f, 1f)
                )
            }
        }

        return obj.copy(
            anchor = updatedAnchor,
            tilePlacement = null
        )
    }
}