package com.example.lupapj.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lupapj.data.model.AppPhase
import com.example.lupapj.data.model.BottomNavItem
import com.example.lupapj.data.model.DemoPetConditionPolicy
import com.example.lupapj.data.model.MainMenuAction
import com.example.lupapj.data.model.PetAction
import com.example.lupapj.data.model.PetConditionTickRemainder
import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.repository.AuthRepository
import com.example.lupapj.data.repository.FriendRepository
import com.example.lupapj.data.repository.GalleryRepository // [추가됨]
import com.example.lupapj.data.repository.RoomRepository
import com.example.lupapj.data.repository.CurrencyRepository // [추가됨(권)] 재화 리포지토리 의존성
import com.example.lupapj.data.repository.ShopRepository // [추가됨(권)] 상점 리포지토리 의존성
import android.graphics.Bitmap // [추가됨]
import com.example.lupapj.data.model.ShopItem // [추가됨(권)] 상점 아이템 모델 Import
import com.example.lupapj.data.model.friend.FRIEND_MESSAGE_MAX_LENGTH
import com.example.lupapj.data.model.friend.FriendOperationFailure
import com.example.lupapj.data.model.friend.FriendOperationResult
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.data.model.scene.PET_AUTONOMOUS_MOVE_DURATION_MS
import com.example.lupapj.data.model.scene.PetMovementState
import com.example.lupapj.data.model.advancePetCondition
import com.example.lupapj.data.model.applyFeedRecovery
import com.example.lupapj.data.model.scene.autonomousMovementProfileFor
import com.example.lupapj.data.model.scene.chooseAutonomousPetTarget
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.example.lupapj.ui.screens.main.RearrangeController
private const val FOOD_CONSUME_AFTER_TRAVEL_DELAY_MS = 900L
private const val FOOD_CONSUME_PAUSE_MS = 650L
private const val AUTONOMOUS_MOVEMENT_RETRY_DELAY_MS = 800L
private const val FRIEND_MESSAGE_POLL_INTERVAL_MS = 3_000L
private const val PET_CONDITION_TICK_INTERVAL_MS = 2_000L // [수정됨] 단기 틱 2초로 변경

class AppViewModel(
    private val authRepository: AuthRepository,
    private val roomRepository: RoomRepository,
    private val galleryRepository: GalleryRepository, // [추가됨]
    private val friendRepository: FriendRepository,
    private val currencyRepository: CurrencyRepository, // [추가됨(권)] ViewModel 파라미터로 추가
    private val shopRepository: ShopRepository // [추가됨(권)] ViewModel 파라미터로 추가
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private var roomBeforeRearrange: RoomUiState? = null
    private var behaviorConsecutiveTicks = 0
    
    // TODO: 아래 상태 변수들과 행동 시퀀스 로직을 PetBehaviorEngine 클래스로 분리 고려 (God Class 방지)
    private var petConditionRemainder = PetConditionTickRemainder()
    private var droppedFoodTicks = 0 // [수정됨(권)] 사료 방치 시간 (M, K 확률 계산용)
    private var droppedToyTicks = 0  // [수정됨(권)] 장난감 방치 시간 (M, K 확률 계산용)
    private var pendingFoodConsumeJob: Job? = null
    private var autonomousPetMovementJob: Job? = null
    private var friendMessagePollingJob: Job? = null
    private var petConditionJob: Job? = null

    init {
        runBootstrap()
        
        // 갤러리 이미지 상태 구독 연동
        viewModelScope.launch {
            galleryRepository.images.collect { images ->
                _uiState.update { it.copy(galleryImages = images) }
            }
        }
        viewModelScope.launch {
            friendRepository.myProfile.collect { profile ->
                _uiState.update { it.copy(myFriendProfile = profile) }
            }
        }
        viewModelScope.launch {
            friendRepository.friends.collect { friends ->
                _uiState.update { it.copy(friends = friends) }
            }
        }
        viewModelScope.launch {
            friendRepository.receivedRequests.collect { requests ->
                _uiState.update { it.copy(receivedFriendRequests = requests) }
            }
        }
        viewModelScope.launch {
            friendRepository.sentRequests.collect { requests ->
                _uiState.update { it.copy(sentFriendRequests = requests) }
            }
        }
        viewModelScope.launch {
            friendRepository.receivedHomeInvitations.collect { invitations ->
                _uiState.update { it.copy(receivedHomeInvitations = invitations) }
            }
        }
        viewModelScope.launch {
            friendRepository.friendMessages.collect { messagesByFriend ->
                _uiState.update { state ->
                    val friendUserId = state.visitingFriendHome?.owner?.userId
                    if (friendUserId == null) {
                        state
                    } else {
                        state.copy(
                            friendRoomMessages = messagesByFriend[friendUserId].orEmpty()
                        )
                    }
                }
            }
        }
        
        // [추가됨(권)] 재화 및 상점 상태 구독 블록. 리포지토리의 StateFlow 값을 구독하여 UiState에 반영합니다.
        viewModelScope.launch {
            currencyRepository.currencyState.collect { currency ->
                _uiState.update { it.copy(currencyAmount = currency.amount) }
            }
        }
        viewModelScope.launch {
            shopRepository.shopItems.collect { items ->
                _uiState.update { it.copy(shopItems = items) }
            }
        }
        viewModelScope.launch {
            shopRepository.inventory.collect { inventory ->
                _uiState.update { it.copy(purchasedItems = inventory) }
            }
        }
    }

    fun onKakaoLoginClick() {
        if (_uiState.value.isProcessingLogin) return

        _uiState.update {
            it.copy(isProcessingLogin = true)
        }
    }

    fun onKakaoLoginSuccess(
        accessToken: String,
        nickname: String?
    ) {
        if (accessToken.isBlank()) {
            _uiState.update {
                it.copy(
                    isProcessingLogin = false,
                    placeholderMessage = "로그인 토큰을 받지 못했습니다."
                )
            }
            return
        }

        // 실제 네트워크 통신을 위해 TokenManager에 저장
        com.example.lupapj.data.local.TokenManager.accessToken = accessToken

        viewModelScope.launch {
            _uiState.update {
                it.copy(isProcessingLogin = true)
            }

            val room = roomRepository.getRoom()
            val displayName = nickname ?: "사용자"

            _uiState.update {
                it.copy(
                    phase = AppPhase.ROOM,
                    authPopupVisible = false,
                    isProcessingLogin = false,
                    room = room,
                    placeholderMessage = "${displayName}님 환영합니다!"
                )
            }
            startPetConditionTicker()
        }
    }

    fun onButtonAClick() {
        when (_uiState.value.recentMainMenuAction) {
            MainMenuAction.SCREENSHOT -> onBottomNavItemClick(BottomNavItem.SCREENSHOT)
            MainMenuAction.GALLERY -> onBottomNavItemClick(BottomNavItem.GALLERY)
            MainMenuAction.CONTACTS -> onBottomNavItemClick(BottomNavItem.CONTACTS)
            MainMenuAction.SHOP -> onBottomNavItemClick(BottomNavItem.SHOP)
            MainMenuAction.PLAYGROUND -> openMinigame()
            null -> _uiState.update {
                it.copy(placeholderMessage = "최근 사용한 기능이 없습니다.")
            }
        }
    }

    fun onButtonBClick() {
        updateRoom { room ->
            room.copy(inventoryVisible = true)
        }
    }

    fun onSettingsClick() {
        _uiState.update {
            it.copy(placeholderMessage = "설정 기능은 다음 데모 범위에서 연결할게요.")
        }
    }

    fun onInventoryDismiss() {
        updateRoom { room ->
            room.copy(inventoryVisible = false)
        }
    }

    fun onRoomObjectClick(objectType: RoomObjectType) {
        val room = _uiState.value.room ?: return

        if (room.rearrangeMode) {
            updateRoom {
                com.example.lupapj.ui.screens.main.RearrangeController.selectObject(it, objectType)
            }
            return
        }

        if (objectType == RoomObjectType.WINDOW) return

        viewModelScope.launch {
            if (objectType == RoomObjectType.BED) {
                // [수정됨] 침대 클릭 시 즉시 침대로 이동 시퀀스 시작
                val roomState = _uiState.value.room ?: return@launch
                val bedAnchor = roomState.sceneDefinition.objects.find { it.type == RoomObjectType.BED }?.anchor as? FloorAnchor
                if (bedAnchor != null) {
                    startBedRestingSequence(bedAnchor)
                }
            } else {
                val nextRoom = roomRepository.performObjectAction(objectType)
                applyRepositoryRoom(nextRoom)
            }
        }
    }

    fun onRearrangeClick() {
        val room = _uiState.value.room ?: return

        if (!room.rearrangeMode) {
            roomBeforeRearrange = room

            updateRoom {
                RearrangeController.toggle(it)
            }

            _uiState.update {
                it.copy(
                    placeholderMessage = "현재 재배치 중입니다. 취소하면 변경 전 배치로 돌아갑니다."
                )
            }
        } else {
            onRearrangeCancel()
        }
    }
    fun onRearrangeCancel() {
        val previousRoom = roomBeforeRearrange

        updateRoom { room ->
            previousRoom?.copy(
                rearrangeMode = false,
                selectedRearrangeObjectType = null
            ) ?: room.copy(
                rearrangeMode = false,
                selectedRearrangeObjectType = null
            )
        }

        roomBeforeRearrange = null

        _uiState.update {
            it.copy(
                placeholderMessage = "재배치를 취소했습니다."
            )
        }
    }
    fun onRearrangeMoveUp() {
        updateRoom { room ->
            com.example.lupapj.ui.screens.main.RearrangeController.moveUp(room)
        }
    }

    fun onRearrangeMoveDown() {
        updateRoom { room ->
            com.example.lupapj.ui.screens.main.RearrangeController.moveDown(room)
        }
    }

    fun onRearrangeMoveLeft() {
        updateRoom { room ->
            com.example.lupapj.ui.screens.main.RearrangeController.moveLeft(room)
        }
    }

    fun onRearrangeMoveRight() {
        updateRoom { room ->
            com.example.lupapj.ui.screens.main.RearrangeController.moveRight(room)
        }
    }

    fun onRearrangeConfirm() {
        val room = _uiState.value.room ?: return

        viewModelScope.launch {
            val confirmedRoom = com.example.lupapj.ui.screens.main.RearrangeController.confirm(room)

            val savedRoom = roomRepository.saveRoomLayout(
                confirmedRoom
            )
            roomBeforeRearrange = null
            applyRepositoryRoom(savedRoom)
        }
    }

    fun onFloorTap(position: FloorAnchor) {
        val room = _uiState.value.room ?: return
        if (!room.feedMode && !room.toyMode) return

        viewModelScope.launch {
            val placingFood = room.feedMode
            val nextRoom = if (placingFood) {
                roomRepository.placeFood(position)
            } else {
                roomRepository.placeToy(position)
            }
            applyRepositoryRoom(nextRoom)

            if (placingFood && nextRoom.houseSceneState.currentSceneRuntime.droppedFoodAnchor != null) {
                // [수정됨] 직접 먹이 주기 시 즉시 반응하지 않고, 행동 엔진의 다음 틱에서 확률적으로 결정하도록 유도
                _uiState.update { it.copy(placeholderMessage = "바닥에 사료를 놓았습니다.") }
            }
        }
    }

    fun onBottomNavItemClick(item: BottomNavItem) {
        rememberRecentMainMenuAction(item.toMainMenuAction())
        when (item) {
            BottomNavItem.SHOP -> {
                // [추가됨(권)] 상점 탭 클릭 시 상점 아이템을 서버(mock)에서 새로고침 후 진입
                viewModelScope.launch {
                    shopRepository.fetchShopItems()
                }
                _uiState.update { it.copy(phase = AppPhase.SHOP) }
            }
            BottomNavItem.SCREENSHOT -> {
                // 스크린샷 모드 진입
                updateRoom { it.copy(isCameraMode = true, cameraZoom = 1f) }
            }
            BottomNavItem.GALLERY -> {
                // 갤러리 진입
                _uiState.update { it.copy(phase = AppPhase.GALLERY) }
            }
            BottomNavItem.CONTACTS -> {
                _uiState.update { it.copy(phase = AppPhase.FRIENDS) }
                refreshFriendOverview()
            }
        }
    }

    // [추가됨] 카메라 관련 로직
    fun exitCameraMode() {
        updateRoom { it.copy(isCameraMode = false, cameraZoom = 1f) }
    }

    fun setCameraZoom(zoom: Float) {
        updateRoom { it.copy(cameraZoom = zoom.coerceIn(1f, 3f)) } // 1x ~ 3x 제한
    }

    fun captureScreen(bitmap: Bitmap) {
        viewModelScope.launch {
            galleryRepository.saveImage(bitmap)
        }
    }

    fun toggleFavorite(imageId: String) {
        galleryRepository.toggleFavorite(imageId)
    }

    fun deleteImage(imageId: String) {
        galleryRepository.deleteImage(imageId)
    }

    fun exitGallery() {
        _uiState.update { it.copy(phase = AppPhase.ROOM) }
    }

    fun exitFriends() {
        _uiState.update { it.copy(phase = AppPhase.ROOM) }
    }

    fun openMailbox() {
        _uiState.update { it.copy(mailboxVisible = true) }
    }

    fun closeMailbox() {
        _uiState.update { it.copy(mailboxVisible = false) }
    }

    private fun refreshFriendOverview() {
        viewModelScope.launch {
            val result = friendRepository.refreshFriendOverview()
            if (result is FriendOperationResult.Failure) {
                _uiState.update {
                    it.copy(friendFeedbackMessage = result.reason.message)
                }
            }
        }
    }

    fun backToFriendsFromFriendRoom() {
        stopFriendMessagePolling()
        _uiState.update {
            it.copy(
                phase = AppPhase.FRIENDS,
                isLoadingFriendHome = false
            )
        }
    }

    fun returnHomeFromFriendRoom() {
        stopFriendMessagePolling()
        _uiState.update {
            it.copy(
                phase = AppPhase.ROOM,
                isLoadingFriendHome = false,
                visitingFriendHome = null,
                friendRoomMessages = emptyList(),
                friendMessageInput = "",
                isSendingFriendMessage = false
            )
        }
    }

    fun acceptHomeInvitation(invitationId: String) {
        val returnPhase = _uiState.value.phase
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    phase = AppPhase.FRIEND_ROOM,
                    isLoadingFriendHome = true,
                    visitingFriendHome = null,
                    friendRoomMessages = emptyList(),
                    friendMessageInput = "",
                    isSendingFriendMessage = false
                )
            }

            val result = friendRepository.acceptHomeInvitation(invitationId)
            val messagesResult = if (result is FriendOperationResult.Success) {
                friendRepository.getFriendMessages(result.value.owner.userId)
            } else {
                null
            }
            _uiState.update {
                when (result) {
                    is FriendOperationResult.Success -> it.copy(
                        isLoadingFriendHome = false,
                        mailboxVisible = false,
                        visitingFriendHome = result.value,
                        friendRoomMessages = when (messagesResult) {
                            is FriendOperationResult.Success -> messagesResult.value
                            else -> emptyList()
                        }
                    )

                    is FriendOperationResult.Failure -> it.copy(
                        phase = returnPhase,
                        isLoadingFriendHome = false,
                        visitingFriendHome = null,
                        friendFeedbackMessage = if (returnPhase == AppPhase.ROOM) {
                            it.friendFeedbackMessage
                        } else {
                            result.reason.message
                        },
                        placeholderMessage = if (returnPhase == AppPhase.ROOM) {
                            result.reason.message
                        } else {
                            it.placeholderMessage
                        }
                    )
                }
            }
            if (result is FriendOperationResult.Success) {
                startFriendMessagePolling(result.value.owner.userId)
            }
        }
    }

    fun rejectHomeInvitation(invitationId: String) {
        viewModelScope.launch {
            val result = friendRepository.rejectHomeInvitation(invitationId)
            _uiState.update {
                val message = result.feedbackMessage(
                    successMessage = "초대를 거절했어요."
                )
                it.copy(
                    friendFeedbackMessage = if (it.phase == AppPhase.ROOM) {
                        it.friendFeedbackMessage
                    } else {
                        message
                    },
                    placeholderMessage = if (it.phase == AppPhase.ROOM) {
                        message
                    } else {
                        it.placeholderMessage
                    }
                )
            }
        }
    }

    private fun startFriendMessagePolling(friendUserId: String) {
        friendMessagePollingJob?.cancel()
        friendMessagePollingJob = viewModelScope.launch {
            while (isActive) {
                delay(FRIEND_MESSAGE_POLL_INTERVAL_MS)
                val state = _uiState.value
                val activeFriendUserId = state.visitingFriendHome?.owner?.userId
                if (state.phase != AppPhase.FRIEND_ROOM || activeFriendUserId != friendUserId) {
                    break
                }
                friendRepository.getFriendMessages(friendUserId)
            }
        }
    }

    private fun stopFriendMessagePolling() {
        friendMessagePollingJob?.cancel()
        friendMessagePollingJob = null
    }

    fun onFriendMessageChange(input: String) {
        _uiState.update {
            it.copy(friendMessageInput = input.take(FRIEND_MESSAGE_MAX_LENGTH))
        }
    }

    fun sendFriendMessage() {
        val state = _uiState.value
        val friendUserId = state.visitingFriendHome?.owner?.userId ?: return
        if (state.isSendingFriendMessage) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingFriendMessage = true) }
            val result = friendRepository.sendFriendMessage(
                friendUserId = friendUserId,
                message = state.friendMessageInput
            )
            _uiState.update {
                when (result) {
                    is FriendOperationResult.Success -> it.copy(
                        friendMessageInput = "",
                        isSendingFriendMessage = false
                    )

                    is FriendOperationResult.Failure -> it.copy(
                        isSendingFriendMessage = false,
                        friendFeedbackMessage = result.reason.message
                    )
                }
            }
        }
    }

    fun onFriendCodeChange(input: String) {
        _uiState.update { it.copy(friendCodeInput = input) }
    }

    fun sendFriendRequest() {
        if (_uiState.value.isSendingFriendRequest) return

        viewModelScope.launch {
            val input = _uiState.value.friendCodeInput
            _uiState.update { it.copy(isSendingFriendRequest = true) }
            val result = friendRepository.sendFriendRequest(input)
            _uiState.update {
                it.copy(
                    friendCodeInput = if (result is FriendOperationResult.Success) "" else it.friendCodeInput,
                    isSendingFriendRequest = false,
                    friendFeedbackMessage = result.feedbackMessage(
                        successMessage = "친구 요청을 보냈어요."
                    )
                )
            }
        }
    }

    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            val result = friendRepository.acceptFriendRequest(requestId)
            _uiState.update {
                val message = result.feedbackMessage(
                    successMessage = "친구 요청을 수락했어요."
                )
                it.copy(
                    friendFeedbackMessage = if (it.phase == AppPhase.ROOM) {
                        it.friendFeedbackMessage
                    } else {
                        message
                    },
                    placeholderMessage = if (it.phase == AppPhase.ROOM) {
                        message
                    } else {
                        it.placeholderMessage
                    }
                )
            }
        }
    }

    fun rejectFriendRequest(requestId: String) {
        viewModelScope.launch {
            val result = friendRepository.rejectFriendRequest(requestId)
            _uiState.update {
                val message = result.feedbackMessage(
                    successMessage = "친구 요청을 거절했어요."
                )
                it.copy(
                    friendFeedbackMessage = if (it.phase == AppPhase.ROOM) {
                        it.friendFeedbackMessage
                    } else {
                        message
                    },
                    placeholderMessage = if (it.phase == AppPhase.ROOM) {
                        message
                    } else {
                        it.placeholderMessage
                    }
                )
            }
        }
    }

    fun cancelFriendRequest(requestId: String) {
        viewModelScope.launch {
            val result = friendRepository.cancelFriendRequest(requestId)
            _uiState.update {
                it.copy(
                    friendFeedbackMessage = result.feedbackMessage(
                        successMessage = "친구 요청을 취소했어요."
                    )
                )
            }
        }
    }

    fun removeFriend(friendUserId: String) {
        viewModelScope.launch {
            val result = friendRepository.removeFriend(friendUserId)
            _uiState.update {
                it.copy(
                    friendFeedbackMessage = result.feedbackMessage(
                        successMessage = "친구를 삭제했어요."
                    )
                )
            }
        }
    }

    fun onFriendFeedbackConsumed() {
        _uiState.update { it.copy(friendFeedbackMessage = null) }
    }

    fun onPlaceholderMessageConsumed() {
        _uiState.update { it.copy(placeholderMessage = null) }
    }
    
    // [추가됨(권)] 미니게임을 완료하여 재화를 획득하는 임시 로직입니다. 
    // 실제 미니게임이 구현되지 않아 버튼 클릭을 통해 즉시 재화를 획득하도록 API 호출을 모사합니다.
    fun earnCurrencyFromMinigame() {
        viewModelScope.launch {
            val result = currencyRepository.earnCurrency(50L, "MINIGAME") // [추가됨(권)] 한 번에 50원을 획득하도록 설정
            when (result) {
                is com.example.lupapj.data.model.CurrencyUpdateResult.Success -> {
                    // [추가됨(권)] 획득 성공 시 스낵바에 띄울 메시지를 설정합니다.
                    _uiState.update { it.copy(shopFeedbackMessage = "미니게임 보상으로 50원을 획득했습니다!") }
                }
                is com.example.lupapj.data.model.CurrencyUpdateResult.ValidationError -> {
                    _uiState.update { it.copy(shopFeedbackMessage = result.message) }
                }
                is com.example.lupapj.data.model.CurrencyUpdateResult.NetworkError -> {
                    _uiState.update { it.copy(shopFeedbackMessage = "네트워크 오류가 발생했습니다.") }
                }
                is com.example.lupapj.data.model.CurrencyUpdateResult.AuthError -> {
                    _uiState.update { it.copy(shopFeedbackMessage = "로그인이 만료되었습니다.") }
                }
            }
        }
    }

    // [추가됨(권)] 상점 목록에서 특정 아이템을 클릭했을 때의 액션입니다.
    fun selectShopItem(item: ShopItem) {
        _uiState.update { it.copy(phase = AppPhase.SHOP_DETAIL, selectedShopItem = item) }
    }

    // [추가됨(권)] 상점 세부 화면에서 아이템 구매 버튼을 클릭했을 때의 액션입니다.
    fun purchaseItem(itemId: String) {
        if (_uiState.value.isPurchasing) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true) }
            // [추가됨(권)] 구매 API를 호출하고 결과를 받아 처리합니다.
            val result = shopRepository.purchaseItem(itemId)
            _uiState.update { state ->
                state.copy(
                    isPurchasing = false,
                    // [추가됨(권)] 구매 성공/실패 여부에 따라 알맞은 팝업 메시지를 노출합니다.
                    shopFeedbackMessage = result.exceptionOrNull()?.message ?: "구매에 성공했습니다!"
                )
            }
        }
    }

    // [추가됨(권)] 상점 화면을 닫고 방(로비)으로 나갑니다.
    fun exitShop() {
        _uiState.update { it.copy(phase = AppPhase.ROOM) }
    }

    // [추가됨(권)] 세부 화면(치장 미리보기)을 닫고 다시 상점 목록으로 돌아갑니다.
    fun exitShopDetail() {
        _uiState.update { it.copy(phase = AppPhase.SHOP, selectedShopItem = null) }
    }

    // [추가됨(권)] 미니게임 화면으로 진입합니다.
    fun openMinigame() {
        _uiState.update {
            it.copy(
                phase = AppPhase.MINIGAME,
                recentMainMenuAction = MainMenuAction.PLAYGROUND
            )
        }
    }

    // [추가됨(권)] 미니게임 화면에서 방으로 나갑니다.
    fun exitMinigame() {
        _uiState.update { it.copy(phase = AppPhase.ROOM) }
    }

    // [추가됨(권)] 스낵바 팝업 메시지가 노출된 후 상태를 초기화합니다.
    fun consumeShopFeedback() {
        _uiState.update { it.copy(shopFeedbackMessage = null) }
    }

    // [추가됨(권)] 행동 디버그 윈도우 토글
    fun toggleBehaviorDebugWindow() {
        _uiState.update { 
            it.copy(behaviorDebugInfo = it.behaviorDebugInfo.copy(isVisible = !it.behaviorDebugInfo.isVisible)) 
        }
    }

    private fun runBootstrap() {
        viewModelScope.launch {
            delay(150)
            _uiState.update { it.copy(authPopupVisible = true) }
        }
    }

    private fun updateRoom(transform: (RoomUiState) -> RoomUiState) {
        _uiState.update { state ->
            val room = state.room ?: return@update state
            state.copy(room = transform(room))
        }
    }

    private fun calculateTileDistance(a: com.example.lupapj.data.model.scene.FloorAnchor, b: com.example.lupapj.data.model.scene.FloorAnchor, spec: com.example.lupapj.data.model.scene.IsoRoomProjectionSpec): Float {
        val dx = (a.u - b.u) * spec.roomWidthTiles
        val dy = (a.v - b.v) * spec.roomDepthTiles
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    // [추가됨(권)] 행동 상태 전이 평가 (매 초마다 호출됨)
    private fun evaluateBehaviorTransition() {
        val state = _uiState.value
        val room = state.room ?: return
        val pet = room.houseSceneState.pet

        if (state.phase != AppPhase.ROOM || room.feedMode || room.toyMode || room.isCameraMode || pet.status.isEgg || pet.movement.isMoving) {
            return
        }

        behaviorConsecutiveTicks++

        // [수정됨(권)] 먹기/놀기 상태는 애니메이션 시퀀스가 종료될 때까지 상태 기계의 개입을 차단함
        if (pet.action == PetAction.EATING || pet.action == PetAction.PLAYING) {
            return
        }

        val isCrisis = com.example.lupapj.data.model.BehaviorStateMachine.isCrisis(pet.status.satiety, pet.status.vitality)

        // [수정됨] 휴식 행동일 경우 10초(장기 틱) 주기로만 전이 평가
        // 긴급 상태(활력 < 30)에서의 휴식은 일정 수치 이상 회복될 때까지 이탈하지 않음
        val isRestingState = pet.action == PetAction.RESTING || pet.action == PetAction.BED_RESTING
        if (isRestingState) {
            if (isCrisis && pet.status.vitality < 30) return // 위기 상황에서는 30까지 무조건 휴식
            if (behaviorConsecutiveTicks % 5 != 0) return
        }

        // 성격 및 상태(위기/안정)에 따른 확률 변수 선택
        val (m, k) = if (isCrisis) {
            com.example.lupapj.data.model.BehaviorStateMachine.getCrisisTransitionParams(pet.personality)
        } else {
            // ACTIVE 배회 중 특수 로직: 목적지 도달 후 1초 대기 후 즉시 이탈
            if (pet.personality == com.example.lupapj.data.model.PetPersonality.ACTIVE && pet.action == PetAction.WALKING) {
                1.0f to 10.0f // 즉시 전이 유도
            } else {
                com.example.lupapj.data.model.BehaviorStateMachine.getStableTransitionParams(pet.personality, pet.action)
            }
        }

        val shouldTransition = com.example.lupapj.data.model.BehaviorStateMachine.checkTransitionProbability(behaviorConsecutiveTicks, m, k)
        val currentProb = m * (1f - kotlin.math.exp(-k * behaviorConsecutiveTicks)).toFloat()

        _uiState.update { s -> 
            s.copy(behaviorDebugInfo = s.behaviorDebugInfo.copy(
                consecutiveTicks = behaviorConsecutiveTicks,
                currentProbability = currentProb,
                isCrisis = isCrisis,
                mValue = m,
                kValue = k
            ))
        }
        
        if (shouldTransition || pet.action == PetAction.IDLE) {
            // [추가됨(권)] 놀이 종료 시 장난감 쓰러짐 상태로 변경
            if (pet.action == PetAction.PLAYING) {
                viewModelScope.launch {
                    val nextRoom = roomRepository.updateToyKnockedOver(true)
                    applyRepositoryRoom(nextRoom)
                }
            }
            behaviorConsecutiveTicks = 0
            
            val hasFood = room.houseSceneState.currentSceneRuntime.droppedFoodAnchor != null
            val hasToy = room.houseSceneState.currentSceneRuntime.droppedToyAnchor != null

            // [추가됨] 아이템 방치 시간 업데이트
            if (hasFood) droppedFoodTicks++ else droppedFoodTicks = 0
            if (hasToy) droppedToyTicks++ else droppedToyTicks = 0

            // [수정됨(권)] 아이템 발견 확률 체크 (M, K 수식 적용)
            // 휴식 중이거나 행동 중일 때는 주변 인지를 차단하여 버그 방지
            if (!isCrisis && !isRestingState && pet.action != PetAction.EATING && pet.action != PetAction.PLAYING) {
                val (mN, kN) = com.example.lupapj.data.model.BehaviorStateMachine.getNoticeParams(pet.personality)
                
                val discoveredFood = hasFood && com.example.lupapj.data.model.BehaviorStateMachine.checkTransitionProbability(droppedFoodTicks, mN, kN)
                if (discoveredFood) {
                    // [수정됨(권)] 사료 발견 시 즉시 식사 시퀀스로 전환
                    executeBehaviorAction(PetAction.EATING, pet)
                    droppedFoodTicks = 0
                    return
                }

                val discoveredToy = hasToy && com.example.lupapj.data.model.BehaviorStateMachine.checkTransitionProbability(droppedToyTicks, mN, kN)
                if (discoveredToy) {
                    // [수정됨(권)] 장난감 발견 시 즉시 놀이 시퀀스로 전환
                    executeBehaviorAction(PetAction.PLAYING, pet)
                    droppedToyTicks = 0
                    return
                }
            }

            val nextAction = if (isCrisis) {
                // 위기 시 생존 행동(식사/수면) 중 부족한 수치 우선 선택
                if (pet.status.satiety < pet.status.vitality) PetAction.EATING else PetAction.BED_RESTING
            } else {
                // [수정됨] 확률 수식을 사용하여 행동 결정 (기존 가중치 기반)
                com.example.lupapj.data.model.BehaviorStateMachine.rollStableBehavior(
                    personality = pet.personality,
                    satiety = pet.status.satiety,
                    hasFood = hasFood,
                    hasToy = hasToy
                )
            }
            executeBehaviorAction(nextAction, pet)
        }
    }

    private fun executeBehaviorAction(action: PetAction, pet: com.example.lupapj.data.model.scene.PetSceneState) {
        val room = _uiState.value.room ?: return
        val houseSceneState = room.houseSceneState
        
        when (action) {
            PetAction.WALKING -> {
                val profile = autonomousMovementProfileFor(pet.personality)
                val targetAnchor = chooseAutonomousPetTarget(
                    currentAnchor = pet.anchor,
                    sceneDefinition = room.sceneDefinition,
                    profile = profile,
                    random = Random.Default
                )
                if (targetAnchor != null) {
                    moveToTargetAndExecute(targetAnchor, pet, PetAction.IDLE, profile) // [수정됨] 도착 후 IDLE로 변경하여 재추첨 유도
                }
            }
            PetAction.RESTING -> {
                setPetAction(PetAction.RESTING)
            }
            PetAction.BED_RESTING -> {
                val bedAnchor = room.sceneDefinition.objects.find { it.type == RoomObjectType.BED }?.anchor as? FloorAnchor
                if (bedAnchor != null) {
                    // [수정됨] 침대 휴식 결정 시 즉시 침대로 이동
                    startBedRestingSequence(bedAnchor)
                } else {
                    setPetAction(PetAction.RESTING)
                }
            }
            PetAction.PLAYING -> {
                val targetAnchor = houseSceneState.currentSceneRuntime.droppedToyAnchor 
                    ?: room.sceneDefinition.objects.find { it.type == RoomObjectType.TOY_BOX }?.anchor?.let { it as? FloorAnchor }
                
                if (targetAnchor != null) {
                    // [추가됨] 침대 근처 필터링 로직 유지
                    val bedAnchor = room.sceneDefinition.objects.find { it.type == RoomObjectType.BED }?.anchor as? FloorAnchor
                    if (bedAnchor != null) {
                        val distToBed = calculateTileDistance(targetAnchor, bedAnchor, room.sceneDefinition.projectionSpec)
                        if (distToBed < 1.0f) { /* 스킵 로직 */ }
                    }

                    if (pet.personality == com.example.lupapj.data.model.PetPersonality.LAZY) {
                        val dist = calculateTileDistance(pet.anchor, targetAnchor, room.sceneDefinition.projectionSpec)
                        if (dist > 3f) {
                            setPetAction(PetAction.RESTING)
                            return
                        }
                    }
                    
                    // [수정됨] 통합된 놀이 시퀀스 호출
                    startPlayingSequence(targetAnchor)
                } else {
                    setPetAction(PetAction.IDLE)
                }
            }
            PetAction.EATING -> {
                val targetAnchor = houseSceneState.currentSceneRuntime.droppedFoodAnchor 
                    ?: room.sceneDefinition.objects.find { it.type == RoomObjectType.FOOD_BAG }?.anchor?.let { it as? FloorAnchor }
                if (targetAnchor != null) {
                    if (pet.personality == com.example.lupapj.data.model.PetPersonality.LAZY) {
                        val dist = calculateTileDistance(pet.anchor, targetAnchor, room.sceneDefinition.projectionSpec)
                        if (dist > 3f) {
                            setPetAction(PetAction.RESTING)
                            return
                        }
                    }
                    // [수정됨] 통합된 식사 시퀀스 호출
                    startEatingSequence(targetAnchor, skipMovement = false)
                } else {
                    setPetAction(PetAction.IDLE)
                }
            }
            else -> {
                setPetAction(PetAction.IDLE)
            }
        }
    }

    private fun moveToTargetAndExecute(
        targetAnchor: FloorAnchor,
        pet: com.example.lupapj.data.model.scene.PetSceneState,
        finalAction: PetAction,
        profile: com.example.lupapj.data.model.scene.PetAutonomousMovementProfile
    ) {
        viewModelScope.launch {
            // [추가됨] 침대에서 일어날 때 완전히 일어난 뒤 이동하도록 지연 시간 추가
            if (pet.isLyingSide) {
                setPetAction(PetAction.IDLE)
                delay(600L)
            }
            
            movePetAutonomously(
                targetAnchor = targetAnchor,
                movementState = com.example.lupapj.data.model.scene.PetMovementState(
                    targetAnchor = targetAnchor,
                    isMoving = true,
                    style = profile.style,
                    isAutonomous = true,
                    speedMultiplier = profile.speedMultiplier,
                    bouncePx = profile.bouncePx
                )
            )
            delay((com.example.lupapj.data.model.scene.PET_AUTONOMOUS_MOVE_DURATION_MS / profile.speedMultiplier).toLong())
            finishAutonomousPetMovement(targetAnchor)
            setPetAction(finalAction)
        }
    }


    private fun startPetConditionTicker() {
        if (petConditionJob?.isActive == true) return

        petConditionJob = viewModelScope.launch {
            while (isActive) {
                delay(PET_CONDITION_TICK_INTERVAL_MS)
                advanceCurrentPetCondition(elapsedSeconds = 2L) // [수정됨] 2초씩 진행
                evaluateBehaviorTransition() // [추가됨(권)] 매 틱(2초) 행동 전이 평가
            }
        }
    }

    private fun advanceCurrentPetCondition(elapsedSeconds: Long) {
        _uiState.update { state ->
            val room = state.room ?: return@update state
            val houseSceneState = room.houseSceneState
            val pet = houseSceneState.pet
            val result = advancePetCondition(
                status = pet.status,
                action = pet.action,
                elapsedSeconds = elapsedSeconds,
                remainder = petConditionRemainder,
                policy = DemoPetConditionPolicy
            )
            petConditionRemainder = result.remainder

            state.copy(
                room = room.copy(
                    houseSceneState = houseSceneState.copy(
                        pet = pet.copy(
                            status = result.status,
                            action = if (result.shouldStopResting) {
                                PetAction.IDLE
                            } else {
                                pet.action
                            }
                        )
                    )
                )
            )
        }
    }

    // canStartAutonomousPetMovement 제거됨 (BehaviorTicker로 대체)

    private fun movePetAutonomously(
        targetAnchor: FloorAnchor,
        movementState: PetMovementState
    ) {
        updateRoom { room ->
            val houseSceneState = room.houseSceneState
            room.copy(
                houseSceneState = houseSceneState.copy(
                    pet = houseSceneState.pet.copy(
                        action = PetAction.WALKING,
                        anchor = targetAnchor,
                        movement = movementState,
                        isLyingSide = false // 이동 시작 시 무조건 눕기 해제
                    )
                )
            )
        }
    }

    private fun finishAutonomousPetMovement(targetAnchor: FloorAnchor) {
        updateRoom { room ->
            val houseSceneState = room.houseSceneState
            val pet = houseSceneState.pet
            if (pet.action != PetAction.WALKING || pet.movement.targetAnchor != targetAnchor) {
                return@updateRoom room
            }

            room.copy(
                houseSceneState = houseSceneState.copy(
                    pet = pet.copy(
                        action = PetAction.IDLE,
                        movement = pet.movement.copy(isMoving = false)
                    )
                )
            )
        }
    }

    private fun rememberRecentMainMenuAction(action: MainMenuAction) {
        _uiState.update { it.copy(recentMainMenuAction = action) }
    }

    private fun BottomNavItem.toMainMenuAction(): MainMenuAction {
        return when (this) {
            BottomNavItem.SHOP -> MainMenuAction.SHOP
            BottomNavItem.SCREENSHOT -> MainMenuAction.SCREENSHOT
            BottomNavItem.CONTACTS -> MainMenuAction.CONTACTS
            BottomNavItem.GALLERY -> MainMenuAction.GALLERY
        }
    }

    private fun applyRepositoryRoom(repositoryRoom: RoomUiState) {
        val currentRoom = _uiState.value.room
        val currentPet = currentRoom?.houseSceneState?.pet
        
        // [수정됨(권)] 레포지토리 업데이트 시 펫의 일시적인 로컬 상태(위치, 이동, 휴식 중 여부)와 실시간 수치를 보존합니다.
        // 레포지토리의 수치가 초기값(80, 75)인 경우 로컬에서 시뮬레이션 중인 수치를 우선하여 UI 튀는 현상 방지.
        val repositoryPet = repositoryRoom.houseSceneState.pet
        val mergedPet = if (currentPet != null && repositoryPet.action == PetAction.IDLE && !repositoryPet.isLyingSide) {
            repositoryPet.copy(
                anchor = currentPet.anchor,
                action = currentPet.action,
                movement = currentPet.movement,
                isLyingSide = currentPet.isLyingSide,
                status = if (repositoryPet.status.satiety == 80 && repositoryPet.status.vitality == 75) {
                    currentPet.status 
                } else {
                    repositoryPet.status 
                }
            )
        } else {
            repositoryPet
        }

        val mergedRoom = repositoryRoom.copy(
            houseSceneState = repositoryRoom.houseSceneState.copy(pet = mergedPet),
            navBarVisible = currentRoom?.navBarVisible ?: false,
            inventoryVisible = currentRoom?.inventoryVisible ?: false
        )
        _uiState.update { it.copy(room = mergedRoom) }
    }

    private fun recoverPetSatietyFromFood() {
        petConditionRemainder = petConditionRemainder.copy(
            satietyDecaySeconds = 0L
        )
        updateRoom { room ->
            val houseSceneState = room.houseSceneState
            val pet = houseSceneState.pet
            room.copy(
                houseSceneState = houseSceneState.copy(
                    pet = pet.copy(
                        status = com.example.lupapj.data.model.applyFeedRecovery(
                            status = pet.status,
                            policy = com.example.lupapj.data.model.DemoPetConditionPolicy
                        )
                    )
                )
            )
        }
    }

    // [추가됨(권)] 거리에 따른 이동 지연 시간 계산
    private fun calculateMovementDelay(from: FloorAnchor, to: FloorAnchor, profile: com.example.lupapj.data.model.scene.PetAutonomousMovementProfile): Long {
        val room = _uiState.value.room ?: return 1000L
        val dist = calculateTileDistance(from, to, room.sceneDefinition.projectionSpec)
        // 1타일당 400ms 기준 (속도 배율 적용)
        val duration = (dist * 400L / profile.speedMultiplier).toLong()
        return duration.coerceIn(300L, 3000L)
    }

    // [추가됨(권)] 식사 시퀀스 통합 관리 (이동 -> 6초 대기 -> 소모 및 회복)
    private fun startEatingSequence(targetAnchor: FloorAnchor, skipMovement: Boolean = false) {
        pendingFoodConsumeJob?.cancel()
        pendingFoodConsumeJob = viewModelScope.launch {
            val pet = _uiState.value.room?.houseSceneState?.pet ?: return@launch
            val profile = autonomousMovementProfileFor(pet.personality)

            if (!skipMovement) {
                // [추가됨] 자고 있었다면 일어나는 시간 확보
                if (pet.isLyingSide) {
                    setPetAction(PetAction.IDLE)
                    delay(600L)
                }

                val delayMs = calculateMovementDelay(pet.anchor, targetAnchor, profile)
                movePetAutonomously(
                    targetAnchor = targetAnchor,
                    movementState = com.example.lupapj.data.model.scene.PetMovementState(
                        targetAnchor = targetAnchor,
                        isMoving = true,
                        style = profile.style,
                        isAutonomous = true,
                        speedMultiplier = profile.speedMultiplier,
                        bouncePx = profile.bouncePx
                    )
                )
                delay(delayMs)
                finishAutonomousPetMovement(targetAnchor)
            }

            setPetAction(PetAction.EATING)
            delay(6000L) // 고정 식사 시간

            if (_uiState.value.room?.houseSceneState?.pet?.action == PetAction.EATING) {
                val nextRoom = roomRepository.consumeFood()
                applyRepositoryRoom(nextRoom)
                recoverPetSatietyFromFood()
                setPetAction(PetAction.IDLE)
            }
        }
    }

    // [추가됨(권)] 놀이 시퀀스 통합 관리 (이동 -> 6초 대기 -> 장난감 쓰러뜨리기)
    private fun startPlayingSequence(targetAnchor: FloorAnchor) {
        pendingFoodConsumeJob?.cancel()
        pendingFoodConsumeJob = viewModelScope.launch {
            val pet = _uiState.value.room?.houseSceneState?.pet ?: return@launch
            val profile = autonomousMovementProfileFor(pet.personality)

            val delayMs = calculateMovementDelay(pet.anchor, targetAnchor, profile)
            movePetAutonomously(
                targetAnchor = targetAnchor,
                movementState = com.example.lupapj.data.model.scene.PetMovementState(
                    targetAnchor = targetAnchor,
                    isMoving = true,
                    style = profile.style,
                    isAutonomous = true,
                    speedMultiplier = profile.speedMultiplier,
                    bouncePx = profile.bouncePx
                )
            )
            delay(delayMs)
            finishAutonomousPetMovement(targetAnchor)

            setPetAction(PetAction.PLAYING)
            delay(6000L) // 고정 놀이 시간

            if (_uiState.value.room?.houseSceneState?.pet?.action == PetAction.PLAYING) {
                val nextRoom = roomRepository.updateToyKnockedOver(true)
                applyRepositoryRoom(nextRoom)
                setPetAction(PetAction.IDLE)
            }
        }
    }

    // [추가됨(권)] 침대 휴식 시퀀스 통합 관리 (이동 -> 도착 후 옆으로 눕기)
    private fun startBedRestingSequence(targetAnchor: FloorAnchor) {
        pendingFoodConsumeJob?.cancel()
        pendingFoodConsumeJob = viewModelScope.launch {
            val pet = _uiState.value.room?.houseSceneState?.pet ?: return@launch
            val profile = autonomousMovementProfileFor(pet.personality)

            val delayMs = calculateMovementDelay(pet.anchor, targetAnchor, profile)
            movePetAutonomously(
                targetAnchor = targetAnchor,
                movementState = com.example.lupapj.data.model.scene.PetMovementState(
                    targetAnchor = targetAnchor,
                    isMoving = true,
                    style = profile.style,
                    isAutonomous = false, // 침대 이동은 사용자 명령 또는 긴급 상황이므로 비자율 처리
                    speedMultiplier = profile.speedMultiplier,
                    bouncePx = profile.bouncePx
                )
            )
            delay(delayMs)
            finishAutonomousPetMovement(targetAnchor)

            // 도착 후 침대 휴식 상태 및 옆으로 눕기 플래그 설정
            _uiState.update { state ->
                val room = state.room ?: return@update state
                state.copy(
                    room = room.copy(
                        houseSceneState = room.houseSceneState.copy(
                            pet = room.houseSceneState.pet.copy(
                                action = PetAction.BED_RESTING,
                                isLyingSide = true
                            )
                        )
                    )
                )
            }
        }
    }

    private fun setPetAction(action: PetAction) {
        _uiState.update { state ->
            val room = state.room ?: return@update state
            state.copy(
                room = room.copy(
                    houseSceneState = room.houseSceneState.copy(
                        pet = room.houseSceneState.pet.copy(
                            action = action,
                            isLyingSide = false // 일반 액션 시에는 눕기 해제
                        )
                    )
                )
            )
        }
    }

    override fun onCleared() {
        pendingFoodConsumeJob?.cancel()
        autonomousPetMovementJob?.cancel()
        friendMessagePollingJob?.cancel()
        petConditionJob?.cancel()
        super.onCleared()
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val roomRepository: RoomRepository,
        private val galleryRepository: GalleryRepository, // [추가됨]
        private val friendRepository: FriendRepository,
        private val currencyRepository: CurrencyRepository, // [추가됨(권)] 팩토리 파라미터 추가
        private val shopRepository: ShopRepository // [추가됨(권)] 팩토리 파라미터 추가
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(
                authRepository = authRepository,
                roomRepository = roomRepository,
                galleryRepository = galleryRepository,
                friendRepository = friendRepository,
                currencyRepository = currencyRepository,
                shopRepository = shopRepository
            ) as T
        }
    }
}

private fun FriendOperationResult<*>.feedbackMessage(
    successMessage: String
): String {
    return when (this) {
        is FriendOperationResult.Success -> successMessage
        is FriendOperationResult.Failure -> reason.message
    }
}

private val FriendOperationFailure.message: String
    get() = when (this) {
        FriendOperationFailure.EMPTY_CODE -> "친구 코드를 입력해주세요."
        FriendOperationFailure.EMPTY_MESSAGE -> "메시지를 입력해주세요."
        FriendOperationFailure.MESSAGE_TOO_LONG -> "메시지는 ${FRIEND_MESSAGE_MAX_LENGTH}자까지 보낼 수 있어요."
        FriendOperationFailure.SELF_CODE -> "내 친구 코드는 입력할 수 없어요."
        FriendOperationFailure.USER_NOT_FOUND -> "해당 친구 코드를 찾을 수 없어요."
        FriendOperationFailure.ALREADY_FRIENDS -> "이미 친구인 유저예요."
        FriendOperationFailure.REQUEST_ALREADY_SENT -> "이미 친구 요청을 보냈어요."
        FriendOperationFailure.REQUEST_ALREADY_RECEIVED -> "이미 받은 요청이 있어요."
        FriendOperationFailure.REQUEST_NOT_FOUND -> "친구 요청을 찾을 수 없어요."
        FriendOperationFailure.REQUEST_NOT_PENDING -> "이미 처리된 요청이에요."
        FriendOperationFailure.FRIEND_NOT_FOUND -> "친구를 찾을 수 없어요."
        FriendOperationFailure.NOT_FRIENDS -> "친구 집은 친구만 방문할 수 있어요."
        FriendOperationFailure.HOME_INVITATION_ALREADY_SENT -> "이미 보낸 집 초대가 있어요."
        FriendOperationFailure.HOME_INVITATION_NOT_FOUND -> "집 초대를 찾을 수 없어요."
        FriendOperationFailure.HOME_INVITATION_NOT_PENDING -> "이미 처리된 집 초대예요."
        FriendOperationFailure.NOT_HOME_INVITATION_RECEIVER -> "내가 받은 집 초대만 수락할 수 있어요."
        FriendOperationFailure.NOT_HOME_INVITATION_SENDER -> "내가 보낸 집 초대만 취소할 수 있어요."
        FriendOperationFailure.FRIEND_HOME_UNAVAILABLE -> "친구 집을 불러올 수 없어요."
        FriendOperationFailure.BLOCKED -> "초대를 수락해야 친구 집에 방문할 수 있어요."
        FriendOperationFailure.UNKNOWN -> "친구 기능을 잠시 사용할 수 없어요."
    }
