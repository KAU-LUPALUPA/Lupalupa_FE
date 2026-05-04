package com.example.lupapj.viewmodel

import com.example.lupapj.data.model.AppPhase
import com.example.lupapj.data.model.GalleryImage // [추가됨]
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.friend.FriendHome
import com.example.lupapj.data.model.friend.FriendMessage
import com.example.lupapj.data.model.friend.FriendRequest
import com.example.lupapj.data.model.friend.FriendSummary
import com.example.lupapj.data.model.friend.FriendUser

data class AppUiState(
    val phase: AppPhase = AppPhase.MAIN_LOADING,
    val loadingMessage: String = "로딩 중...",
    val authPopupVisible: Boolean = false,
    val isProcessingLogin: Boolean = false,
    val room: RoomUiState? = null,
    val placeholderMessage: String? = null,
    val galleryImages: List<GalleryImage> = emptyList(), // [추가됨] 갤러리 상태 관리를 위한 이미지 목록
    val myFriendProfile: FriendUser? = null,
    val friends: List<FriendSummary> = emptyList(),
    val receivedFriendRequests: List<FriendRequest> = emptyList(),
    val sentFriendRequests: List<FriendRequest> = emptyList(),
    val friendCodeInput: String = "",
    val isSendingFriendRequest: Boolean = false,
    val isLoadingFriendHome: Boolean = false,
    val visitingFriendHome: FriendHome? = null,
    val friendRoomMessages: List<FriendMessage> = emptyList(),
    val friendMessageInput: String = "",
    val isSendingFriendMessage: Boolean = false,
    val friendFeedbackMessage: String? = null
)
