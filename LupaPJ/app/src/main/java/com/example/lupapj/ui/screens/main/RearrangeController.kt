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
                val nextU = anchor.u + dx
                val nextV = anchor.v + dy

                if (!isSafeFloorPosition(nextU, nextV)) {
                    return obj
                }

                anchor.copy(
                    u = nextU,
                    v = nextV
                )
            }

            is WallAnchor -> {
                // 벽에 붙은 오브젝트는 재배치로 움직이지 않게 막음
                return obj
            }
        }

        return obj.copy(
            anchor = updatedAnchor,
            tilePlacement = null
        )
    }
    private fun isSafeFloorPosition(
        u: Float,
        v: Float
    ): Boolean {
        val minU = 0.20f
        val maxU = 0.80f
        val minV = 0.28f
        val maxV = 0.70f

        if (u < minU || u > maxU) return false
        if (v < minV || v > maxV) return false

        return true
    }
}