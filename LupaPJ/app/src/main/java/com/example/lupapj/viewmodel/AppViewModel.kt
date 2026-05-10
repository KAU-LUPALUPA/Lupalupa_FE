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
import com.example.lupapj.data.model.PetUiState
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
import com.example.lupapj.data.model.plaza.PLAZA_MESSAGE_MAX_LENGTH
import com.example.lupapj.data.model.plaza.PlazaOperationFailure
import com.example.lupapj.data.model.plaza.PlazaOperationResult
import com.example.lupapj.data.model.plaza.PlazaPetSnapshot
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.data.model.scene.PET_AUTONOMOUS_MOVE_DURATION_MS
import com.example.lupapj.data.model.scene.PetMovementState
import com.example.lupapj.data.model.advancePetCondition
import com.example.lupapj.data.model.applyFeedRecovery
import com.example.lupapj.data.model.scene.autonomousMovementProfileFor
import com.example.lupapj.data.model.scene.chooseAutonomousPetTarget
import com.example.lupapj.data.repository.PlazaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val FOOD_CONSUME_AFTER_TRAVEL_DELAY_MS = 900L
private const val FOOD_CONSUME_PAUSE_MS = 650L
private const val AUTONOMOUS_MOVEMENT_RETRY_DELAY_MS = 800L
private const val FRIEND_MESSAGE_POLL_INTERVAL_MS = 3_000L
private const val PET_CONDITION_TICK_INTERVAL_MS = 1_000L
private const val DEV_LOGIN_ACCESS_TOKEN = "dev-access-token"
private const val DEV_LOGIN_NICKNAME = "개발자"

class AppViewModel(
    private val authRepository: AuthRepository,
    private val roomRepository: RoomRepository,
    private val galleryRepository: GalleryRepository, // [추가됨]
    private val friendRepository: FriendRepository,
    private val plazaRepository: PlazaRepository,
    private val currencyRepository: CurrencyRepository, // [추가됨(권)] ViewModel 파라미터로 추가
    private val shopRepository: ShopRepository // [추가됨(권)] ViewModel 파라미터로 추가
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private var pendingFoodConsumeJob: Job? = null
    private var autonomousPetMovementJob: Job? = null
    private var friendMessagePollingJob: Job? = null
    private var petConditionJob: Job? = null
    private var plazaJoinJob: Job? = null
    private var plazaMessageSendJob: Job? = null
    private var plazaJoinRequestId = 0
    private var petConditionRemainder = PetConditionTickRemainder()

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
        viewModelScope.launch {
            plazaRepository.activePlaza.collect { plaza ->
                _uiState.update { state ->
                    val currentPlaza = state.activePlaza
                    if (
                        plaza != null &&
                        currentPlaza?.plazaId == plaza.plazaId &&
                        plaza.roomRevision <= currentPlaza.roomRevision
                    ) {
                        state
                    } else {
                        state.copy(activePlaza = plaza)
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
                _uiState.update { it.copy(purchasedItemIds = inventory) }
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

            startAutonomousPetMovement()
            startPetConditionTicker()
        }
    }

    fun onDevLoginClick() {
        if (_uiState.value.isProcessingLogin) return

        onKakaoLoginSuccess(
            accessToken = DEV_LOGIN_ACCESS_TOKEN,
            nickname = DEV_LOGIN_NICKNAME
        )
    }

    fun onButtonAClick() {
        when (_uiState.value.recentMainMenuAction) {
            MainMenuAction.SCREENSHOT -> onBottomNavItemClick(BottomNavItem.SCREENSHOT)
            MainMenuAction.GALLERY -> onBottomNavItemClick(BottomNavItem.GALLERY)
            MainMenuAction.CONTACTS -> onBottomNavItemClick(BottomNavItem.CONTACTS)
            MainMenuAction.SHOP -> onBottomNavItemClick(BottomNavItem.SHOP)
            MainMenuAction.PLAYGROUND -> openPlaza()
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
            val nextRoom = roomRepository.performObjectAction(objectType)
            applyRepositoryRoom(nextRoom)
        }
    }

    fun onRearrangeClick() {
        updateRoom { room ->
            com.example.lupapj.ui.screens.main.RearrangeController.toggle(room)
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

            if (placingFood && nextRoom.droppedFoodAnchor != null) {
                scheduleFoodConsumption()
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

    fun openPlaza() {
        _uiState.update {
            it.copy(
                phase = AppPhase.PLAZA,
                recentMainMenuAction = MainMenuAction.PLAYGROUND,
                plazaFeedbackMessage = null
            )
        }
    }

    fun returnHomeFromPlaza() {
        plazaJoinRequestId += 1
        plazaJoinJob?.cancel()
        plazaJoinJob = null
        cancelPlazaMessageSend()
        val shouldLeavePlaza = _uiState.value.activePlaza != null

        _uiState.update {
            it.copy(
                phase = AppPhase.ROOM,
                activePlaza = null,
                plazaCodeInput = "",
                plazaMessageInput = "",
                isJoiningPlaza = false,
                isSendingPlazaMessage = false,
                plazaFeedbackMessage = null
            )
        }

        if (!shouldLeavePlaza) return
        viewModelScope.launch {
            plazaRepository.leavePlaza()
        }
    }

    fun onPlazaCodeChange(input: String) {
        _uiState.update {
            it.copy(plazaCodeInput = input.take(12).uppercase())
        }
    }

    fun joinRandomPlaza() {
        if (_uiState.value.isJoiningPlaza) return

        val requestId = ++plazaJoinRequestId
        plazaJoinJob?.cancel()
        cancelPlazaMessageSend()
        plazaJoinJob = viewModelScope.launch {
            val participantProfile = currentPlazaParticipantProfile()
            _uiState.update {
                it.copy(
                    isJoiningPlaza = true,
                    plazaFeedbackMessage = null
                )
            }
            val result = plazaRepository.joinRandomPlaza(
                currentUserId = participantProfile.userId,
                nickname = participantProfile.nickname,
                pet = participantProfile.pet
            )
            if (!isActivePlazaJoinRequest(requestId)) {
                if (
                    result is PlazaOperationResult.Success &&
                    plazaRepository.activePlaza.value?.plazaId == result.value.plazaId
                ) {
                    plazaRepository.leavePlaza()
                }
                return@launch
            }
            _uiState.update {
                when (result) {
                    is PlazaOperationResult.Success -> it.copy(
                        isJoiningPlaza = false,
                        plazaCodeInput = "",
                        plazaFeedbackMessage = null
                    )

                    is PlazaOperationResult.Failure -> it.copy(
                        isJoiningPlaza = false,
                        plazaFeedbackMessage = result.reason.message
                    )
                }
            }
        }
    }

    fun joinPlazaByCode() {
        if (_uiState.value.isJoiningPlaza) return

        val requestId = ++plazaJoinRequestId
        plazaJoinJob?.cancel()
        cancelPlazaMessageSend()
        plazaJoinJob = viewModelScope.launch {
            val state = _uiState.value
            val participantProfile = currentPlazaParticipantProfile()
            _uiState.update {
                it.copy(
                    isJoiningPlaza = true,
                    plazaFeedbackMessage = null
                )
            }
            val result = plazaRepository.joinPlazaByCode(
                codeInput = state.plazaCodeInput,
                currentUserId = participantProfile.userId,
                nickname = participantProfile.nickname,
                pet = participantProfile.pet
            )
            if (!isActivePlazaJoinRequest(requestId)) {
                if (
                    result is PlazaOperationResult.Success &&
                    plazaRepository.activePlaza.value?.plazaId == result.value.plazaId
                ) {
                    plazaRepository.leavePlaza()
                }
                return@launch
            }
            _uiState.update {
                when (result) {
                    is PlazaOperationResult.Success -> it.copy(
                        isJoiningPlaza = false,
                        plazaCodeInput = "",
                        plazaFeedbackMessage = null
                    )

                    is PlazaOperationResult.Failure -> it.copy(
                        isJoiningPlaza = false,
                        plazaFeedbackMessage = result.reason.message
                    )
                }
            }
        }
    }

    fun leaveCurrentPlaza() {
        plazaJoinRequestId += 1
        plazaJoinJob?.cancel()
        plazaJoinJob = null
        cancelPlazaMessageSend()
        val leavingPlaza = _uiState.value.activePlaza

        _uiState.update {
            it.copy(
                activePlaza = null,
                plazaMessageInput = "",
                isJoiningPlaza = false,
                isSendingPlazaMessage = false
            )
        }

        viewModelScope.launch {
            val result = plazaRepository.leavePlaza()
            _uiState.update {
                when (result) {
                    is PlazaOperationResult.Success -> it.copy(
                        plazaFeedbackMessage = "광장에서 나왔어요."
                    )

                    is PlazaOperationResult.Failure -> it.copy(
                        activePlaza = leavingPlaza,
                        plazaFeedbackMessage = result.reason.message
                    )
                }
            }
        }
    }

    fun onPlazaMessageChange(input: String) {
        _uiState.update {
            it.copy(plazaMessageInput = input.take(PLAZA_MESSAGE_MAX_LENGTH))
        }
    }

    fun sendPlazaMessage() {
        val state = _uiState.value
        if (state.isSendingPlazaMessage) return
        val sendingPlazaId = state.activePlaza?.plazaId
            ?: return

        plazaMessageSendJob = viewModelScope.launch {
            _uiState.update { it.copy(isSendingPlazaMessage = true) }
            val result = plazaRepository.sendMessage(state.plazaMessageInput)

            _uiState.update { latestState ->
                if (
                    latestState.phase != AppPhase.PLAZA ||
                    latestState.activePlaza?.plazaId != sendingPlazaId
                ) {
                    return@update latestState
                }

                when (result) {
                    is PlazaOperationResult.Success -> latestState.copy(
                        plazaMessageInput = "",
                        isSendingPlazaMessage = false,
                        plazaFeedbackMessage = null
                    )

                    is PlazaOperationResult.Failure -> latestState.copy(
                        isSendingPlazaMessage = false,
                        plazaFeedbackMessage = result.reason.message
                    )
                }
            }
        }
    }

    fun onPlazaFeedbackConsumed() {
        _uiState.update { it.copy(plazaFeedbackMessage = null) }
    }

    private fun isActivePlazaJoinRequest(requestId: Int): Boolean {
        val state = _uiState.value
        return plazaJoinRequestId == requestId && state.phase == AppPhase.PLAZA
    }

    private fun cancelPlazaMessageSend() {
        plazaMessageSendJob?.cancel()
        plazaMessageSendJob = null
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

    private fun currentPlazaParticipantProfile(): PlazaParticipantProfile {
        val state = _uiState.value
        val friendProfile = state.myFriendProfile
        val roomPet = state.room?.pet ?: PetUiState()
        val ownerUserId = friendProfile?.userId ?: roomPet.ownerUserId

        return PlazaParticipantProfile(
            userId = ownerUserId,
            nickname = friendProfile?.nickname ?: "나",
            pet = roomPet.toPlazaPetSnapshot(ownerUserId = ownerUserId)
        )
    }

    private fun startAutonomousPetMovement() {
        if (autonomousPetMovementJob?.isActive == true) return

        autonomousPetMovementJob = viewModelScope.launch {
            while (isActive) {
                val state = _uiState.value
                val room = state.room

                if (room == null || !state.canStartAutonomousPetMovement()) {
                    delay(AUTONOMOUS_MOVEMENT_RETRY_DELAY_MS)
                    continue
                }

                val profile = autonomousMovementProfileFor(room.houseSceneState.pet.personality)
                delay(profile.nextIdleDelayMillis(Random.Default))

                val latestState = _uiState.value
                val latestRoom = latestState.room
                if (latestRoom == null || !latestState.canStartAutonomousPetMovement()) {
                    continue
                }

                val currentPet = latestRoom.houseSceneState.pet
                val targetAnchor = chooseAutonomousPetTarget(
                    currentAnchor = currentPet.anchor,
                    sceneDefinition = latestRoom.sceneDefinition,
                    profile = profile,
                    random = Random.Default
                )

                if (targetAnchor == null) {
                    delay(AUTONOMOUS_MOVEMENT_RETRY_DELAY_MS)
                    continue
                }

                movePetAutonomously(
                    targetAnchor = targetAnchor,
                    movementState = PetMovementState(
                        targetAnchor = targetAnchor,
                        isMoving = true,
                        style = profile.style,
                        isAutonomous = true
                    )
                )
                delay(PET_AUTONOMOUS_MOVE_DURATION_MS)
                finishAutonomousPetMovement(targetAnchor)
            }
        }
    }

    private fun startPetConditionTicker() {
        if (petConditionJob?.isActive == true) return

        petConditionJob = viewModelScope.launch {
            while (isActive) {
                delay(PET_CONDITION_TICK_INTERVAL_MS)
                advanceCurrentPetCondition(elapsedSeconds = 1L)
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

    private fun AppUiState.canStartAutonomousPetMovement(): Boolean {
        val room = room ?: return false
        val pet = room.houseSceneState.pet

        return phase == AppPhase.ROOM &&
            !room.feedMode &&
            !room.toyMode &&
            !room.isCameraMode &&
            !pet.status.isEgg &&
            pet.action == PetAction.IDLE
    }

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
                        movement = movementState
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
        val currentPetStatus = currentRoom?.houseSceneState?.pet?.status
        val mergedHouseSceneState = if (currentPetStatus == null) {
            repositoryRoom.houseSceneState
        } else {
            val repositoryPet = repositoryRoom.houseSceneState.pet
            repositoryRoom.houseSceneState.copy(
                pet = repositoryPet.copy(status = currentPetStatus)
            )
        }
        val mergedRoom = repositoryRoom.copy(
            houseSceneState = mergedHouseSceneState,
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
                        status = applyFeedRecovery(
                            status = pet.status,
                            policy = DemoPetConditionPolicy
                        )
                    )
                )
            )
        }
    }

    private fun scheduleFoodConsumption() {
        pendingFoodConsumeJob?.cancel()
        pendingFoodConsumeJob = viewModelScope.launch {
            delay(FOOD_CONSUME_AFTER_TRAVEL_DELAY_MS + FOOD_CONSUME_PAUSE_MS)

            val currentRoom = _uiState.value.room ?: return@launch
            if (currentRoom.droppedFoodAnchor == null) return@launch

            val nextRoom = roomRepository.consumeFood()
            applyRepositoryRoom(nextRoom)
            recoverPetSatietyFromFood()
        }
    }

    override fun onCleared() {
        pendingFoodConsumeJob?.cancel()
        autonomousPetMovementJob?.cancel()
        friendMessagePollingJob?.cancel()
        petConditionJob?.cancel()
        plazaJoinJob?.cancel()
        plazaMessageSendJob?.cancel()
        super.onCleared()
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val roomRepository: RoomRepository,
        private val galleryRepository: GalleryRepository, // [추가됨]
        private val friendRepository: FriendRepository,
        private val plazaRepository: PlazaRepository,
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
                plazaRepository = plazaRepository,
                currencyRepository = currencyRepository,
                shopRepository = shopRepository
            ) as T
        }
    }
}

private data class PlazaParticipantProfile(
    val userId: String,
    val nickname: String,
    val pet: PlazaPetSnapshot
)

private fun PetUiState.toPlazaPetSnapshot(ownerUserId: String): PlazaPetSnapshot {
    return PlazaPetSnapshot(
        petId = petId,
        ownerUserId = ownerUserId,
        name = name,
        characterAssetKey = characterAssetKey,
        appearance = appearance,
        status = status,
        personality = personality,
        equippedItemIds = equippedItemIds
    )
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

private val PlazaOperationFailure.message: String
    get() = when (this) {
        PlazaOperationFailure.EMPTY_CODE -> "광장 코드를 입력해주세요."
        PlazaOperationFailure.INVALID_CODE -> "광장 코드는 PZ-0000 형식으로 입력해주세요."
        PlazaOperationFailure.PLAZA_NOT_FOUND -> "해당 광장을 찾을 수 없어요."
        PlazaOperationFailure.PLAZA_FULL -> "광장이 가득 찼어요."
        PlazaOperationFailure.EMPTY_MESSAGE -> "메시지를 입력해주세요."
        PlazaOperationFailure.MESSAGE_TOO_LONG -> "메시지는 ${PLAZA_MESSAGE_MAX_LENGTH}자까지 보낼 수 있어요."
        PlazaOperationFailure.NOT_IN_PLAZA -> "입장한 광장이 없어요."
        PlazaOperationFailure.UNKNOWN -> "광장 기능을 잠시 사용할 수 없어요."
    }
