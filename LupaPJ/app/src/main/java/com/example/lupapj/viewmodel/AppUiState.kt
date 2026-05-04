package com.example.lupapj.viewmodel

import com.example.lupapj.data.model.AppPhase
import com.example.lupapj.data.model.GalleryImage // [추가됨]
import com.example.lupapj.data.model.MainMenuAction
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.friend.FriendHome
import com.example.lupapj.data.model.friend.FriendMessage
import com.example.lupapj.data.model.friend.FriendRequest
import com.example.lupapj.data.model.friend.FriendSummary
import com.example.lupapj.data.model.friend.FriendUser
import com.example.lupapj.data.model.ShopItem // [추가됨(권)] 상점 아이템 데이터 모델 Import

data class AppUiState(
    val phase: AppPhase = AppPhase.MAIN_LOADING,
    val loadingMessage: String = "로딩 중...",
    val authPopupVisible: Boolean = false,
    val isProcessingLogin: Boolean = false,
    val room: RoomUiState? = null,
    val recentMainMenuAction: MainMenuAction? = null,
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
    val friendFeedbackMessage: String? = null,
    
    // [추가됨(권)] 상점 및 재화 관련 상태
    val currencyAmount: Int = 0, // [추가됨(권)] 현재 보유한 재화 양
    val shopItems: List<ShopItem> = emptyList(), // [추가됨(권)] 상점 진열대에 표시할 아이템 목록
    val purchasedItemIds: List<String> = emptyList(), // [추가됨(권)] 보유 중인 상점 아이템 ID 목록
    val selectedShopItem: ShopItem? = null, // [추가됨(권)] 상세 화면에서 보고 있는 선택된 아이템
    val isPurchasing: Boolean = false, // [추가됨(권)] 구매 진행 중 로딩 상태 표시
    val shopFeedbackMessage: String? = null // [추가됨(권)] 상점 구매 성공/실패 메시지 팝업용
)
