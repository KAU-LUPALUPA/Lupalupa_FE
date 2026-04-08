package com.example.lupapj.ui.preview

import com.example.lupapj.data.model.AppPhase
import com.example.lupapj.data.model.RoomPoint
import com.example.lupapj.data.model.initialRoomUiState
import com.example.lupapj.viewmodel.AppUiState

val previewLoadingUiState = AppUiState(
    phase = AppPhase.MAIN_LOADING,
    loadingMessage = "로딩 완료",
    authPopupVisible = true
)

val previewRoomUiState = initialRoomUiState()

val previewRoomUiStateWithFood = initialRoomUiState().copy(
    foodPosition = RoomPoint(0.52f, 0.70f),
    statusText = "먹는 중"
)
