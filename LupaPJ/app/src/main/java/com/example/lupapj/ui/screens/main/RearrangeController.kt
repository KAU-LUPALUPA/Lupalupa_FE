package com.example.lupapj.ui.screens.main


import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomUiState

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
            selectedRearrangeObjectType = null
        )
    }

    private fun move(
        room: RoomUiState,
        dx: Float,
        dy: Float
    ): RoomUiState {

        val selectedType = room.selectedRearrangeObjectType
            ?: return room

        val updatedObjects = room.sceneDefinition.objects.map { obj ->

            if (obj.type != selectedType) {
                obj
            } else {
                obj.copy(
                    position = obj.position.copy(
                        x = (obj.position.x + dx).coerceIn(0f, 1f),
                        y = (obj.position.y + dy).coerceIn(0f, 1f)
                    )
                )
            }
        }

        return room.copy(
            sceneDefinition = room.sceneDefinition.copy(
                objects = updatedObjects
            )
        )
    }
}