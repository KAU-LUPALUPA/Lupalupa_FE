package com.example.lupapj.data.mock

import com.example.lupapj.data.model.PetAction
import com.example.lupapj.data.model.PetUiState
import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomPoint
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.initialRoomUiState
import com.example.lupapj.data.model.label
import com.example.lupapj.data.repository.RoomRepository
import kotlinx.coroutines.delay

class MockRoomRepository : RoomRepository {
    private var roomState = initialRoomUiState()

    override suspend fun getRoom(): RoomUiState {
        delay(180)
        return roomState
    }

    override suspend fun performObjectAction(objectType: RoomObjectType): RoomUiState {
        delay(220)
        roomState = when (objectType) {
            RoomObjectType.BED -> roomState.copy(
                feedMode = false,
                foodPosition = null,
                pet = PetUiState(
                    currentAction = PetAction.RESTING,
                    position = RoomPoint(0.24f, 0.61f)
                ),
                statusText = PetAction.RESTING.label
            )

            RoomObjectType.TOY_BOX -> roomState.copy(
                feedMode = false,
                pet = PetUiState(
                    currentAction = PetAction.PLAYING,
                    position = RoomPoint(0.60f, 0.61f)
                ),
                statusText = PetAction.PLAYING.label
            )

            RoomObjectType.FOOD_BAG -> roomState.copy(
                feedMode = true,
                statusText = "먹이 줄 위치를 바닥에서 선택하세요."
            )

            RoomObjectType.WINDOW -> roomState
        }
        return roomState
    }

    override suspend fun placeFood(position: RoomPoint): RoomUiState {
        delay(220)
        if (!roomState.feedMode) return roomState

        val clampedPoint = RoomPoint(
            xFraction = position.xFraction.coerceIn(0.18f, 0.72f),
            yFraction = position.yFraction.coerceIn(0.44f, 0.78f)
        )

        roomState = roomState.copy(
            feedMode = false,
            foodPosition = clampedPoint,
            pet = PetUiState(
                currentAction = PetAction.EATING,
                position = RoomPoint(
                    xFraction = clampedPoint.xFraction,
                    yFraction = (clampedPoint.yFraction - 0.05f).coerceAtLeast(0.38f)
                )
            ),
            statusText = PetAction.EATING.label
        )
        return roomState
    }
}
