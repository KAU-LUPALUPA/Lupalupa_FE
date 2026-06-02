package com.example.lupapj.viewmodel

import com.example.lupapj.data.model.AppPhase
import com.example.lupapj.data.model.GalleryImage // [추가됨]
import com.example.lupapj.data.model.MainMenuAction
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.friend.FriendHome
import com.example.lupapj.data.model.friend.FriendHomeInvitation
import com.example.lupapj.data.model.friend.FriendHomeVisitSession
import com.example.lupapj.data.model.friend.FriendMessage
import com.example.lupapj.data.model.friend.FriendRequest
import com.example.lupapj.data.model.friend.FriendSummary
import com.example.lupapj.data.model.friend.FriendUser
import com.example.lupapj.data.model.InventoryItem // [추가됨]
import com.example.lupapj.data.model.plaza.PlazaRoom // [보존] 팀원 작업 내용
import com.example.lupapj.data.repository.ContestGroupDetail
import com.example.lupapj.data.repository.ContestGroupSummary
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
    val isContestEntryUploading: Boolean = false,
    val contestUploadMessage: String? = null,
    val contestGroups: List<ContestGroupSummary> = emptyList(),
    val selectedContestGroup: ContestGroupDetail? = null,
    val isContestGroupsLoading: Boolean = false,
    val contestGroupMessage: String? = null,
    val isContestParticipating: Boolean = false,
    val contestMyEntryImageUrl: String? = null,
    val isContestVoteSubmitting: Boolean = false,
    val contestVoteMessage: String? = null,
    val myFriendProfile: FriendUser? = null,
    val friends: List<FriendSummary> = emptyList(),
    val receivedFriendRequests: List<FriendRequest> = emptyList(),
    val sentFriendRequests: List<FriendRequest> = emptyList(),
    val receivedHomeInvitations: List<FriendHomeInvitation> = emptyList(),
    val friendCodeInput: String = "",
    val isSendingFriendRequest: Boolean = false,
    val pendingHomeInvitationFriendId: String? = null,
    val isLoadingFriendHome: Boolean = false,
    val visitingFriendHome: FriendHome? = null,
    val activeHomeVisitSession: FriendHomeVisitSession? = null,
    val hostingHomeVisitSessions: List<FriendHomeVisitSession> = emptyList(),
    val friendRoomMessages: List<FriendMessage> = emptyList(),
    val friendMessageInput: String = "",
    val isSendingFriendMessage: Boolean = false,
    val friendFeedbackMessage: String? = null,
    val mailboxVisible: Boolean = false,
    val activePlaza: PlazaRoom? = null, // [보존] 팀원 작업 내용
    val plazaCodeInput: String = "", // [보존] 팀원 작업 내용
    val plazaMessageInput: String = "", // [보존] 팀원 작업 내용
    val isJoiningPlaza: Boolean = false, // [보존] 팀원 작업 내용
    val isSendingPlazaMessage: Boolean = false, // [보존] 팀원 작업 내용
    val plazaFeedbackMessage: String? = null, // [보존] 팀원 작업 내용
    
    // [추가됨(권)] 상점 및 재화 관련 상태
    val currencyAmount: Long = 0L, // [추가됨(권)] 현재 보유한 재화 양
    val shopItems: List<ShopItem> = emptyList(), // [추가됨(권)] 상점 진열대에 표시할 아이템 목록
    val purchasedItems: List<InventoryItem> = emptyList(), // [변경됨(권)] 보유 중인 상점 아이템 (인스턴스 정보 포함)
    val selectedShopItem: ShopItem? = null, // [추가됨(권)] 상세 화면에서 보고 있는 선택된 아이템
    val isPurchasing: Boolean = false, // [추가됨(권)] 구매 진행 중 로딩 상태 표시
    val shopFeedbackMessage: String? = null, // [추가됨(권)] 상점 구매 성공/실패 메시지 팝업용
    val userId: String? = null, // [수정됨(권)] 하트비트 및 오프라인 계산용 유저 ID 통합 관리
    val behaviorDebugInfo: com.example.lupapj.data.model.BehaviorDebugInfo = com.example.lupapj.data.model.BehaviorDebugInfo() // [추가됨(권)] 행동 엔진 디버그 정보
)
