package com.example.lupapj.viewmodel

import com.example.lupapj.data.model.AppPhase
import com.example.lupapj.data.model.RoomUiState

data class AppUiState(
    val phase: AppPhase = AppPhase.MAIN_LOADING,
    val loadingMessage: String = "로딩 중...",
    val authPopupVisible: Boolean = false,
    val isProcessingLogin: Boolean = false,
    val room: RoomUiState? = null,
    val placeholderMessage: String? = null
)
