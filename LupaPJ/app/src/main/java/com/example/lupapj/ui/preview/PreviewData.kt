package com.example.lupapj.ui.preview

import com.example.lupapj.data.mock.DemoScenes
import com.example.lupapj.data.model.AppPhase
import com.example.lupapj.data.model.PetAction
import com.example.lupapj.data.model.PetAppearance
import com.example.lupapj.data.model.PetTraits
import com.example.lupapj.data.model.PetStatus
import com.example.lupapj.data.model.initialRoomUiState
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.data.model.scene.PetSceneState
import com.example.lupapj.data.model.scene.RoomSceneRuntimeState
import com.example.lupapj.data.model.scene.initialHouseSceneState
import com.example.lupapj.viewmodel.AppUiState

val previewLoadingUiState = AppUiState(
    phase = AppPhase.LOGIN_PROMPT,
    loadingMessage = "로딩 완료",
    authPopupVisible = true
)

val previewRoomUiState = initialRoomUiState(
    sceneDefinition = DemoScenes.mainRoom
)

val previewRoomUiStateWithFood = initialRoomUiState(
    sceneDefinition = DemoScenes.mainRoom,
    houseSceneState = initialHouseSceneState(
        sceneId = DemoScenes.mainRoom.id
    ).copy(
        pet = PetSceneState(
            action = PetAction.EATING,
            anchor = FloorAnchor(u = 0.52f, v = 0.65f),
            appearance = PetAppearance(
                headSizeScale = 1.08f,
                bodySizeScale = 0.96f,
                eyeSizeScale = 1.12f,
                noseSizeScale = 0.92f,
                mouthSizeScale = 1.04f
            ),
            status = PetStatus(
                satiety = 95,
                vitality = 80,
                isEgg = false
            ),
            traits = PetTraits()
        ),
        currentSceneRuntime = RoomSceneRuntimeState(
            sceneId = DemoScenes.mainRoom.id,
            droppedFoodAnchor = FloorAnchor(u = 0.52f, v = 0.70f)
        )
    )
)
