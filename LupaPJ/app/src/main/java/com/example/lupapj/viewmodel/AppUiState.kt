package com.example.lupapj.viewmodel

import com.example.lupapj.data.model.AppPhase
import com.example.lupapj.data.model.GalleryImage // [추가됨]
import com.example.lupapj.data.model.RoomUiState

data class AppUiState(
    val phase: AppPhase = AppPhase.MAIN_LOADING,
    val loadingMessage: String = "로딩 중...",
    val authPopupVisible: Boolean = false,
    val isProcessingLogin: Boolean = false,
    val room: RoomUiState? = null,
    val placeholderMessage: String? = null,
    val galleryImages: List<GalleryImage> = emptyList() // [추가됨] 갤러리 상태 관리를 위한 이미지 목록
)
